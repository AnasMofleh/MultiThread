package emulator;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Observable;
import java.util.concurrent.Semaphore;

import clock.ClockInput;

/** Hardware model of our simulated alarm clock. */
@SuppressWarnings("deprecation")
class ClockState extends Observable implements ClockInput {
    public static final int NO_EDITABLE_DIGIT   = -1;
    public static final int LAST_EDITABLE_DIGIT = 5;
    private static final int INVALID_TIME_VALUE = 999999;
    
    // indicates which digit (0..5) is currently being edited, or <0 if none
    private int editedTimeDigit = NO_EDITABLE_DIGIT;
    private int editedAlarmDigit = NO_EDITABLE_DIGIT;

    // Digits for clock time and alarm time. These are always in range 0..9.
    private final int[] time  = { 0, 0, 0, 0, 0, 0 };
    private final int[] alarm = { 0, 0, 0, 0, 0, 0 };
    
    // Alarm indicator (on or off)
    private boolean alarmIndicator = false;
    
    // Flashing alarm icon animation state
    private boolean visualAlarmOn = false;
    
    // Indicate whether any changes (digit up/down) have been made since user started editing
    private boolean anyTimeDigitChanged = false;
    private boolean anyAlarmDigitChanged = false;

    // "Hardware interrupt" semaphore, signaled whenever a new choice (above) is available
    private final Semaphore choiceSem = new Semaphore(0);
    private final Deque<ClockInput.UserInput> userEvents = new LinkedList<>();

    /** Changes clock time. If the time is currently being edited, the new time is ignored. */
    public synchronized void setTimeValue(int hhmmss) {
        if (! anyTimeDigitChanged) {
            // Ensure values are in range 0..9
            for (int i = time.length - 1; i >= 0; i--) {
                time[i] = Math.floorMod(hhmmss, 10);
                hhmmss /= 10;
            }
            setChanged();
            notifyObservers();
        }
    }

    /** @return clock time, as an array of digits. */
    public synchronized int[] getTime() {
        return time.clone();
    }

    /** @return clock time, as an array of digits. */
    public synchronized int[] getAlarm() {
        return alarm.clone();
    }

    /** @return true if the alarm status is ON, false for OFF. */
    public synchronized boolean alarmIndicatorIsOn() {
        return alarmIndicator && !visualAlarmOn;
    }

    /** Set the status to ALARM ON or OFF. */
    public synchronized void setAlarmStatus(boolean on) {
        if (alarmIndicator != on || visualAlarmOn) {
            alarmIndicator = on;
            visualAlarmOn = false;
            setChanged();
        }
    }

    /** Set the status to ALARM ON or OFF. */
    public synchronized void visualAlarm(boolean on) {
        if (visualAlarmOn != on) {
            visualAlarmOn = on;
            setChanged();            
            notifyObservers();
        }
    }

    /** @return true if the visual alarm is currently signaling, false otherwise */
    public synchronized boolean visualAlarmIsOn() {
        return visualAlarmOn;
    }

    /** User pressed UP button. */
    public synchronized void upPressed() {
        anyTimeDigitChanged  |= increaseDigit(time, editedTimeDigit);
        anyAlarmDigitChanged |= increaseDigit(alarm, editedAlarmDigit);
    }

    /** User pressed DOWN button. */
    public synchronized void downPressed() {
        anyTimeDigitChanged  |= decreaseDigit(time, editedTimeDigit);
        anyAlarmDigitChanged |= decreaseDigit(alarm, editedAlarmDigit);
    }
    
    /** User pressed LEFT button. */
    public synchronized void leftPressed() {
        if (editedTimeDigit > 0) {
            editedTimeDigit--;
        } else if (editedAlarmDigit > 0) {
            editedAlarmDigit--;
        }
    }

    /** User pressed RIGHT button. */
    public synchronized void rightPressed() {
        if (editedTimeDigit >= 0 && editedTimeDigit < LAST_EDITABLE_DIGIT) {
            editedTimeDigit++;
        } else if (editedAlarmDigit >= 0 && editedAlarmDigit < LAST_EDITABLE_DIGIT) {
            editedAlarmDigit++;
        }
    }
    
