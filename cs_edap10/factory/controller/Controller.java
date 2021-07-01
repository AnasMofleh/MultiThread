package factory.controller;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import factory.model.Camera;
import factory.model.DigitalSignal;
import factory.model.WidgetKind;

public class Controller {
    private final DigitalSignal cameraSensor, pressSensor, paintSensor;
    private final Camera camera;
    private final Queue<Optional<WidgetKind>> pressQueue = new LinkedBlockingQueue<>();
    private final Queue<Optional<WidgetKind>> paintQueue = new LinkedBlockingQueue<>();
    final ToolController toolController;

    public Controller(DigitalSignal press, DigitalSignal gun, DigitalSignal motor, Camera camera,
            DigitalSignal cameraSensor, DigitalSignal pressSensor, DigitalSignal paintSensor, long pressingMillis,
            long paintingMillis) {
        this.cameraSensor = cameraSensor;
        this.pressSensor = pressSensor;
        this.paintSensor = paintSensor;
        this.camera = camera;
        this.toolController = new ToolController(motor, press, gun, pressingMillis, paintingMillis);
    }

    public void startFactory() {
        cameraSensor.setOnHigh(() -> pressQueue.add(camera.takePhoto()));
        pressSensor.setOnHigh(this::press);
        paintSensor.setOnHigh(this::paint);
    }

    /**
     * Press the current item under the press tool. This method must not block since
     * it is called from the GUI thread! The returned latch can be used to wait for
     * the operation to complete.
     */
    public CountDownLatch press() {
        Optional<WidgetKind> next = pressQueue.poll();
        CountDownLatch latch;
        if (next != null) {
            latch = new CountDownLatch(1);
            next.ifPresent(item -> {
                pressItemAsync(item, latch);
                paintQueue.add(next);
            });
        } else {
            latch = new CountDownLatch(0);
        }
        return latch;
    }

    /**
     * Paint the current item under the paint gun. This method must not block since
     * it is called from the GUI thread! The returned latch can be used to wait for
     * the operation to complete.
     */
    public CountDownLatch paint() {
        Optional<WidgetKind> next = paintQueue.poll();
        CountDownLatch latch;
        if (next != null) {
            latch = new CountDownLatch(1);
            next.ifPresent(item -> paintItemAsync(item, latch));
        } else {
            latch = new CountDownLatch(0);
        }
        return latch;
    }

    /** Called by MockFactory */
    public void pressItem(WidgetKind item) throws InterruptedException {
        toolController.onPressSensorHigh(item);
    }

    /** Called by MockFactory */
    public void paintItem(WidgetKind item) throws InterruptedException {
        toolController.onPaintSensorHigh(item);
    }
    
    /**
     * @param item the current item under the press
     * @param done latch to be counted down when pressing is finished
     */
    private void pressItemAsync(WidgetKind item, CountDownLatch done) {
        new Thread(() -> {
            try {
                toolController.onPressSensorHigh(item);
            } catch (InterruptedException ignored) {
            }
            done.countDown();
        }).start();
    }

    /**
     * @param item the current item under the paint gun
     * @param done latch to be counted down when painting is finished
     */
    private void paintItemAsync(WidgetKind item, CountDownLatch done) {
        new Thread(() -> {
            try {
                toolController.onPaintSensorHigh(item);
            } catch (InterruptedException ignored) {
            }
            done.countDown();
        }).start();
    }
}
