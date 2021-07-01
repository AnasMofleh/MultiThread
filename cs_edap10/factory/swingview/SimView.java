package factory.swingview;

public interface SimView {
  double getBeltY();

  /** Convert belt position to canvas X position. */
  double beltToX(double position);
}
