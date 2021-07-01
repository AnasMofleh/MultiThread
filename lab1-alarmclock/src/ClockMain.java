import java.util.concurrent.Semaphore;
import clock.Clock;
import clock.ClockInput;
import clock.ClockInput.UserInput;
import clock.ClockOutput;
import emulator.AlarmClockEmulator;


public class ClockMain {

    public static void main(String[] args) throws InterruptedException {
        AlarmClockEmulator emulator = new AlarmClockEmulator();
        ClockInput  in  = emulator.getInput();
        ClockOutput out = emulator.getOutput();
        Semaphore s = new Semaphore(1);

        Clock c = new Clock(out, in);

        Thread t1 = new Thread(() -> {
            Long t0 = System.currentTimeMillis();
            while (true) {

                t0 += 1000;
                try {
                    var diff1 = t0 - System.currentTimeMillis();
                    if (diff1 > 0 ){
                        Thread.sleep(diff1);
                    }
                    c.inc();
                    c.checkIfAlarmTime();
                    c.show();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }});

        t1.start();

        Semaphore sem = in.getSemaphore();

        while (true) {
            sem.acquire();                        // wait for user input

            UserInput userInput = in.getUserInput();
            int choice = userInput.getChoice();
            int value = userInput.getValue();

            switch (choice) {
                case 1 : c.setClock(value);
                    break;
                case 2 : c.setAlarm(value);
                    break;
                case 3 : c.changeAlarmState();
                    break;
                case 4 : c.changeAlarmState(false);
                    break;

            }

            System.out.println("choice = " + choice + "  value=" + value);
        }
    }
}
