package lab;

import wash.WashingIO;

public class WaterController extends MessagingThread<WashingMessage> {
    private WashingIO io;
    private int command, counter;
    private double upperVolume;
    private MessagingThread<WashingMessage> sender;

    public WaterController(WashingIO io) {
        this.io = io;
    }

    @Override
    public void run() {
        try {

            while (true) {

                WashingMessage m = receiveWithTimeout(3000 / Wash.SPEEDUP); // läser för mycket

                if (m != null ) {
                    command = m.getCommand();
                    upperVolume = m.getValue();
                    sender = m.getSender();
                }

                switch (command) {
                    case WashingMessage.WATER_FILL:
                        if (io.getWaterLevel() >= upperVolume) {
                            io.fill(false);

                            if (counter == 0) {
                                sender.send(new WashingMessage(this, WashingMessage.ACKNOWLEDGMENT));
                                counter =1;
                            }
                        }


                        if (io.getWaterLevel() < upperVolume) {
                            io.fill(true);
                            io.drain(false);
                        }

                        break;
                    case WashingMessage.WATER_DRAIN:
                        io.fill(false);
                        io.drain(true);

                        if (io.getWaterLevel() == 0) {
                            counter =0;
                            sender.send(new WashingMessage(this, WashingMessage.ACKNOWLEDGMENT));
                        }

                        break;

                    case WashingMessage.WATER_IDLE:
                        io.fill(false);
                        io.drain(false);
                        break;
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
