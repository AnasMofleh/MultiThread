package factory.model;

import java.util.Optional;

public class ItemSensor {
  public final DigitalSignal signal;
  private WidgetKind currentItem = null;

  public ItemSensor() {
    this.signal = new DigitalSignal();
  }

  public synchronized void setItem(WidgetKind item) {
    this.currentItem = item;
  }

  /**
   * Returns an optional containing the item kind for the item under the sensor.
   * An empty optional is returned if no item is below the sensor.
   */
  public synchronized Optional<WidgetKind> currentItem() {
    // Computer vision would in reality take some time and need to be done asynchronously
    // in order to not block a controller thread.
    return currentItem == null ? Optional.empty() : Optional.of(currentItem);
  }
}
