package factory.simulation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import factory.model.DigitalSignal;

// TODO: verify that all items are sensed by the sensors.
// Precondition: two different items are never sensed at the same time by the same sensor.
/**
 * Simulates a conveyor belt.
 * Transports items while the motor is running.
 * Use placeItem to add new items to the belt.
 *
 * @param <T> the type of items carried by the conveyor
 */
public class Conveyor<T extends SimItem> extends Thread {
  private static final double MAX_SPEED = 2.0, ACCEL_MULT = 9.8, BRAKE_MULT = 12.0;
  private double speed = 0;
  private final double length;
  private final DigitalSignal motor;
  private final SimSensor[] sensors;
  private List<T> q0 = new LinkedList<>();
  private List<T> q1 = new LinkedList<>();
  private List<T> q2 = new LinkedList<>();
  private List<T> q3 = new LinkedList<>();
  private List<T> q4 = new LinkedList<>();
  private double dropPosition;
  private double simSpeed = 1.0;
  private double position = 0.0;

  Conveyor(double length, double dropPosition, DigitalSignal motor,
      SimSensor sensor0, SimSensor sensor1, SimSensor sensor2, SimSensor sensor3) {
    super("Conveyor");
    this.length = length;
    this.dropPosition = dropPosition;
    this.motor = motor;
    sensors = new SimSensor[] { sensor0, sensor1, sensor2, sensor3 };
  }

  /**
   * Place a new item on the conveyor belt.
   */
  synchronized void placeItem(T item) {
    item.position = dropPosition;
    q0.add(0, item);
    sensors[0].signal.on();
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
        if (motor.isHigh()) {
          speed = Math.min(MAX_SPEED, speed + ACCEL_MULT * delta / 1000.0);
        } else {
          speed = Math.max(0, speed - BRAKE_MULT * delta / 1000.0);
        }
        double move = delta * speed;
        synchronized (this) {
          move *= simSpeed;
          position += move;
          advance(q0, move);
          advance(q1, move);
          advance(q2, move);
          advance(q3, move);
          advance(q4, move);
          checkSensor(q0, q1, sensors[0]);
          checkSensor(q1, q2, sensors[1]);
          checkSensor(q2, q3, sensors[2]);
          checkSensor(q3, q4, sensors[3]);
          while (!q4.isEmpty()) {
            SimItem last = q4.get(q4.size() - 1);
            if (last.position <= length) {
              break;
            }
            q4.remove(q4.size() - 1);
          }
        }
      }
    } catch (InterruptedException ignored) {
    }
  }

  /**
   * Moves items over from the input queue to the output queue when they have passed the
   * end of the sensor range.
   */
  private void checkSensor(List<T> in, List<T> out, SimSensor sensor) {
    DigitalSignal signal = sensor.signal;
    if (!in.isEmpty()) {
      T last = in.get(in.size() - 1);
      if (signal.isLow()) {
        // Check if the next object has entered the sensor range.
        if (last.position >= sensor.start) {
          sensor.setSimItem(last);
          signal.on();
        }
      } else {
        // Check if current object has left the sensor range.
        if (last.position > sensor.end) {
          out.add(0, in.remove(in.size() - 1));
          signal.off();
          sensor.setSimItem(null);
        }
      }
    }
  }

  /**
   * Move the items in the queue along the belt.
   * @param queue items to move
   * @param delta distance to move the items
   */
  private void advance(List<T> queue, double delta) {
    for (T item : queue) {
      item.position += delta;
    }
  }

  /**
   * Get the set of all items currently on the conveyor belt.
   */
  public synchronized Set<T> currentItems() {
    Set<T> items = new HashSet<>();
    items.addAll(q0);
    items.addAll(q1);
    items.addAll(q2);
    items.addAll(q3);
    items.addAll(q4);
    return items;
  }

  public synchronized void setSimSpeed(double simSpeed) {
    this.simSpeed = simSpeed;
  }

  public synchronized double getPosition() {
    return position;
  }
}
