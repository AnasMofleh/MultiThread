package clock;

import java.util.concurrent.Semaphore;

public class Clock {
    Semaphore s = new Semaphore(1);
    ClockOutput out;
    ClockInput in;
    private int hh, mm, ss,alarmTime, hhmmss, alarmCounter;
    private boolean alarmState, alarmPeeping;

    public Clock(ClockOutput out, ClockInput in) throws InterruptedException {
        s.acquire();
        this.out = out;
        this.in = in;
        alarmState = false;
        alarmCounter = 0;
        s.release();
    }

    public void show() throws InterruptedException {
        s.acquire();
        out.displayTime(hhmmss);
        s.release();
    }

    public void setClock(int value) throws InterruptedException {
        s.acquire();
        hhmmss = value;
        s.release();
    }

    public void inc() throws InterruptedException {
        s.acquire();
        ss = hhmmss % 100;
        mm = ((hhmmss / 100) % 100);
        hh= hhmmss / 10000;
        ss++;

        if (ss == 60) {
            ss = 0;
            mm += 1;
        }
        if (mm == 60){
            mm = 0;
            hh += 1;
        }
        if(hh == 24) {
            hh=0;
            mm=0;
            ss=0;
        }
        hhmmss = (hh * 10000) + (mm * 100) + ss;
        s.release();
    }


    public void setAlarm(int value) throws InterruptedException {
        s.acquire();
        alarmTime = value;
        s.release();
    }

    public void changeAlarmState() throws InterruptedException {
        s.acquire();
        alarmState = !alarmState;
        out.setAlarmIndicator(alarmState);
        s.release();
    }

    public void changeAlarmState(Boolean bool) throws InterruptedException {
        s.acquire();
        if(alarmPeeping) {
            alarmState = false;
            out.setAlarmIndicator(alarmState);
            alarmPeeping = false;
            alarmCounter = 0;
        }
        s.release();
    }

    public void checkIfAlarmTime() throws InterruptedException {
        s.acquire();
        if (alarmState) {
            if (alarmTime == hhmmss) alarmCounter = 21;
            if (alarmCounter > 0) {
                alarmPeeping = true;
                alarmCounter--;
                out.alarm();
            }

            if (alarmCounter == 1) {
                alarmState = !alarmState;
                out.setAlarmIndicator(alarmState);
                alarmPeeping = false;
                alarmCounter = 0;
            }
        }
        s.release();
    }
}
