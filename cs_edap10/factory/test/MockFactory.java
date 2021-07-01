package factory.test;

import factory.controller.Controller;
import factory.model.Camera;
import factory.model.DigitalSignal;
import factory.model.WidgetKind;
import factory.model.ItemSensor;

class MockFactory {
    private final long pressingMillis;
    private final long paintingMillis;
    public DigitalSignal
        press = new DigitalSignal(),
        gun = new DigitalSignal(),
        motor = new DigitalSignal(),
        pressSensor = new DigitalSignal(),
        paintSensor = new DigitalSignal();
    public final ItemSensor cameraSensor = new ItemSensor();
    private final Camera camera = new Camera(cameraSensor);
    private Controller controller;

    public MockFactory(long pressingMillis, long paintingMillis) {
        this.pressingMillis = pressingMillis;
        this.paintingMillis = paintingMillis;
    }

    public void queueItem(WidgetKind item) {
        cameraSensor.setItem(item);
        cameraSensor.signal.on();
        cameraSensor.signal.off();
    }

    /**
     * Press the next item. This method blocks until pressing is finished.
     */
    public void triggerPressSensor() {
        try {
            controller.press().await();
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }

    /**
     * Paint the next item. This method blocks until painting is finished.
     */
    public void triggerPaintSensor() {
        try {
            controller.paint().await();
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }

    /** Press the given item. */
    public void triggerPressSensor(WidgetKind item) throws InterruptedException {
        controller.pressItem(item);
    }

    /** Paint the given item. */
    public void triggerPaintSensor(WidgetKind item) throws InterruptedException {
        controller.paintItem(item);
    }

    public void start() {
        controller = new Controller(press, gun, motor, camera, cameraSensor.signal, pressSensor, paintSensor,
                pressingMillis, paintingMillis);
        controller.startFactory();
    }
}