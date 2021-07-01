package factory.swingview;

import java.awt.Color;
import java.awt.Graphics;

import factory.model.WidgetKind;

public abstract class BoxItem extends VisualSimItem {
  private double height, width;
  private Color fill;

  BoxItem(SimView view, WidgetKind kind, Color fill, double position) {
    super(view, kind, position);
    width = 30;
    height = 50;
    this.fill = fill;
  }

  @Override public void draw(Graphics g) {
    height = 20 + (1 - compression) * 30;
    g.setColor(painted ? Color.RED : fill);
    g.fillRect(
        (int) (view.beltToX(position) - width * 0.5),
        (int) (view.getBeltY() - height),
        (int) width, (int) height);
  }
}
