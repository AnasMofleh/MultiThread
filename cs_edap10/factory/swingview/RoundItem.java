package factory.swingview;

import java.awt.Color;
import java.awt.Graphics;

import factory.model.WidgetKind;

public abstract class RoundItem extends VisualSimItem {
  private final double height;
  private double radX, radY;
  private Color fill;

  RoundItem(SimView view, WidgetKind kind, Color fill, double height, double position) {
    super(view, kind, position);
    this.height = height;
    radX = 15;
    radY = height;
    this.fill = fill;
  }

  @Override public void draw(Graphics g) {
    radY = height * (1 - 0.5 * compression);
    g.setColor(painted ? Color.RED : fill);
    g.fillOval(
        (int) (view.beltToX(position) - radX),
        (int) (view.getBeltY() - radY * 2),
        (int) radX*2, (int) radY*2);
  }
}
