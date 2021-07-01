package lab;

import wash.WashingIO;

public class SpinController extends MessagingThread<WashingMessage> {
    private WashingIO io;
    private int message;
    private boolean spinning = true;

    public SpinController(WashingIO io) {
        this.io = io;
    }

    @Override
    public void run() {
        try {

            // ... TODO ...

            while (true) {
                // wait for up to a (simulated) minute for a WashingMessage
                WashingMessage m = receiveWithTimeout(60000 / Wash.SPEEDUP);

                // if m is null, it means a minute passed and no message was received
                if (m != null) {
                    message = m.getCommand();
                }

                switch (message) {
                    case WashingMessage.SPIN_OFF:
                        io.setSpinMode(io.SPIN_IDLE);
                        break;
                    case WashingMessage.SPIN_SLOW:
                        if (spinning) io.setSpinMode(io.SPIN_LEFT);
                        else io.setSpinMode(io.SPIN_RIGHT);
                        spinning = !spinning;
                        break;
                    case WashingMessage.SPIN_FAST:
                        io.setSpinMode(io.SPIN_FAST);
                        break;
                }
            }
        } catch (InterruptedException unexpected) {
            // we don't expect this thread to be interrupted,
            // so throw an error if it happens anyway
            throw new Error(unexpected);
        }
    }
}
