package factory.model;

import java.util.Optional;

public class Camera {
  private final ItemSensor sensor;

  public Camera(ItemSensor sensor) {
    this.sensor = sensor;
  }

  /**
   * Identifies the object under the camera.
   * Returns an empty optional if no object is below the camera.
   */
  public Optional<WidgetKind> takePhoto() {
    // Computer vision would in reality take some time and need to be done asynchronously
    // in order to not block a controller thread.
    return sensor.currentItem();
  }
}
