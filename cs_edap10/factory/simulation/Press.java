package factory.simulation;

import java.util.Optional;

import factory.model.DigitalSignal;

public class Press extends Thread {
  private static final double PRESS_SPEED = 1;
  public static final long PRESSING_MILLIS = 1000;
  private final DigitalSignal controlSignal;
  private SimSensor sensor;
  public volatile float position = 0;
  private double simSpeed = 1.0;

  Press(DigitalSignal controlSignal, SimSensor sensor) {
    this.controlSignal = controlSignal;
    this.sensor = sensor;
  }

  @Override public void run() {
    try {
      long t0 = System.currentTimeMillis();
      //noinspection InfiniteLoopStatement
      while (true) {
        long delay = t0 + Simulation.SIM_FRAME_MILLIS - System.currentTimeMillis();
        if (delay > 0) {
          sleep(delay);
        }
        long delta = System.currentTimeMillis() - t0;
        t0 += delta;
        double move = PRESS_SPEED * delta;
        synchronized (this) {
          move *= simSpeed;
        }
        if (controlSignal.isHigh()) {
          position = (float) Math.min(position + move, PRESSING_MILLIS);
        } else {
          position = (float) Math.max(position - move, 0);
        }
        if (position > 0.1) {
          Optional<SimItem> item = sensor.currentSimItem();
          item.ifPresent(it -> it.squash(position / PRESSING_MILLIS));
        }
      }
    } catch (InterruptedException ignored) {
    }
  }

  public synchronized void setSimSpeed(double simSpeed) {
    this.simSpeed = simSpeed;
  }
}
