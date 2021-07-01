package factory.swingview;

import factory.model.WidgetKind;
import factory.simulation.ItemFactory;

class ItemFactoryImpl implements ItemFactory<VisualSimItem> {
  private final SimView view;

  ItemFactoryImpl(SimView view) {
    this.view = view;
  }

  @Override public VisualSimItem build(WidgetKind kind, double position) {
    switch (kind) {
      case ORANGE_ROUND_WIDGET:
        return new OrangeWidget(view, position);
      case BLUE_RECTANGULAR_WIDGET:
        return new BlueBox(view, position);
      case GREEN_GADGET:
        return new GreenGadget(view, position);
    }
    throw new Error("Unknown item kind: " + kind);
  }
}
