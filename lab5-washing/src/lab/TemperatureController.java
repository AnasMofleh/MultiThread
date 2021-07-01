package lab;

import wash.WashingIO;

public class TemperatureController extends MessagingThread<WashingMessage> {
    private WashingIO io;
    private double upper, lower;
    private int command, counter ;
    private MessagingThread<WashingMessage> sender;

    public TemperatureController(WashingIO io) {
        this.io = io;
    }

    @Override
    public void run() {
        while (true) {
            try {
                WashingMessage m = receiveWithTimeout(9000 / Wash.SPEEDUP);
                if (m != null) {
                    sender = m.getSender();
                    command = m.getCommand();
                    upper = m.getValue();
                    lower = upper - 2;
                }


                switch (command) {
                    case WashingMessage.TEMP_IDLE:
                        io.heat(false);
                        break;
                    case WashingMessage.TEMP_SET:
                        if (io.getTemperature() < lower + 1) io.heat(true); // kanske vi behöver ändra condition
                        else {
                            if (io.getTemperature() == 20)sender.send(new WashingMessage(this, WashingMessage.ACKNOWLEDGMENT));
                            io.heat(false);
                        }
                        break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
