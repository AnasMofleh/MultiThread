package factory.test;

import factory.model.DigitalSignal;
import factory.model.SignalValue;

/**
 * This is used for checking if another signal is high at
 * the time when this signal is switched.
 */
class CheckedSignal extends DigitalSignal {
  private final DigitalSignal checked;
  private int hiOnEnable = 0;
  private int hiOnDisable = 0;

  CheckedSignal(DigitalSignal checked) {
    this.checked = checked;
  }

  @Override public synchronized void setState(SignalValue newState) {
    if (value != newState) {
      value = newState;
      if (newState == SignalValue.HIGH) {
        if (checked.isHigh()) {
          hiOnEnable += 1;
        }
      } else {
        if (checked.isHigh()) {
          hiOnDisable += 1;
        }
      }
    }
    super.setState(newState);
  }

  /**
   * Returns the number of times the watched signal was high during enable.
   */
  public synchronized int hiOnEnable() {
    return hiOnEnable;
  }

  /**
   * Returns the number of times the watched signal was high during disable.
   */
  public synchronized int hiOnDisable() {
    return hiOnDisable;
  }
}
