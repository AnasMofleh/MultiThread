package lift;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import util.RetinaDisplay;

@SuppressWarnings("serial")
public class LiftView {

    /** Floors are numbered 0 .. NBR_FLOORS-1 */
    public static final int NBR_FLOORS = 7;

    /** Maximal number of passengers in the lift at a time */ 
    public static final int MAX_PASSENGERS = 4;

    static {
        // Makes for better performance on our lab machines.
        // (Must be done before Swing initialization.)
        System.setProperty("sun.java2d.opengl", "True");
    }

    private static final int FLOOR_HEIGHT = 60;
    private static final int PASSENGER_HEIGHT = FLOOR_HEIGHT / 2;
    private static final int PASSENGER_WIDTH = PASSENGER_HEIGHT * 2 / 3;
    private static final int NUMBER_WIDTH = FLOOR_HEIGHT;
    private static final int SHAFT_WIDTH = 9 * PASSENGER_WIDTH / 4;
    private static final int ENTRY_WIDTH = 8 * PASSENGER_WIDTH;
    private static final int EXIT_WIDTH = ENTRY_WIDTH;
    
    private static final int FLOOR_NUMBER_MARGIN = FLOOR_HEIGHT / 4;
    private static final Font FLOOR_NUMBER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, fontSizeInPoints(FLOOR_HEIGHT - 1 - 2 * FLOOR_NUMBER_MARGIN));
    private static final Color BACKGROUND_COLOR = new Color(240, 240, 240);

    private Cage cage;
    private Floor[] floor = new Floor[NBR_FLOORS];

    private static Random rand = new Random();

    /** Create a new LiftView window. The lift starts at floor 0. */
    public LiftView() {
        deferToSwingThread(() -> {
            JFrame f = new JFrame("Lift");
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JLayeredPane pane = f.getLayeredPane();
            for (int i = 0; i < NBR_FLOORS; i++) {
                floor[i] = new Floor(i);
                pane.add(floor[i]);
                
                JLabel l = new JLabel(Integer.toString(i), SwingConstants.CENTER);
                l.setBounds(0, FLOOR_HEIGHT * (NBR_FLOORS - i - 1), NUMBER_WIDTH, FLOOR_HEIGHT);
                l.setForeground(Color.LIGHT_GRAY);
                l.setFont(FLOOR_NUMBER_FONT);
                pane.add(l);
            }

            cage = new Cage();
            pane.add(cage);
            pane.moveToBack(cage);

            f.getContentPane().setPreferredSize(new Dimension(NUMBER_WIDTH + ENTRY_WIDTH + SHAFT_WIDTH + EXIT_WIDTH, FLOOR_HEIGHT * NBR_FLOORS - 1));
            f.getContentPane().setBackground(BACKGROUND_COLOR);
            f.setResizable(false);
            f.pack();
            f.setVisible(true);
        });
    }

    /** Move the lift from floor 'here' to floor 'next'. These two must be different. */
    public void moveLift(int here, int next) {
        checkValidFloor(here);
        checkValidFloor(next);
        checkDifferent(here, next);
        if (here < 0 || here >= NBR_FLOORS || next < 0 || next >= NBR_FLOORS || here == next) {
            throw new IllegalArgumentException("here=" + here + ", next=" + next);
        }
        cage.moveCage(here, next);
    }

    /** Create a new Lift passenger. This method can safely be called from any thread. */
    public Passenger createPassenger() {
        final CompletableFuture<Passenger> passengerStore = new CompletableFuture<>();
        try {
            SwingUtilities.invokeLater(() -> passengerStore.complete(new LiftPassenger()));
            return passengerStore.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new Error(e);
        }
    }

    /**
     * Display the number of passengers waiting to enter and exit on each floor.
     * This method is thread safe -- it can safely be accessed by multiple threads
     * concurrently.
     * 
     * @param nbrEntry  element [i] indicates the number of passengers
     *                  waiting to enter on floor i
     *
     * @param nbrExit   element [i] indicates the number of passengers
     *                  waiting to exit on floor i
     */
    public void showDebugInfo(int[] nbrEntry, int[] nbrExit) {
        if (nbrEntry.length != NBR_FLOORS || nbrExit.length != NBR_FLOORS) {
            throw new IllegalArgumentException("arrays must be of length" + NBR_FLOORS);
        }

        for (int i = 0; i < nbrEntry.length; i++) {
            floor[i].setWaitInfo(nbrEntry[i], nbrExit[i]);
        }
    }

    // =======================================================================

    private static void checkValidFloor(int n) {
        if (n < 0 || n >= NBR_FLOORS) {
            throw new IllegalArgumentException("floor " + n + " (should be in range 0.." + (NBR_FLOORS - 1) + ")");
        }
    }

    private static void checkDifferent(int a, int b) {
        if (a == b) {
            throw new IllegalArgumentException("source floor is equal to destination");
        }
    }
    
    /** Like invokeAndWait, but translate all exceptions to Error */
    private static void deferToSwingThread(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new Error(e);
        }
    }
    
    private static int fontSizeInPoints(int pixelHeight) {
        return pixelHeight * Toolkit.getDefaultToolkit().getScreenResolution() / 72;
    }

    // =======================================================================

    /*
     * A JPanel with a set of positions for a passenger to stand in. The first
     * position is at X == queueStart. Subsequent positions are placed in
     * lower X values, with distance DISTANCE_BETWEEN_POSITIONS between.
     */
    private class PassengerContainer extends JPanel {
        
        private static final int DISTANCE_BETWEEN_POSITIONS = PASSENGER_WIDTH * 4 / 10;

        // positions that are currently allocated
        private final PriorityQueue<Integer> allocated = new PriorityQueue<>();
        
        // positions that were allocated, and then returned
        private final PriorityQueue<Integer> free = new PriorityQueue<>(Collections.reverseOrder());
        
        private final int queueStart;

        private PassengerContainer(int width, int queueStart) {
            super(null);
            this.queueStart = queueStart;
            setSize(new Dimension(width, FLOOR_HEIGHT - 1));
        }
        
        /** @return a suitable X coordinate for a passenger to stand */
        public synchronized int allocatePlaceToStand() {
            if (! free.isEmpty()) {
                int reused = free.poll();
                allocated.add(reused);
                return reused;
            } else if (! allocated.isEmpty()) {
                int last = allocated.peek();
                int x = last - DISTANCE_BETWEEN_POSITIONS;
                allocated.add(x);
                return x;
            } else {
                allocated.add(queueStart);
                return queueStart;
            }
        }
        
        /** A passenger is no longer standing in the indicated X coordinate,
         *  so it can be used by another passenger */
        public synchronized void releasePlaceToStand(int x) {
            if (allocated.remove(x)) {
                free.add(x);
            }
        }
    }

    // =======================================================================

    private static final Font WAIT_INFO_FONT = new Font(Font.SANS_SERIF, Font.BOLD, fontSizeInPoints(FLOOR_HEIGHT / 5));
    
    private static final Color WAIT_INFO_COLOR = new Color(248, 176, 112);
    
    private static final int ENTRY_CIRCLE_X = (ENTRY_WIDTH - FLOOR_HEIGHT * 3 / 5) / 2;
    private static final int EXIT_CIRCLE_X = ENTRY_WIDTH + SHAFT_WIDTH + (EXIT_WIDTH - FLOOR_HEIGHT * 3 / 5) / 2;
    private static final int ENTRY_EXIT_CIRCLE_Y = FLOOR_HEIGHT / 5;
    private static final int ENTRY_EXIT_CIRCLE_SIZE = FLOOR_HEIGHT * 3 / 5;
    
    private class Floor extends PassengerContainer {
        private int nbrEntry = -1; // means no value set
        private int nbrExit = -1;

        public Floor(int floor) {
            super(ENTRY_WIDTH + SHAFT_WIDTH + EXIT_WIDTH, ENTRY_WIDTH - PASSENGER_WIDTH);
            setOpaque(false);

            int y = (NBR_FLOORS - floor - 1) * FLOOR_HEIGHT;
            setLocation(NUMBER_WIDTH, y);
        }
        
        @Override
        public void paintComponent(Graphics g) {
            // super.paintComponent(g);   // not really needed here
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ENTRY_WIDTH, FLOOR_HEIGHT - 1);
            g.fillRect(ENTRY_WIDTH + SHAFT_WIDTH, 0, EXIT_WIDTH, FLOOR_HEIGHT - 1);
            
            if (nbrEntry > 0) {
                g.setFont(WAIT_INFO_FONT);
                g.setColor(WAIT_INFO_COLOR);
                g.fillOval(ENTRY_CIRCLE_X, ENTRY_EXIT_CIRCLE_Y, ENTRY_EXIT_CIRCLE_SIZE, ENTRY_EXIT_CIRCLE_SIZE);
                g.setColor(Color.WHITE);
                g.drawString(Integer.toString(nbrEntry), ENTRY_CIRCLE_X + FLOOR_HEIGHT / 5, FLOOR_HEIGHT * 3 / 5);
            }
            
            if (nbrExit > 0) {
                g.setFont(WAIT_INFO_FONT);
                g.setColor(WAIT_INFO_COLOR);
                g.fillOval(EXIT_CIRCLE_X, ENTRY_EXIT_CIRCLE_Y, ENTRY_EXIT_CIRCLE_SIZE, ENTRY_EXIT_CIRCLE_SIZE);
                g.setColor(Color.WHITE);
                g.drawString(Integer.toString(nbrExit), EXIT_CIRCLE_X + FLOOR_HEIGHT / 5, FLOOR_HEIGHT * 3 / 5);
            }
        }

        private synchronized void setWaitInfo(int nbrEntry, int nbrExit) {
            if (nbrEntry != this.nbrEntry) {
                this.nbrEntry = nbrEntry;
                repaint(ENTRY_CIRCLE_X, ENTRY_EXIT_CIRCLE_Y, ENTRY_EXIT_CIRCLE_SIZE, ENTRY_EXIT_CIRCLE_SIZE);
            }
            if (nbrExit != this.nbrExit) {
                this.nbrExit = nbrExit;
                repaint(EXIT_CIRCLE_X, ENTRY_EXIT_CIRCLE_Y, ENTRY_EXIT_CIRCLE_SIZE, ENTRY_EXIT_CIRCLE_SIZE);
            }
        }
    }

    // =======================================================================
    
    private static enum PassengerState {
        NOT_STARTED, WALKING_IN, WAITING_TO_ENTER,
        ENTERING, IN_LIFT, EXITING, EXITED,
        WALKING_OUT, DONE,
        ERROR;
    }

    // -----------------------------------------------------------------------

    private static final int NBR_FRAMES = 8;

    private static final Color[] PASSENGER_COLORS = {
        new Color(0, 0, 0),
        new Color(0, 0, 224),
        new Color(0, 128, 0),
        new Color(208, 0, 0),
    };

    private static class PassengerConfig {
        final ImageIcon still;
        final ImageIcon[] frames;
        public PassengerConfig(ImageIcon still) {
            this.still = still;
            frames = new ImageIcon[NBR_FRAMES];
        }
    }
    
    private static final ImageIcon disabledIcon;
    
    private static final PassengerConfig[] PASSENGER_CONFIGS = new PassengerConfig[PASSENGER_COLORS.length];
    
    /** An ImageIcon for a passenger, colored, and adjusted for high-resolution displays on Macs. */
    private static class PassengerIcon extends ImageIcon {
        
        private static final boolean USING_RETINA_DISPLAY = RetinaDisplay.detected();
        
        private PassengerIcon(Image original, Color color) {
            super(colorImage(original, color).getScaledInstance(USING_RETINA_DISPLAY ? PASSENGER_WIDTH  * 2 : PASSENGER_WIDTH,
                                                                USING_RETINA_DISPLAY ? PASSENGER_HEIGHT * 2 : PASSENGER_HEIGHT,
                                                                Image.SCALE_SMOOTH));
        }
        
        @Override
        public int getIconWidth() {
            return PASSENGER_WIDTH;
        }
        
        @Override
        public int getIconHeight() {
            return PASSENGER_HEIGHT;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (! USING_RETINA_DISPLAY) {
                super.paintIcon(c, g, x, y);
                return;
            }
            
            Graphics2D g2d = (Graphics2D) g.create(x, y, PASSENGER_WIDTH, PASSENGER_HEIGHT);
            g2d.scale(0.5, 0.5);
            g2d.drawImage(getImage(), 0, 0, null); // don't care about the observer here
            g2d.dispose();
        }
        
        /** Apply the given color to an image */
        private static Image colorImage(Image original, Color color) {
            BufferedImage result = new BufferedImage(original.getWidth(null), original.getHeight(null), BufferedImage.TRANSLUCENT);
            Graphics2D graphics = result.createGraphics();
            graphics.drawImage(original, 0, 0, null);
            graphics.setXORMode(color);
            graphics.drawImage(original, 0, 0, null);
            graphics.dispose();
            return result;
        }
    }
    
    static {
        try {
            Image w0 = ImageIO.read(LiftView.class.getResource("/w0.png"));

            disabledIcon = new PassengerIcon(w0, Color.LIGHT_GRAY);
            for (int k = 0; k < PASSENGER_CONFIGS.length; k++) {
                ImageIcon w0color = new PassengerIcon(w0, PASSENGER_COLORS[k]);
                PASSENGER_CONFIGS[k] = new PassengerConfig(w0color);
            }
            for (int i = 1; i <= NBR_FRAMES; i++) {
                Image wi = ImageIO.read(LiftView.class.getResource("/w" + i + ".png"));
                for (int k = 0; k < PASSENGER_CONFIGS.length; k++) {
                    PASSENGER_CONFIGS[k].frames[i - 1] = new PassengerIcon(wi, PASSENGER_COLORS[k]);
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    // -----------------------------------------------------------------------
    
    private static final Font PASSENGER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, fontSizeInPoints((FLOOR_HEIGHT - PASSENGER_HEIGHT) / 4));
    private static final Color PASSENGER_FONT_COLOR = Color.BLACK;
    
    private class LiftPassenger extends JPanel implements Passenger {
        private static final int STEP_SIZE = PASSENGER_WIDTH / NBR_FRAMES;

        private JLabel imageLabel = new JLabel();
        private JLabel destLabel = new JLabel();

        private int frame = 0;
        private int from, to;
        private int stepDelay;
        private int placeToStand;
        private PassengerState state = PassengerState.NOT_STARTED;

        private PassengerConfig config;

        public LiftPassenger() {
            setSize(new Dimension(PASSENGER_WIDTH, FLOOR_HEIGHT));

            from = rand.nextInt(NBR_FLOORS);
            to = rand.nextInt(NBR_FLOORS - 1);
            if (from == to) {
                to++;
            }

            stepDelay = 50 + rand.nextInt(10);
            config = PASSENGER_CONFIGS[rand.nextInt(PASSENGER_CONFIGS.length)];

            setOpaque(false);
            setLayout(new BorderLayout());
            add(imageLabel, BorderLayout.PAGE_END);
            destLabel = new JLabel(Integer.toString(to), SwingConstants.CENTER);
            destLabel.setFont(PASSENGER_FONT);
            destLabel.setForeground(PASSENGER_FONT_COLOR);
            add(destLabel, BorderLayout.CENTER);
        }

        private void walkTo(int xDestination, boolean stopOnArrival) {
            int nbrSteps = (xDestination - getX()) / STEP_SIZE;
            for (int i = 0; i < nbrSteps; i++) {
                advanceImage(STEP_SIZE, 1, stepDelay);
            }

            if (stopOnArrival) {
                while (frame != 1) {
                    int dx = (getX() < xDestination) ? 1 : 0;
                    int direction = (frame <= NBR_FRAMES / 2 && frame != 0) ? - 1 : 1;
                    advanceImage(dx, direction, stepDelay / 2);
                }
                deferToSwingThread(() -> {
                    setLocation(xDestination, 0);
                    imageLabel.setIcon(config.still);
                });
                frame = 0; // so we get back to frame 1 next time
            }
        }

        // dx = movement on X axis, direction = +1/-1 (next/previous frame), delay is in ms
        private void advanceImage(int dx, int direction, long delay) {
            try {
                frame = Math.floorMod(frame + direction, NBR_FRAMES);
                deferToSwingThread(() -> {
                    setLocation(getX() + dx, 0);
                    imageLabel.setIcon(config.frames[frame]);
                });
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }

        // if the lift cage is not level with floor f, the passenger is moved to its 'from' floor
        // 'returnTo' and assumes an error state
        private void checkLiftLevelWith(Floor f) {
            try {
                if (cage.getY() != f.getY()) {
                    String s = state.toString();
                    state = PassengerState.ERROR;
                    PassengerContainer parent = (PassengerContainer) getParent();
                    deferToSwingThread(() -> {
                        parent.releasePlaceToStand(placeToStand);
                        parent.remove(this);
                        floor[from].add(this);
                        imageLabel.setIcon(disabledIcon);
                        destLabel.setText("!?");
                        destLabel.setForeground(Color.RED);
                    });
                    placeToStand = floor[from].allocatePlaceToStand();
                    int yDist = getY() + parent.getY() - floor[from].getY();
                    int xDist = getX() - placeToStand;
                    for (int x = getX(); x > placeToStand; x -= STEP_SIZE) {
                        final int ax = x;
                        int y = (yDist * (x - placeToStand)) / xDist;
                        deferToSwingThread(() -> setLocation(ax, y));
                        Thread.sleep(stepDelay / 2);
                    }
                    deferToSwingThread(() -> setLocation(placeToStand, 0));
                    throw new IllegalStateException("Lift not level with floor (state " + s + ")");
                }
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }

        private void checkStateTransition(PassengerState current, PassengerState next) {
            if (current != state) {
                throw new IllegalStateException("invalid state: " + state + "; method can only be invoked in state " + current);
            }
            state = next;
        }

        @Override
        public int getStartFloor() {
            return from;
        }

        @Override
        public int getDestinationFloor() {
            return to;
        }

        @Override
        public void begin() {
            checkStateTransition(PassengerState.NOT_STARTED, PassengerState.WALKING_IN);

            deferToSwingThread(() -> {
                floor[from].add(this);
                setLocation(-PASSENGER_WIDTH, 0);
            });
            placeToStand = floor[from].allocatePlaceToStand();
            walkTo(placeToStand, true);

            checkStateTransition(PassengerState.WALKING_IN, PassengerState.WAITING_TO_ENTER);
        }

        @Override
        public void enterLift() {
            checkStateTransition(PassengerState.WAITING_TO_ENTER, PassengerState.ENTERING);

            floor[from].releasePlaceToStand(placeToStand);

            checkLiftLevelWith(floor[from]);
            
            placeToStand = cage.allocatePlaceToStand();

            walkTo(placeToStand + (cage.getX() - floor[from].getX()), true);

            deferToSwingThread(() -> {
                floor[from].remove(this);
                cage.add(this);
                setLocation(getX() + (floor[from].getX() - cage.getX()), getY());
            });

            checkLiftLevelWith(floor[from]);

            checkStateTransition(PassengerState.ENTERING, PassengerState.IN_LIFT);
        }

        @Override
        public void exitLift() {
            checkStateTransition(PassengerState.IN_LIFT, PassengerState.EXITING);

            cage.releasePlaceToStand(placeToStand);

            checkLiftLevelWith(floor[to]);

            deferToSwingThread(() -> {
                cage.remove(this);
                floor[to].add(this);
                setLocation(getX() + (cage.getX() - floor[to].getX()), getY());
            });
            
            walkTo(ENTRY_WIDTH + SHAFT_WIDTH, false);

            checkLiftLevelWith(floor[to]);

            checkStateTransition(PassengerState.EXITING, PassengerState.EXITED);
        }

        @Override
        public void end() {
            checkStateTransition(PassengerState.EXITED, PassengerState.WALKING_OUT);

            walkTo(ENTRY_WIDTH + SHAFT_WIDTH + EXIT_WIDTH + 2 /* avoid strange residue */, false);
            deferToSwingThread(() -> floor[to].remove(this));

            checkStateTransition(PassengerState.WALKING_OUT, PassengerState.DONE);
        }
    }

    // =======================================================================
    
    private class Cage extends PassengerContainer {

        private static final int INCREMENT                = 3;
        private static final int DELAY_BETWEEN_INCREMENTS = 60;

        public Cage() {
            super(SHAFT_WIDTH - 2, SHAFT_WIDTH - PASSENGER_WIDTH);
            setOpaque(true);
            setBackground(Color.WHITE);
            setLocation(NUMBER_WIDTH + ENTRY_WIDTH + 1, floor[0].getY());
        }

        public void moveCage(int from, int to) {
            int start = floor[from].getY();
            int stop = floor[to].getY();
            try {
                if (start < stop) {
                    for (int y = start; y < stop; y += INCREMENT) {
                        final int y2 = y;
                        deferToSwingThread(() -> setLocation(getX(), y2));
                        Thread.sleep(DELAY_BETWEEN_INCREMENTS);
                    }
                } else {
                    for (int y = start; y > stop; y -= INCREMENT) {
                        final int y2 = y;
                        deferToSwingThread(() -> setLocation(getX(), y2));
                        Thread.sleep(DELAY_BETWEEN_INCREMENTS);
                    }
                }
                deferToSwingThread(() -> setLocation(getX(), stop));
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }
    }
}
