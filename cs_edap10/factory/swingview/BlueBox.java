package factory.swingview;

import java.awt.Color;

import factory.model.WidgetKind;

class BlueBox extends BoxItem {
  BlueBox(SimView view, double position) {
    super(view, WidgetKind.BLUE_RECTANGULAR_WIDGET, Color.BLUE, position);
  }
}
