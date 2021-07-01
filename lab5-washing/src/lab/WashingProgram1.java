package lab;

import wash.WashingIO;

class WashingProgram1 extends MessagingThread<WashingMessage> {

    private WashingIO io;
    private MessagingThread<WashingMessage> temp;
    private MessagingThread<WashingMessage> water;
    private MessagingThread<WashingMessage> spin;

    private final int waterVolume = 10, waterTemperature = 40;

    public WashingProgram1(WashingIO io,
                           MessagingThread<WashingMessage> temp,
                           MessagingThread<WashingMessage> water,
                           MessagingThread<WashingMessage> spin) {
        this.io = io;
        this.temp = temp;
        this.water = water;
        this.spin = spin;
    }

    @Override
    public void run() {
        try {

            io.lock(true); //lock the machine
            water.send(new WashingMessage(this, WashingMessage.WATER_FILL, waterVolume)); // fill the water

            receive(); // wait until the water is filled

            temp.send(new WashingMessage(this, WashingMessage.TEMP_SET, waterTemperature)); // regulate the temperature

            //receive(); // wait for the temperature to become 40ish

            spin.send(new WashingMessage(this, WashingMessage.SPIN_SLOW)); //start spinning

            Thread.sleep(30 * 60000 / Wash.SPEEDUP); //wait for 30 minuter

            temp.send(new WashingMessage(this, WashingMessage.TEMP_IDLE)); //

            water.send(new WashingMessage(this, WashingMessage.WATER_DRAIN)); // drain water

            receive();

            for (int i = 0 ; i < 5 ; i++) {
                water.send(new WashingMessage(this, WashingMessage.WATER_FILL, waterVolume)); // fill the water
                receive(); // wait until water filled
                water.send(new WashingMessage(this, WashingMessage.WATER_DRAIN)); // drain water
                receive(); //wait until water drained
            }

            spin.send(new WashingMessage(this, WashingMessage.SPIN_OFF)); // turn off spinning

            spin.send(new WashingMessage(this, WashingMessage.SPIN_FAST)); // turn off spinning

            Thread.sleep(5 * 60000 / Wash.SPEEDUP);

            spin.send(new WashingMessage(this, WashingMessage.SPIN_OFF)); // turn off spinning

            io.lock(false);

        } catch (InterruptedException e) {

            // if we end up here, it means the program was interrupt()'ed
            // set all controllers to idle

            temp.send(new WashingMessage(this, WashingMessage.TEMP_IDLE));
            water.send(new WashingMessage(this, WashingMessage.WATER_IDLE));
            spin.send(new WashingMessage(this, WashingMessage.SPIN_OFF));
            System.out.println("washing program terminated");
        }
    }
}

