package factory.controller;

import factory.model.DigitalSignal;
import factory.model.WidgetKind;
import factory.swingview.Factory;


public class ToolController {
    private final DigitalSignal conveyor, press, paint;
    private final long pressingMillis, paintingMillis;
    private int machins;

    public ToolController(DigitalSignal conveyor,
                          DigitalSignal press,
                          DigitalSignal paint,
                          long pressingMillis,
                          long paintingMillis) {
        this.conveyor = conveyor;
        this.press = press;
        this.paint = paint;
        this.pressingMillis = pressingMillis;
        this.paintingMillis = paintingMillis;
        this.machins = 0;
    }

    public synchronized void onPressSensorHigh(WidgetKind widgetKind) throws InterruptedException {
        if (widgetKind == WidgetKind.BLUE_RECTANGULAR_WIDGET) {
            conveyor.off();
            press.on();
            machins++;
            waitOut(pressingMillis);
            press.off();
            waitOut(pressingMillis);   // press needs this time to retract
            machins--;
            if (machins == 0) conveyor.on();
        }
    }

    public synchronized void onPaintSensorHigh(WidgetKind widgetKind) throws InterruptedException {
        if (widgetKind == WidgetKind.ORANGE_ROUND_WIDGET) {
            conveyor.off();
            paint.on();
            machins++;
            waitOut(paintingMillis);
            paint.off();
            waitOut(paintingMillis);
            machins--;
            if (machins == 0) conveyor.on();
        }
    }



    private void waitOut(long millis) throws InterruptedException {
        long timeTowakeup = System.currentTimeMillis() + millis;

        while (System.currentTimeMillis() < timeTowakeup) {
            var diff = timeTowakeup - System.currentTimeMillis();
            wait(diff);
        }
    }

    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        Factory factory = new Factory();
        factory.startSimulation();
    }
}
