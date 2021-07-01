package factory.simulation;

import factory.model.WidgetKind;

/**
 * Factory for building simulation items.
 * @param <T> the type of the simulated items built
 */
public interface ItemFactory<T extends SimItem> {
  /**
   * @param kind item kind
   * @param position initial position
   */
  T build(WidgetKind kind, double position);
}
