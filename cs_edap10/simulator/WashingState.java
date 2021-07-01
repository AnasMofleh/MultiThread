package simulator;

import java.util.Observable;

import wash.WashingIO;

@SuppressWarnings("deprecation")
class WashingState extends Observable implements WashingIO {
    
    // temperatures
    public static final int          MAX_TEMP = 100;
    public static final int      AMBIENT_TEMP = 20;
    public static final int   COLOR_WASH_TEMP = 40;
    public static final int   WHITE_WASH_TEMP = 60;
    public static final double SHRINKAGE_TEMP = 60.01;
    public static final int   COOL_WATER_TEMP = 8;
    
    // number of positions for rotating barrel
    public static final int NBR_CLOTHES_FRAMES = 48;

    // Water heat capacity (J / (kg * K))
    private static final double WATER_HEAT_CAPACITY = 4184;

    // Water flow from fill/drain pumps (l/s)
    private static final double FLOW_IN = 0.11345677; // Approx. 0.1
    private static final double FLOW_OUT = 0.1926345; // Approx. 0.2

    // Radiator power (W)
    private static final double RADIATOR_POWER = 2034.5; // Approx. 2kW

    // Cooling proportional constant (a.k.a. Boris' Magic Number)
    private static final double COOLING_FACTOR = 4200;
    
    private final int speed;
    private final Thread rotationThread;
    
    private double level = 0;               // water level, range 0 ... MAX_WATER_LEVEL
    private double temp = AMBIENT_TEMP;     // water temperature (deg C)
    private int mode = WashingIO.SPIN_IDLE; // rotation mode
    private int frame = 0;                  // current animation frame
    private long startTime = -1;            // timestamp of when current program started
    private boolean isHeating = false;
    private boolean isFilling = false;
    private boolean isDraining = false;
    private boolean isLocked = false;
    private int currentProgram = 0;
    private boolean clothesShrunk = false;  // true if ever washed over SHRINKAGE_TEMP
    private String error = null;            // error message, or null if no error
    private String errorExplanation = null; // only valid if error != null
    private int lastButtonPressed = -1;     // no need to queue these events up

    public WashingState(int speed, Thread rotationThread) {
        this.speed = speed;
        this.rotationThread = rotationThread;
    }
    
    // --------------------------------------------------- interface WashingIO

    @Override
    public synchronized void heat(boolean on) {
        if (isHeating != on) {
            isHeating = on;
            setChanged();
            notifyObservers();
        }
    }

    @Override
    public synchronized void fill(boolean on) {
        if (isFilling != on) {
            isFilling = on;
            setChanged();
            notifyObservers();
        }
    }

    @Override
    public synchronized void drain(boolean on) {
        if (isDraining != on) {
            isDraining = on;
            setChanged();
            notifyObservers();
        }
    }

    @Override
    public synchronized void lock(boolean locked) {
        if (isLocked != locked) {
            isLocked = locked;
            setChanged();
            notifyObservers();
        }
    }

    @Override
    public synchronized void setSpinMode(int spinMode) {
        if (mode != spinMode) {
            mode = spinMode;
            rotationThread.interrupt();
        }
    }

    @Override
    public synchronized int awaitButton() throws InterruptedException {
        while (lastButtonPressed < 0 && error == null) {
            wait();
        }
        if (error != null) {
            throw new Error(error + " (" + errorExplanation + ")");
        }
        int b = lastButtonPressed;
        lastButtonPressed = -1;
        return b;
    }

    @Override
    public synchronized double getWaterLevel() {
        return level;
    }

    @Override
    public synchronized double getTemperature() {
        return temp;
    }
    
    // ------------------------------------------------------ package internal

    synchronized int getSpinMode() {
        return mode;
    }

    synchronized String getError() {
        return error;
    }

    // advance animation frame by step (possibly negative)
    synchronized void advanceFrame(int step) {
        frame = Math.floorMod(frame + step, NBR_CLOTHES_FRAMES);
        setChanged();        
        notifyObservers();
    }

    synchronized int getFrame() {
        return frame;
    }

    synchronized void submitButtonPress(int button) {
        if (button != currentProgram) {
            currentProgram = lastButtonPressed = button;
            startTime = System.currentTimeMillis();
            notifyAll();
        }
    }

    synchronized int getCurrentProgram() {
        return currentProgram;
    }
    
    /** Returns number of simulated minutes elapsed */
    synchronized int getElapsedMinutes() {
        return (int) ((System.currentTimeMillis() - startTime) * speed / (1000 * 60));
    }
    
    synchronized boolean isHeating() {
        return isHeating;
    }

    synchronized boolean isFilling() {
        return isFilling;
    }

    synchronized boolean isDraining() {
        return isDraining;
    }

    synchronized boolean isLocked() {
        return isLocked;
    }

    synchronized boolean clothesShrunk() {
        return clothesShrunk;
    }
    
    /** Update simulation state.
        @param millis  number of milliseconds since last call */
    synchronized void update(long millis) {
        double dt = millis * speed / 1000.0;
        
        //
        // Water flowing in/out
        // (possibly affecting temperature)
        //
        
        if (isFilling) {
            double dV = FLOW_IN * dt;
            temp = (temp * level + COOL_WATER_TEMP * dV) / (level + dV);
            level += dV;
            
            if (level > WashingIO.MAX_WATER_LEVEL) {
                fail("OVERFLOW", "barrel full, valve open");
                return;
            }
        }
        if (isDraining) {
            double dV = -FLOW_OUT * dt;
            level += dV;
            if (level < 0) {
                level = 0;
                if (!isHeating) {
                    temp = AMBIENT_TEMP;
                }
            }
        }

        //
        // Heating
        //

        if (isHeating) {
            if (level > 0) {
                temp += dt * RADIATOR_POWER / (level * WATER_HEAT_CAPACITY);
            } else {
                temp = MAX_TEMP;
            }
            
            if (temp >= MAX_TEMP) {
                temp = MAX_TEMP;
                fail("OVERHEATED", "temperature \u2265 " + MAX_TEMP + "\u00B0C");
                return;
            }
            
        }

        //
        // Cooling
        //
        
        temp -= dt * (temp - AMBIENT_TEMP) / COOLING_FACTOR;

        //
        // Errors: open hatch, shrinking clothes,
        // drain+valve both open, or centrifuging significant amount of water
        //
        
        if (level > WashingIO.MAX_WATER_LEVEL * 0.1 && !isLocked) {
            fail("OVERFLOW", "hatch open, water in barrel");
            return;
        }
        
        if (level > WashingIO.MAX_WATER_LEVEL * 0.1 && temp > WHITE_WASH_TEMP) {
            clothesShrunk = true;
        }
        
        if (isDraining && isFilling) {
            fail("OVERFLOW", "drain and valve both open");
            return;
        }
        
        if (level > WashingIO.MAX_WATER_LEVEL * 0.1 && mode == WashingIO.SPIN_FAST) {
            fail("MECHANICAL FAILURE", "centrifuge on, water in machine");
            return;
        }
        
        setChanged();
        notifyObservers();
    }

    // --------------------------------------------------------------- private

    private synchronized void fail(String error, String errorExplanation) {
        this.error = error;
        this.errorExplanation = errorExplanation;

        setChanged();
        notifyObservers();
        
        notifyAll();
    }
}

