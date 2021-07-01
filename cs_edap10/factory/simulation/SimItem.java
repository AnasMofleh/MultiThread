package factory.simulation;

import factory.model.WidgetKind;

public abstract class SimItem {
  final WidgetKind kind;
  public double position;
  protected double compression = 0.0;
  protected boolean painted = false;

  public SimItem(WidgetKind kind, double position) {
    this.kind = kind;
    this.position = position;
  }

  /** When the press squashes an this item. */
  final synchronized void squash(double ratio) {
    compression = Math.min(1.0, Math.max(compression, ratio));
  }

  /** When the paint sprayer paints this item. */
  final synchronized void paint() {
    painted = true;
  }
}
