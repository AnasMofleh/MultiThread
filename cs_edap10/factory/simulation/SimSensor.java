package factory.simulation;

import java.util.Optional;

import factory.model.WidgetKind;
import factory.model.ItemSensor;

public class SimSensor extends ItemSensor {
  public final double start, end;
  private SimItem currentItem = null;

  SimSensor(double start, double end) {
    super();
    this.start = start;
    this.end = end;
  }

  synchronized void setSimItem(SimItem item) {
    this.currentItem = item;
  }

  /**
   * Returns an optional containing the item kind for the item under the sensor.
   * An empty optional is returned if no item is below the sensor.
   */
  @Override public synchronized Optional<WidgetKind> currentItem() {
    // Computer vision would in reality take some time and need to be done asynchronously
    // in order to not block a controller thread.
    return currentItem == null ? Optional.empty() : Optional.of(currentItem.kind);
  }

  /**
   * Returns an optional containing the item under the sensor.
   * An empty optional is returned if no item is below the sensor.
   */
  synchronized Optional<SimItem> currentSimItem() {
    // Computer vision would in reality take some time and need to be done asynchronously
    // in order to not block a controller thread.
    return currentItem == null ? Optional.empty() : Optional.of(currentItem);
  }
}
