package factory.simulation;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import factory.model.DigitalSignal;
import factory.model.WidgetKind;

/**
 * Places objects on the conveyor belt.
 *
 * @param <T> the type of items dispensed
 */
public class Dispenser<T extends SimItem> extends Thread {
  private static final int MIN_DELAY = 700;
  private static final int DELAY_VARIANCE = 500;
  private final Random random;
  private final DigitalSignal beltClear;
  private final Conveyor<T> conveyor;
  private final ItemFactory<T> factory;
  private final Queue<QueuedItem> spawnQueue = new ConcurrentLinkedQueue<>();
  private boolean autoSpawn = true;
  private final WidgetKind[] items = WidgetKind.values();

  Dispenser(DigitalSignal beltClear, Conveyor<T> conveyor, ItemFactory<T> factory) {
    super("Dispenser");
    this.beltClear = beltClear;
    this.conveyor = conveyor;
    this.factory = factory;
    random = new Random(71538);

    // Some carefully placed items to cause problems :)
    spawnQueue.add(new QueuedItem(WidgetKind.ORANGE_ROUND_WIDGET, 200));
    spawnQueue.add(new QueuedItem(WidgetKind.GREEN_GADGET, 770));
    spawnQueue.add(new QueuedItem(WidgetKind.BLUE_RECTANGULAR_WIDGET, 1000));
    spawnQueue.add(new QueuedItem(WidgetKind.GREEN_GADGET, 10));
    spawnQueue.add(new QueuedItem(WidgetKind.BLUE_RECTANGULAR_WIDGET, 10));
    spawnQueue.add(new QueuedItem(WidgetKind.ORANGE_ROUND_WIDGET, 200));
    spawnQueue.add(new QueuedItem(WidgetKind.BLUE_RECTANGULAR_WIDGET, 1160));
    spawnQueue.add(new QueuedItem(WidgetKind.GREEN_GADGET, 0));
    spawnQueue.add(new QueuedItem(WidgetKind.BLUE_RECTANGULAR_WIDGET, 0));
  }

  @Override public void run() {
    try {
      while (!isInterrupted()) {
        long delay;
        WidgetKind kind;
        synchronized (this) {
          while (!autoSpawn && spawnQueue.isEmpty()) {
            wait();
          }
        }
        if (spawnQueue.isEmpty()) {
          delay = MIN_DELAY + random.nextInt(DELAY_VARIANCE);
          kind = nextRandomItem();
        } else {
          QueuedItem item = spawnQueue.poll();
          delay = item.delay;
          kind = item.kind;
        }
        if (delay > 0) {
          sleep(delay); // Wait for next object to be ready.
        }
        beltClear.waitLow(); // Wait for conveyor to be empty.
        //System.out.println("Created " + kind);
        conveyor.placeItem(factory.build(kind, 0));
      }
    } catch (InterruptedException ignored) {
    }
  }

  public synchronized WidgetKind nextRandomItem() {
    return items[random.nextInt(items.length)];
  }

  public synchronized void enableAutomation(boolean autoSpawn) {
    this.autoSpawn = autoSpawn;
    if (!autoSpawn) {
      spawnQueue.clear();
    }
    notifyAll();
  }

  public synchronized void queueItem(WidgetKind item) {
    spawnQueue.add(new QueuedItem(item, 0));
    notifyAll();
  }

  static class QueuedItem {
    final WidgetKind kind;
    final long delay;

    QueuedItem(WidgetKind kind, long delay) {
      this.kind = kind;
      this.delay = delay;
    }
  }
}
