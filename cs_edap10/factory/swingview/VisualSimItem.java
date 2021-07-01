package factory.swingview;

import java.awt.Graphics;

import factory.model.WidgetKind;
import factory.simulation.SimItem;

abstract class VisualSimItem extends SimItem {
  final SimView view;

  VisualSimItem(SimView view, WidgetKind kind, double position) {
    super(kind, position);
    this.view = view;
  }

  public abstract void draw(Graphics g);
}
