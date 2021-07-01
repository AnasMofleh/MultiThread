package simulator;

import java.awt.Color;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import wash.WashingIO;

@SuppressWarnings("deprecation")
public class WashingSimulator {
    static {
        // Makes for better performance on our lab machines.
        // (Must be done before Swing initialization.)
        System.setProperty("sun.java2d.opengl", "True");
        
        try {
            // actually, the Metal look-and-feel is more suitable for a
            // washing machine, even on the Mac.
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // never mind
        }
    }

    private final Thread rotationThread   = new Thread(this::runRotation);
    private final Thread simulationThread = new Thread(this::runSimulation);

    private final WashingState state;

    @SuppressWarnings("unused")
    private final WashingView view;

    public WashingSimulator(int speed) {
        this.state = new WashingState(speed, rotationThread);
        this.view = WashingView.createViewFor(state);
    }

    public WashingIO startSimulation() {
        rotationThread.start();
        simulationThread.start();
        
        return state;
    }

    // -----------------------------------------------------------------------
    
    private static final long SLOW_DELAY_MS = 60;
    private static final long FAST_DELAY_MS = 20;

    private void runRotation() {
        while (state.getError() == null) {
            try {
                switch (state.getSpinMode()) {
                case WashingIO.SPIN_IDLE:
                    Thread.currentThread().join(); // infinite sleep
                    break;
                case WashingIO.SPIN_LEFT:
                case WashingIO.SPIN_RIGHT:
                    Thread.sleep(SLOW_DELAY_MS);
                    break;
                case WashingIO.SPIN_FAST:
                    Thread.sleep(FAST_DELAY_MS);
                    break;
                }
            } catch (InterruptedException e) {
                // do nothing:
                // interrupt() is used for awaking the thread from sleep
            }
            
            switch (state.getSpinMode()) {
            case WashingIO.SPIN_IDLE:
                // idle means idle, don't change
                break;
            case WashingIO.SPIN_LEFT:
                state.advanceFrame(-1);
                break;
            case WashingIO.SPIN_RIGHT:
                state.advanceFrame(1);
                break;
            case WashingIO.SPIN_FAST:
                state.advanceFrame(2);
                break;
            }
        }
    }

    // -----------------------------------------------------------------------

    private static final long SIMULATION_INTERVAL_MS = 50;

    private void runSimulation() {
        try {
            long prev = System.currentTimeMillis();
            while (state.getError() == null) {
                Thread.sleep(SIMULATION_INTERVAL_MS);

                long now = System.currentTimeMillis();

                state.update(now - prev);

                prev = now;
            }
        } catch (InterruptedException e) {
            throw new Error("unexpected interruption");
        }
    }
}
