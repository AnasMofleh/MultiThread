package emulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import clock.ClockOutput;

/** UI for the alarm clock simulation. */
@SuppressWarnings({"deprecation","serial"})
class LedDisplay extends JPanel implements KeyListener, ClockOutput, Observer {
    private static final int LARGE_DIGIT_WIDTH   = 68;
    private static final int LARGE_DIGIT_HEIGHT  = 90;
    private static final int LARGE_COLON_WIDTH   = 22;

    private static final int SMALL_DIGIT_HEIGHT  = LARGE_DIGIT_HEIGHT / 2;
    private static final int SMALL_DIGIT_WIDTH   = LARGE_DIGIT_WIDTH  / 2;
    private static final int SMALL_COLON_WIDTH   = LARGE_COLON_WIDTH * SMALL_DIGIT_WIDTH / LARGE_DIGIT_WIDTH;
    private static final int DISPLAY_MARGIN      = LARGE_DIGIT_WIDTH  / 2;
    
    private static final Dimension DISPLAY_SIZE
        = new Dimension(2 * DISPLAY_MARGIN + 6 * LARGE_DIGIT_WIDTH + 2 * LARGE_COLON_WIDTH,
                        3 * DISPLAY_MARGIN + LARGE_DIGIT_HEIGHT + SMALL_DIGIT_HEIGHT);
    
    private static final Color ALARM_COLOR = new Color(96, 0, 0);

    private static final int DIGIT_BLINK_CYCLE = 500;

    private final Thread blinkThread = new Thread(this::blinkMain);

    private final Icon[] largeDigitIcons = new Icon[10];
    private final Icon[] smallDigitIcons = new Icon[10];

    private final JLabel[] timeDisplay = new JLabel[6];
    private final JLabel[] alarmDisplay = new JLabel[6];
    private final JLabel alarmStatus;

    private final JLabel rateLabel = new JLabel(" ");
    private final JLabel alarmLabel = new JLabel("ALARM", JLabel.RIGHT);

    private final ClockState state;

    /** Private constructor: use factory method 'createViewFor' below */
    private LedDisplay(ClockState state) {
        super(null);
        
        try {
            this.state = state;

            // set up the frame
            
            for (int i = 0; i < 10; i++) {
                Image img = ImageIO.read(getClass().getResource("/" + i + ".png"));
                largeDigitIcons[i] = new ImageIcon(img.getScaledInstance(LARGE_DIGIT_WIDTH, LARGE_DIGIT_HEIGHT, Image.SCALE_SMOOTH));
                smallDigitIcons[i] = new ImageIcon(img.getScaledInstance(-1, SMALL_DIGIT_HEIGHT, Image.SCALE_SMOOTH));
            }

            for (int i = 0; i < timeDisplay.length; i++) {
                timeDisplay[i] = new JLabel(largeDigitIcons[0]);
                timeDisplay[i].setBounds(DISPLAY_MARGIN + i * LARGE_DIGIT_WIDTH + LARGE_COLON_WIDTH * (i / 2), DISPLAY_MARGIN, LARGE_DIGIT_WIDTH, LARGE_DIGIT_HEIGHT);
                add(timeDisplay[i]);
                
                alarmDisplay[i] = new JLabel(smallDigitIcons[0]);
                alarmDisplay[i].setBounds(DISPLAY_MARGIN + i * SMALL_DIGIT_WIDTH + SMALL_COLON_WIDTH * (i / 2), LARGE_DIGIT_HEIGHT + 2 * DISPLAY_MARGIN, SMALL_DIGIT_WIDTH, SMALL_DIGIT_HEIGHT);
                add(alarmDisplay[i]);
            }

            Icon alarmIcon = new ImageIcon(ImageIO.read(getClass().getResource("/alarm.png")).getScaledInstance(-1, SMALL_DIGIT_HEIGHT, Image.SCALE_SMOOTH));

            alarmStatus = new JLabel(alarmIcon);
            alarmStatus.setBounds(DISPLAY_SIZE.width - DISPLAY_MARGIN - alarmIcon.getIconWidth(), LARGE_DIGIT_HEIGHT + 2 * DISPLAY_MARGIN, alarmIcon.getIconWidth(), alarmIcon.getIconHeight()); 
            alarmStatus.setVisible(false); // alarm initially off
            add(alarmStatus);

            Image img = ImageIO.read(getClass().getResource("/colon.png"));
            Icon largeColonImage = new ImageIcon(img.getScaledInstance(-1, LARGE_DIGIT_HEIGHT, Image.SCALE_SMOOTH));
            Icon smallColonImage = new ImageIcon(img.getScaledInstance(-1, SMALL_DIGIT_HEIGHT, Image.SCALE_SMOOTH));

            JLabel colon1 = new JLabel(largeColonImage);
            colon1.setBounds(DISPLAY_MARGIN + 2 * LARGE_DIGIT_WIDTH, DISPLAY_MARGIN, LARGE_COLON_WIDTH, LARGE_DIGIT_HEIGHT);
            add(colon1);
            JLabel colon2 = new JLabel(largeColonImage);
            colon2.setBounds(DISPLAY_MARGIN + 4 * LARGE_DIGIT_WIDTH + LARGE_COLON_WIDTH, DISPLAY_MARGIN, LARGE_COLON_WIDTH, LARGE_DIGIT_HEIGHT);
            add(colon2);

            JLabel colon3 = new JLabel(smallColonImage);
            colon3.setBounds(DISPLAY_MARGIN + 2 * SMALL_DIGIT_WIDTH, LARGE_DIGIT_HEIGHT + 2 * DISPLAY_MARGIN, SMALL_COLON_WIDTH, SMALL_DIGIT_HEIGHT);
            add(colon3);
            JLabel colon4 = new JLabel(smallColonImage);
            colon4.setBounds(DISPLAY_MARGIN + 4 * SMALL_DIGIT_WIDTH + SMALL_COLON_WIDTH, LARGE_DIGIT_HEIGHT + 2 * DISPLAY_MARGIN, SMALL_COLON_WIDTH, SMALL_DIGIT_HEIGHT);
            add(colon4);

            requestFocus();
            setBackground(Color.BLACK);

            // create a window to hold this frame

            JFrame frame = new JFrame("Alarm Clock");
            frame.setLayout(new BorderLayout());
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.addKeyListener(this);
            frame.getContentPane().add(this, BorderLayout.CENTER);

            JPanel buttonBox = new JPanel(new BorderLayout());
            alarmLabel.setVisible(false);
            buttonBox.add(rateLabel, BorderLayout.WEST);
            buttonBox.add(alarmLabel, BorderLayout.EAST);
            buttonBox.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            frame.getContentPane().add(buttonBox, BorderLayout.SOUTH);

            frame.setLocationRelativeTo(null);
            frame.pack();
            frame.setVisible(true);

            blinkThread.start();
        } catch (IOException e) {
            throw new Error("unable to read LED images from JAR file", e);
        }
    }
    
