package emulator;

import clock.ClockInput;
import clock.ClockOutput;

/** Emulator for alarm clock. */
public class AlarmClockEmulator {
    static {
        // Makes for better performance on our lab machines.
        // (Must be done before Swing initialization.)
        System.setProperty("sun.java2d.opengl", "True");
    }

    private final ClockState model = new ClockState();
    private final LedDisplay view  = LedDisplay.createViewFor(model);

    /** @return input signals from (simulated) hardware */
    public ClockInput getInput() {
        return model;
    }

    /** @return output signals to (simulated) hardware */
    public ClockOutput getOutput() {
        return view;
    }
}