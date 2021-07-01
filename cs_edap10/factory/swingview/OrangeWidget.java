package factory.swingview;

import java.awt.Color;

import factory.model.WidgetKind;

class OrangeWidget extends RoundItem {
  OrangeWidget(SimView view, double position) {
    super(view, WidgetKind.ORANGE_ROUND_WIDGET, Color.ORANGE, 15, position);
  }
}