    /** Factory method */
    public static LedDisplay createViewFor(ClockState state) {
        // perform initialization in the Swing UI thread
        final CompletableFuture<LedDisplay> displayStore = new CompletableFuture<>();
        try {
            SwingUtilities.invokeLater(() -> {
                LedDisplay ld = new LedDisplay(state);
                state.addObserver(ld);
                displayStore.complete(ld);
            });
            return displayStore.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new Error(e);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return DISPLAY_SIZE;
    }

    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(() -> {
            final int[] t = state.getTime();
            for (int i = 0; i < t.length; i++) {
                Icon current = timeDisplay[i].getIcon();
                Icon wanted = largeDigitIcons[t[i]];
                if (current != wanted) {
                    timeDisplay[i].setIcon(wanted);
                }
            }
            final int[] a = state.getAlarm();
            for (int i = 0; i < a.length; i++) {
                Icon current = alarmDisplay[i].getIcon();
                Icon wanted = smallDigitIcons[a[i]];
                if (current != wanted) {
                    alarmDisplay[i].setIcon(wanted);
                }
            }
            boolean alarmOn = state.alarmIndicatorIsOn();
            if (alarmStatus.isVisible() != alarmOn) {
                alarmStatus.setVisible(alarmOn);
            }
        });
    }

    /** Thread for our crude animation (LED digit blinking while editable) */
    private void blinkMain() {
        while (true) {
            int td = state.currentlyEditedTimeDigit(); 
            int ad = state.currentlyEditedAlarmDigit(); 
            try {
                if (td >= 0) {
                    SwingUtilities.invokeLater(() -> timeDisplay[td].setVisible(! timeDisplay[td].isVisible()));
                }
                if (ad >= 0) {
                    SwingUtilities.invokeLater(() -> alarmDisplay[ad].setVisible(! alarmDisplay[ad].isVisible()));
                }
                Thread.sleep(DIGIT_BLINK_CYCLE);
            } catch (InterruptedException ie) {
                if (td >= 0) {
                    SwingUtilities.invokeLater(() -> timeDisplay[td].setVisible(true));
                }
                if (ad >= 0) {
                    SwingUtilities.invokeLater(() -> alarmDisplay[ad].setVisible(true));
                }
            }
        }
    }

