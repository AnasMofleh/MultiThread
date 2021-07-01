package factory.swingview;

import static factory.simulation.Simulation.CONVEYOR_LENGTH;
import static factory.simulation.Simulation.PAINTER_POSITION;
import static factory.simulation.Simulation.PRESS_POSITION;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import factory.simulation.Press;
import factory.simulation.Simulation;

public class Factory implements SimView {
    static {
        // Makes for better performance on our lab machines.
        // (Must be done before Swing initialization.)
        System.setProperty("sun.java2d.opengl", "True");
    }

    /** Defines the visualization frame rate. */
    private static final int FRAME_MILLIS = 33;

    private static final int
        MARGIN = 10,
        SENSOR_HEIGHT = 10,
        TOOL_HEIGHT = 110,
        BELT_CLEARANCE = 55,
        BELT_TOP = TOOL_HEIGHT + BELT_CLEARANCE,
        BELT_LEN = 700,
        BELT_HEIGHT = 70,
        CANVAS_WIDTH = BELT_LEN + BELT_HEIGHT + MARGIN,
        CANVAS_HEIGHT = BELT_TOP + BELT_HEIGHT + SENSOR_HEIGHT + MARGIN,
        BELT_X = (int) ((CANVAS_WIDTH - BELT_LEN) * 0.5);

    private static final double INITIAL_SPEED = 0.7;
    private static final int TOOL_BOX_WIDTH = 70;
    public static final int PISTON_WIDTH = 32;
    public static final int TUBE_WIDTH = 10;
    private static final Color DARKBLUE = new Color(25, 50, 180);

    private final Simulation<VisualSimItem> sim;
    private final JFrame frame;

    /** Items currently on the belt. */
    private Set<VisualSimItem> onConveyor = new HashSet<>();

    public Factory() {
        // create JFrame in Swing UI thread
        final CompletableFuture<JFrame> displayStore = new CompletableFuture<>();
        try {
            SwingUtilities.invokeLater(() -> {
                JFrame f = new JFrame("Factory");
                f.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
                f.setResizable(false);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.add(new Canvas());
                f.setLocationRelativeTo(null);
                f.pack();
                displayStore.complete(f);
            });
            frame = displayStore.get();
    
            sim = new Simulation<>(new ItemFactoryImpl(this));
            sim.setSpeed(INITIAL_SPEED);
    
            new Timer(FRAME_MILLIS, e -> frame.repaint()).start();
        } catch (InterruptedException | ExecutionException e) {
            throw new Error(e);
        }
    }

    public void startSimulation() {
        try {
            SwingUtilities.invokeAndWait(() -> frame.setVisible(true));
        } catch (InvocationTargetException | InterruptedException e) {
            throw new Error(e);
        }
        sim.start();
    }

    @Override
    public double beltToX(double position) {
        return BELT_X + BELT_LEN * position / CONVEYOR_LENGTH;
    }

    private static Color sensorColor(boolean high) {
        return high ? Color.RED : Color.GREEN;
    }

    private static Color pressColor(boolean high) {
        return high ? Color.RED : DARKBLUE;
    }

    @Override
    public double getBeltY() {
        return BELT_TOP;
    }

    @SuppressWarnings("serial")
    class Canvas extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
            g.setColor(Color.DARK_GRAY);
            g.fillRect(BELT_X, BELT_TOP, BELT_LEN, BELT_HEIGHT);

            // Press:
            g.setColor(pressColor(sim.press.isHigh()));
            g.fillRect((int) (beltToX(PRESS_POSITION) - TOOL_BOX_WIDTH * 0.5), 0, 70, 100);
            g.setColor(DARKBLUE);
            g.fillRect((int) (beltToX(PRESS_POSITION) - PISTON_WIDTH * 0.5), 100, PISTON_WIDTH,
                    (int) (10 + (sim.pressSim.position / Press.PRESSING_MILLIS) * 30));

            // Paint gun:
            g.setColor(pressColor(sim.paintGun.isHigh()));
            g.fillRect((int) (beltToX(PAINTER_POSITION) - TOOL_BOX_WIDTH * 0.5), 0, TOOL_BOX_WIDTH, 80);
            g.setColor(DARKBLUE);
            g.fillRect((int) (beltToX(PAINTER_POSITION) - TUBE_WIDTH * 0.5), 80, TUBE_WIDTH, 30);

            g.setColor(DARKBLUE);
            g.fillOval((int) (BELT_X - BELT_HEIGHT * 0.5), BELT_TOP, BELT_HEIGHT, BELT_HEIGHT);
            g.setColor(DARKBLUE);
            g.fillOval((int) (BELT_X + BELT_LEN - BELT_HEIGHT * 0.5), BELT_TOP, BELT_HEIGHT, BELT_HEIGHT);
            double sensorWidth = beltToX(sim.sensor1.end) - beltToX(sim.sensor1.start);
            g.setColor(sensorColor(sim.sensor2.signal.isHigh()));
            g.fillRect((int) beltToX(sim.sensor2.start), BELT_TOP + BELT_HEIGHT, (int) sensorWidth, SENSOR_HEIGHT);
            g.setColor(sensorColor(sim.sensor3.signal.isHigh()));
            g.fillRect((int) beltToX(sim.sensor3.start), BELT_TOP + BELT_HEIGHT, (int) sensorWidth, SENSOR_HEIGHT);

            Set<VisualSimItem> missing = new HashSet<>(onConveyor);
            Set<VisualSimItem> newItems = sim.conveyor.currentItems();
            missing.removeAll(newItems);
            newItems.removeAll(onConveyor);
            onConveyor.removeAll(missing);
            onConveyor.addAll(newItems);
            for (VisualSimItem item : onConveyor) {
                item.draw(g);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
    }
}
