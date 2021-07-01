package factory.simulation;

import factory.controller.Controller;
import factory.model.Camera;
import factory.model.DigitalSignal;

public class Simulation<T extends SimItem> {
    /** Defines the simulation frame rate. */
    static final long SIM_FRAME_MILLIS = 15;

    public static final double CONVEYOR_LENGTH = 7000, SENSOR_WIDTH = 220, DROPPER_POSITION = 500,
            CAMERA_POSITION = 1500, PRESS_POSITION = 3000, PAINTER_POSITION = 5500;

    public final Dispenser<T> dispenser;
    public final Conveyor<T> conveyor;
    public final DigitalSignal motor, press, paintGun;
    private final Controller controller;
    public final SimSensor sensor0, sensor1, sensor2, sensor3;
    public Press pressSim;
    private Painter paintSim;

    public Simulation(ItemFactory<T> factory) {
        sensor0 = new SimSensor(DROPPER_POSITION - 400, DROPPER_POSITION + 400);
        sensor1 = new SimSensor(CAMERA_POSITION - SENSOR_WIDTH / 2, CAMERA_POSITION + SENSOR_WIDTH / 2);
        sensor2 = new SimSensor(PRESS_POSITION - SENSOR_WIDTH / 2, PRESS_POSITION + SENSOR_WIDTH / 2);
        sensor3 = new SimSensor(PAINTER_POSITION - SENSOR_WIDTH / 2, PAINTER_POSITION + SENSOR_WIDTH / 2);
        motor = new DigitalSignal();
        press = new DigitalSignal();
        pressSim = new Press(press, sensor2);
        paintGun = new DigitalSignal();
        paintSim = new Painter(paintGun, sensor3);
        Camera camera = new Camera(sensor1);
        controller = new Controller(press, paintGun, motor, camera, sensor1.signal, sensor2.signal, sensor3.signal, Painter.PAINTING_MILLIS, Press.PRESSING_MILLIS);
        conveyor = new Conveyor<>(CONVEYOR_LENGTH, DROPPER_POSITION, motor, sensor0, sensor1, sensor2, sensor3);
        dispenser = new Dispenser<>(sensor0.signal, conveyor, factory);
    }

    public void start() {
        dispenser.start();
        conveyor.start();
        motor.on();
        controller.startFactory();
        pressSim.start();
        paintSim.start();
    }

    public void setSpeed(double speed) {
        conveyor.setSimSpeed(speed);
        pressSim.setSimSpeed(speed);
        paintSim.setSimSpeed(speed);
    }
}