    // ================================================= interface KeyListener
    
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_CONTROL:
        case KeyEvent.VK_ALT:
            state.setAlarmPressed();
            blinkThread.interrupt();
            break;
        case KeyEvent.VK_SHIFT:
            state.setTimePressed();
            blinkThread.interrupt();
            break;
        case KeyEvent.VK_DOWN:
            state.downPressed();
            blinkThread.interrupt();
            break;
        case KeyEvent.VK_UP:
            state.upPressed();
            blinkThread.interrupt();
            break;
        case KeyEvent.VK_LEFT:
            state.leftPressed();
            blinkThread.interrupt();
            break;
        case KeyEvent.VK_RIGHT:
            state.rightPressed();
            blinkThread.interrupt();
            break;
        }

        update(state, null);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_CONTROL:
        case KeyEvent.VK_ALT:
            state.setAlarmReleased();
            blinkThread.interrupt();
            break;
        case KeyEvent.VK_SHIFT:
            state.setTimeReleased();
            blinkThread.interrupt();
            break;
        }

        update(state, null);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // ignored
    }

    // ================================================= interface ClockOutput

    // Keep track of clock updates, for measuring clock rate
    private static final int MAX_SAMPLES = 10;
    private static final int MIN_SAMPLES = 5;
    private final List<Sample> samples = new ArrayList<>();

    private int lastTimeSet = -1;

    /** A sample of (system time, alarmclock time) */
    private class Sample {
        double x;
        int y;

        public Sample(double x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @Override
    public void alarm() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                alarmLabel.setVisible(true);
                setBackground(ALARM_COLOR);
            });
            Thread.sleep(DIGIT_BLINK_CYCLE);
            SwingUtilities.invokeAndWait(() -> {
                alarmLabel.setVisible(false);
                setBackground(Color.BLACK);
            });
        } catch (InterruptedException | InvocationTargetException unexpected) {
            throw new Error(unexpected);
        }
    }

    /**
     * Update clock statistics and return results.
     *
     * @return array with two elements: clock rate and update interval, or null if
     *         not enough samples available
     */
    private double[] clockStatistics(int hhmmss) {
        int hh = hhmmss / 10000;
        int mm = (hhmmss / 100) % 100;
        int ss = hhmmss % 100;
        int secs = ss + mm * 60 + hh * 3600;

        // if the clock seems to be changed by the user, reset the statistics
        if (secs != lastTimeSet && secs != lastTimeSet + 1) {
            samples.clear();

            // Ignore first call to displayTime, and first call after changing
            // the time. In some solutions, displayTime is called in response
            // to a button press -- that is, out of the usual once-per-second
            // loop. By ignoring the first call, we prevent such spurious
            // calls from upsetting the statistics. (You could argue this
            // would be a timing bug in the application, but this particular
            // issue is not what we want students to focus their time on.)

            lastTimeSet = secs;
            return null;
        }
        lastTimeSet = secs;

        samples.add(new Sample(System.nanoTime() / 1e9, secs));
        if (samples.size() < MIN_SAMPLES) {
            return null;
        } else if (samples.size() > MAX_SAMPLES) {
            samples.remove(0);
        }
        int n = samples.size();

        // linear regression: x = system time, y = alarmclock time
        double sx = samples.stream().mapToDouble(s -> s.x).sum();
        double sxx = samples.stream().mapToDouble(s -> s.x * s.x).sum();
        double sy = samples.stream().mapToDouble(s -> s.y).sum();
        double sxy = samples.stream().mapToDouble(s -> s.x * s.y).sum();
        double rate = (n * sxy - sx * sy) / (n * sxx - sx * sx);

        // linear regression: x = sample index, y = system time
        double tx = IntStream.range(0, n).sum();
        double txx = IntStream.range(0, n).mapToDouble(x -> x * x).sum();
        double ty = sx;
        double txy = IntStream.range(0, n).mapToDouble(x -> x * samples.get(x).x).sum();
        double interval = (n * txy - tx * ty) / (n * txx - tx * tx);

        return new double[] { rate, interval };
    }
    
    @Override
    public void displayTime(int hhmmss) {
        state.setTimeValue(hhmmss);
        blinkThread.interrupt();
        double[] r = clockStatistics(hhmmss);
        SwingUtilities.invokeLater(() -> {
            if (r == null) {
                rateLabel.setText("Statistics currently unavailable – awaiting more calls to displayTime()");
            } else {
                rateLabel.setText("Clock rate: "
                        + String.format(Locale.US, "%2.3f", r[0])
                        + "    Update interval: "
                        + String.format(Locale.US, "%2.3fs", r[1]));
            }
        });
        // disturbance to test time calculations in students' solutions
        try {
            long naughty = (long) (Math.random() * 3);
            Thread.sleep(naughty);
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }

    @Override
    public void setAlarmIndicator(boolean on) {
        state.setAlarmStatus(on);
    }
}