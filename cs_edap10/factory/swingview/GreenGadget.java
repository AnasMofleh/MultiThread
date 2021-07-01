package factory.swingview;

import java.awt.Color;

import factory.model.WidgetKind;

class GreenGadget extends RoundItem {
  GreenGadget(SimView view, double position) {
    super(view, WidgetKind.GREEN_GADGET, new Color(25, 200, 100), 25, position);
  }
}