    /** User pressed SET TIME button. */
    public synchronized void setTimePressed() {
        if (editedAlarmDigit >= 0) {
            reportInput(ClockInput.CHOICE_TOGGLE_ALARM, INVALID_TIME_VALUE);
        } else {
            editedTimeDigit = LAST_EDITABLE_DIGIT;
            editedAlarmDigit = NO_EDITABLE_DIGIT;
            anyTimeDigitChanged = false;
        }
    }
    
    /** User pressed SET ALARM button. */
    public synchronized void setAlarmPressed() {
        if (editedTimeDigit >= 0) {
            reportInput(ClockInput.CHOICE_TOGGLE_ALARM, INVALID_TIME_VALUE);
        } else {
            editedAlarmDigit = LAST_EDITABLE_DIGIT;
            editedTimeDigit = NO_EDITABLE_DIGIT;
            anyAlarmDigitChanged = false;
        }
    }
    
    /** User released SET TIME button. If a value was entered, store it and signal the semaphore. */
    public synchronized void setTimeReleased() {
        editedTimeDigit = NO_EDITABLE_DIGIT;
        if (anyTimeDigitChanged) {
            reportInput(ClockInput.CHOICE_SET_TIME, timeValueFromArray(time));
        } else {
            reportInput(ClockInput.CHOICE_OTHER, INVALID_TIME_VALUE);
        }
        anyTimeDigitChanged = false;
    }
    
    /** User released SET ALARM button. If a value was entered, store it and signal the semaphore. */
    public synchronized void setAlarmReleased() {
        editedAlarmDigit = NO_EDITABLE_DIGIT;
        if (anyAlarmDigitChanged) {
            reportInput(ClockInput.CHOICE_SET_ALARM, timeValueFromArray(alarm));
        } else {
            reportInput(ClockInput.CHOICE_OTHER, INVALID_TIME_VALUE);
        }
        anyAlarmDigitChanged = false;
    }
    
    /**
     * @return index of currently edited digit in clock time, or < 0 if the clock
     *         time is currently not being edited.
     */
    public synchronized int currentlyEditedTimeDigit() {
        return editedTimeDigit;
    }
    
    /**
     * @return index of currently edited digit in alarm time, or < 0 if the alarm
     *         time is currently not being edited.
     */
    public synchronized int currentlyEditedAlarmDigit() {
        return editedAlarmDigit;
    }

    // ================================================== interface ClockInput

    @Override
    public Semaphore getSemaphore() {
        return choiceSem;
    }

    @Override
    public synchronized UserInput getUserInput() {
        return userEvents.removeLast();
    }

    // -----------------------------------------------------------------------

    /** For testing */
    @Override
    public String toString() {
        return String.format("%06d %06d", timeValueFromArray(time), timeValueFromArray(alarm));
    }

    // -----------------------------------------------------------------------

    private void reportInput(int choice, int value) {
        userEvents.addFirst(new UserInput() {
            @Override
            public int getChoice() {
                return choice;
            }

            @Override
            public int getValue() {
                return value;
            }
        });
        choiceSem.release();
    }
    
    private boolean decreaseDigit(int[] digits, int pos) {
        if (pos >= 0 && pos < digits.length && digits[pos] > 0) {
            digits[pos]--;
            return true;
        } else {
            return false;
        }
    }

    private boolean increaseDigit(int[] digits, int pos) {
        boolean change = false;
        if (pos >= 0 && pos < digits.length && digits[pos] < 9) {
            if (!(pos == 0 && digits[0] == 2
                    || pos == 1 && digits[0] == 2 && digits[1] == 3
                    || pos == 2 && digits[2] == 5
                    || pos == 4 && digits[4] == 5))
            {
                digits[pos]++;
                change = true;
            }
            // if the first digit of hours becomes 2, the second digit cannot be > 3
            if (pos == 0 && digits[0] == 2 && digits[1] > 3) {
                digits[1] = 3;
            }
        }
        return change;
    }

    private int timeValueFromArray(int[] digits) {
        return Arrays.stream(digits).reduce((a, b) -> 10 * a + b).getAsInt();
    }
}
