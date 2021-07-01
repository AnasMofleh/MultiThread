package factory.model;

public class DigitalSignal {
  protected SignalValue value = SignalValue.LOW;
  private Runnable onHigh = () -> {};
  private Runnable onLow = () -> {};

  public final synchronized void setOnHigh(Runnable onHigh) {
    this.onHigh = onHigh;
  }

  public final synchronized void setOnLow(Runnable onLow) {
    this.onLow = onLow;
  }

  /**
   * Wait for the sensor signal to go high.
   */
  public final synchronized void waitHigh() throws InterruptedException {
    waitState(SignalValue.HIGH);
  }

  /**
   * Wait for the sensor signal to go low.
   */
  public final synchronized void waitLow() throws InterruptedException {
    waitState(SignalValue.LOW);
  }

  /**
   * Wait for the sensor signal to reach the given state.
   */
  public final synchronized void waitState(SignalValue state) throws InterruptedException {
    while (value != state) {
      wait();
    }
  }

  /**
   * Change this signal to HIGH.
   */
  public final synchronized void on() {
    setState(SignalValue.HIGH);
  }

  /**
   * Change this signal to HIGH.
   */
  public final synchronized void off() {
    setState(SignalValue.LOW);
  }

  public synchronized void setState(SignalValue newState) {
    if (value != newState) {
      value = newState;
      notifyAll();
      if (newState == SignalValue.HIGH) {
        onHigh.run();
      } else {
        onLow.run();
      }
    }
  }

  /**
   * Check if the signal is currently HIGH.
   */
  public final synchronized boolean isHigh() {
    return value == SignalValue.HIGH;
  }

  /**
   * Check if the signal is currently LOW.
   */
  public final synchronized boolean isLow() {
    return value == SignalValue.LOW;
  }
}
