package factory.simulation;

import java.util.Optional;

import factory.model.DigitalSignal;

public class Painter extends Thread {
  private static final double PRESS_SPEED = 1;
  public static final long PAINTING_MILLIS = 1000;
  private final DigitalSignal controlSignal;
  private SimSensor sensor;
  private double position = 0;
  private double simSpeed = 1.0;

  Painter(DigitalSignal controlSignal, SimSensor sensor) {
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
          position = Math.min(position + move, PAINTING_MILLIS);
          if (position > PAINTING_MILLIS * 0.5) {
            Optional<SimItem> item = sensor.currentSimItem();
            item.ifPresent(SimItem::paint);
          }
        } else {
          position = 0;
        }
      }
    } catch (InterruptedException ignored) {
    }
  }

  public synchronized void setSimSpeed(double simSpeed) {
    this.simSpeed = simSpeed;
  }
}
