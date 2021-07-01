package simulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;

import util.RetinaDisplay;
import wash.WashingIO;

// washing machine image from Pixabay:
// https://pixabay.com/es/service/license/

@SuppressWarnings({"deprecation", "serial"})
class WashingView extends JFrame {

    private static final Dimension MACHINE_SIZE      = new Dimension(454, 508);
    private static final Rectangle BARREL_RECT       = new Rectangle(MACHINE_SIZE.width * 239 / 908,
                                                                     MACHINE_SIZE.height * 356 / 1016,
                                                                     MACHINE_SIZE.width * 414 / 908,
                                                                     MACHINE_SIZE.height * 442 / 1016);
    private static final Rectangle PANEL_RECT        = new Rectangle(MACHINE_SIZE.width * 336 / 908,
                                                                     MACHINE_SIZE.height * 33 / 1016,
                                                                     MACHINE_SIZE.width * 547 / 908,
                                                                     MACHINE_SIZE.height * 119 / 1016);
    
    private static final Rectangle ERROR_RECT        = new Rectangle(10,
                                                                     MACHINE_SIZE.height * 2 / 5,
                                                                     MACHINE_SIZE.width - 20,
                                                                     MACHINE_SIZE.height * 1 / 5);
    private static final Font      ERROR_FONT        = new Font(Font.SANS_SERIF,
                                                                Font.BOLD,
                                                                (ERROR_RECT.height / 5) * Toolkit.getDefaultToolkit().getScreenResolution() / 72);

    private static final Rectangle HACKER_PANEL_RECT = new Rectangle(0,
                                                                     MACHINE_SIZE.height * 915 / 1016,
                                                                     MACHINE_SIZE.width,
                                                                     MACHINE_SIZE.height * 100 / 1016);

    private static final Font INDICATOR_FONT         = new Font(Font.MONOSPACED, Font.BOLD, 13);
    
    private static final Color WATER_COLOR           = new Color(0.3f, 0.5f, 1.0f, 0.5f);
    private static final Color INDICATOR_ON_COLOR    = Color.GREEN;

    private static final Border PANEL_BORDER         = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createLoweredBevelBorder());
    
    private final Image machineImage;
    private final BufferedImage[] clothesImages = new BufferedImage[WashingState.NBR_CLOTHES_FRAMES];
    private final BufferedImage[] shrunkImages  = new BufferedImage[WashingState.NBR_CLOTHES_FRAMES];
    
    private final WashingState state;

    /** Private constructor: use factory method 'createViewFor' below  */
    private WashingView(WashingState state) {
        super("Washing Machine");
        this.state = state;

        try {
            boolean usingRetinaDisplay = RetinaDisplay.detected();
            
            int machineWidth  = usingRetinaDisplay ? MACHINE_SIZE.width * 2  : MACHINE_SIZE.width;
            int machineHeight = usingRetinaDisplay ? MACHINE_SIZE.height * 2 : MACHINE_SIZE.height;
            int barrelX       = usingRetinaDisplay ? BARREL_RECT.x * 2       : BARREL_RECT.x;
            int barrelWidth   = usingRetinaDisplay ? BARREL_RECT.width * 2   : BARREL_RECT.width;
            int barrelHeight  = usingRetinaDisplay ? BARREL_RECT.height * 2  : BARREL_RECT.height;
            machineImage = ImageIO.read(WashingView.class.getResource("/machine.png")).getScaledInstance(machineWidth, machineHeight, Image.SCALE_SMOOTH);

            Image shirt = ImageIO.read(WashingView.class.getResource("/shirt.png")).getScaledInstance(barrelWidth * 2 / 3, barrelHeight * 2 / 3, Image.SCALE_SMOOTH);
            int centerX = machineWidth / 2 - barrelX; // hatch is not quite symmetric on X axis
            int centerY = barrelHeight / 2;
            for (int i = 0; i < WashingState.NBR_CLOTHES_FRAMES; i++) {
                clothesImages[i] = new BufferedImage(barrelWidth, barrelHeight, BufferedImage.TRANSLUCENT);
                Graphics2D g1 = clothesImages[i].createGraphics();
                g1.rotate(i * 2 * Math.PI / WashingState.NBR_CLOTHES_FRAMES, centerX, centerY);
                g1.drawImage(shirt, centerX - shirt.getWidth(null) / 2, centerY - shirt.getHeight(null) / 2, null);
                g1.dispose();

                shrunkImages[i] = new BufferedImage(barrelWidth, barrelHeight, BufferedImage.TRANSLUCENT);
                Graphics2D g2 = shrunkImages[i].createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.rotate(i * 2 * Math.PI / WashingState.NBR_CLOTHES_FRAMES, centerX, centerY);
                g2.translate(centerX / 2, centerY / 2);
                g2.scale(0.5, 0.5);
                g2.drawImage(shirt, centerX - shirt.getWidth(null) / 2, centerY - shirt.getHeight(null) / 2, null);
                g2.dispose();
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        
        add(new MachinePanel(), BorderLayout.CENTER);
        
        setLocationRelativeTo(null);
        setVisible(true);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
    }
    
    /** Factory method */
    public static WashingView createViewFor(WashingState state) {
        final CompletableFuture<WashingView> displayStore = new CompletableFuture<>();
        try {
            SwingUtilities.invokeLater(() -> displayStore.complete(new WashingView(state)));
            return displayStore.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new Error(e);
        }
    }

    // -----------------------------------------------------------------------

    private class MachinePanel extends JPanel implements Observer {

        // cached state, values chosen to force initial update
        private double level = -1;
        private int frame = -1;
        private boolean shrunk = false;

        private MachinePanel() {
            super(null);

            JPanel subpanel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.weightx = c.weighty = 1.0;
            c.gridx = c.gridy = 0;
            c.anchor = GridBagConstraints.CENTER;

            subpanel.add(new StringIndicator(() -> String.format(Locale.US, "%04.1f\u00B0C", state.getTemperature())), c);

            c.gridwidth = 2;
            c.gridx++;
            subpanel.add(new TemperatureBar(), c);

            c.gridx += 2;
            c.gridwidth = 1;
            subpanel.add(new StringIndicator(() -> {
                if (state.getCurrentProgram() == 0) {
                    return "-:--";
                } else {
                    int mins = state.getElapsedMinutes();
                    return String.format(Locale.US, "%d:%02d", (mins / 60), (mins % 60));
                }
            }), c);

            c.gridx = 0;
            c.gridy++;
            c.weighty = 0;

            JToggleButton[] buttons = new JToggleButton[4];
            final ButtonGroup group = new ButtonGroup();

            buttons[0] = new JToggleButton("STOP");
            buttons[0].setForeground(Color.RED);
            buttons[0].addActionListener(e -> {
                for (int k = 1; k <= 3; k++) {
                    buttons[k].setEnabled(true);
                }
                buttons[0].setEnabled(false);
                group.clearSelection();
                state.submitButtonPress(0);
            });
            buttons[0].setFocusPainted(false);
            buttons[0].setEnabled(false);
            subpanel.add(buttons[0], c);
            group.add(buttons[0]);

            for (int i = 1; i <= 3; i++) {
                c.gridx++;
                final int n = i;
                buttons[i] = new JToggleButton("P" + i);
                // make all buttons the same size
                buttons[i].setPreferredSize(buttons[0].getPreferredSize());
                buttons[i].addActionListener(e -> {
                    for (int k = 1; k <= 3; k++) {
                        if (n != k) {
                            buttons[k].setEnabled(false);
                        }
                    }
                    buttons[0].setSelected(false);
                    buttons[0].setEnabled(true);

                    state.submitButtonPress(n);
                });
                buttons[i].setFocusPainted(false);
                subpanel.add(buttons[i], c);
                group.add(buttons[i]);
            }

            subpanel.setBorder(PANEL_BORDER);
            subpanel.setBounds(PANEL_RECT);

            add(subpanel);

            add(new ErrorLabel());

            final JPanel hackerPanel = new HackerPanel();
            add(hackerPanel);

            // make sure we pick up keypresses, independent of focus
            KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            manager.addKeyEventDispatcher(new KeyEventDispatcher() {
                @Override
                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        switch (e.getKeyCode()) {
                        case KeyEvent.VK_0:
                            buttons[0].doClick();
                            break;
                        case KeyEvent.VK_1:
                            buttons[1].doClick();
                            break;
                        case KeyEvent.VK_2:
                            buttons[2].doClick();
                            break;
                        case KeyEvent.VK_3:
                            buttons[3].doClick();
                            break;
                        case KeyEvent.VK_H:
                            hackerPanel.setVisible(!hackerPanel.isVisible());
                            break;
                        }
                    }
                    return false;
                }
            });

            state.addObserver(this);
        }

        @Override
        public void update(Observable o, Object arg) {
            double newLevel = state.getWaterLevel();
            int newFrame = state.getFrame();
            boolean newShrunk = state.clothesShrunk();
            
            if (newLevel != level || frame != newFrame || shrunk != newShrunk) {
                repaint(BARREL_RECT);
                level = newLevel;
                frame = newFrame;
                shrunk = newShrunk;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return MACHINE_SIZE;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.drawImage((state.clothesShrunk() ? shrunkImages : clothesImages)[state.getFrame()],
                    BARREL_RECT.x, BARREL_RECT.y, BARREL_RECT.width, BARREL_RECT.height, this);

            g.setColor(WATER_COLOR);
            g.fillRect(BARREL_RECT.x,
                       BARREL_RECT.y + (int) (BARREL_RECT.height * (1 - state.getWaterLevel() / WashingIO.MAX_WATER_LEVEL)),
                       BARREL_RECT.width,
                       (int) (BARREL_RECT.height * state.getWaterLevel() / WashingIO.MAX_WATER_LEVEL));

            g.drawImage(machineImage, 0, 0, MACHINE_SIZE.width, MACHINE_SIZE.height, this);
        }

        // -----------------------------------------------------------------------

        private class TemperatureBar extends JPanel implements Observer {
            private final AtomicInteger value = new AtomicInteger(0);
            private final Rectangle bounds = new Rectangle();
            
            private TemperatureBar() {
                super(null);
                state.addObserver(this);
                setOpaque(true);
                setBackground(Color.BLACK);
                update(state, null);
            }
            
            @Override
            public void update(Observable o, Object arg) {
                int newValue = (int) state.getTemperature();
                if (value.getAndSet(newValue) != newValue) {
                    repaint();
                }
            }
            
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                int v = value.get();
                
                if (v >= WashingState.WHITE_WASH_TEMP) {
                    g.setColor(Color.RED);
                } else if (v >= WashingState.WHITE_WASH_TEMP - 2) {
                    g.setColor(Color.WHITE);
                } else if (v >= WashingState.COLOR_WASH_TEMP) {
                    g.setColor(INDICATOR_ON_COLOR);
                } else if (v >= WashingState.COLOR_WASH_TEMP - 2) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(INDICATOR_ON_COLOR);
                }

                getBounds(bounds);
                
                int margin = bounds.height * 2 / 5;
                int totalWidth = bounds.width;
                int drawWidth = totalWidth * v / WashingState.MAX_TEMP;
                g.fillRect(0, margin, drawWidth, bounds.height - 2 * margin);
                
                g.setColor(Color.DARK_GRAY);
                g.fillRect(drawWidth, margin, totalWidth - drawWidth, bounds.height - 2 * margin);
            }
        }
        
        // -----------------------------------------------------------------------
    
        private class StringIndicator extends JLabel implements Observer {
            private final Supplier<String> expr;
            private volatile String value = "";
            
            private StringIndicator(Supplier<String> expr) {
                super("", SwingConstants.CENTER);
                this.expr = expr;
                setFont(INDICATOR_FONT);
                setForeground(INDICATOR_ON_COLOR);
                setBackground(Color.BLACK);
                setOpaque(true);
                
                state.addObserver(this);
                update(state, null);
            }
            
            @Override
            public void update(Observable o, Object arg) {
                String newValue = expr.get();
                if (! newValue.equals(value)) {
                    value = newValue;
                    SwingUtilities.invokeLater(() -> setText(newValue));
                }
            }
        }

        // -----------------------------------------------------------------------
    
        private class ErrorLabel extends JLabel implements Observer {

            private static final int ERROR_FLASH_PERIOD = 500;
            
            private ErrorLabel() {
                super("", SwingConstants.CENTER);
                setOpaque(true);
                setBounds(ERROR_RECT);
                setFont(ERROR_FONT);
                setBackground(Color.RED);
                setForeground(Color.WHITE);
                setVisible(false);

                state.addObserver(this);
            }

            @Override
            public void update(Observable o, Object arg) {
                final String s = state.getError();
                if (s != null) {
                    SwingUtilities.invokeLater(() -> {
                        state.deleteObserver(this);
                        setText(s);
                        setVisible(true);
                        
                        new Timer(ERROR_FLASH_PERIOD, e -> setVisible(! isVisible())).start();
                    });
                }
            }
        }
    }
    
    // -----------------------------------------------------------------------

    private class HackerPanel extends JPanel {
        private final ButtonGroup buttonGroup = new ButtonGroup(); 
        
        private AbstractButton createOnOffButton(String s, Consumer<Boolean> setter, BooleanSupplier getter) {
            JToggleButton b = new JToggleButton(s);
            b.setFocusPainted(false);
            b.addActionListener((e) -> {
                boolean flag = b.isSelected();
                setter.accept(flag);
            });
            state.addObserver((o, a) -> {
                final boolean newState = getter.getAsBoolean();
                SwingUtilities.invokeLater(() -> {
                    if (newState != b.isSelected()) {
                        b.setSelected(newState);
                    }
                });
            });
            return b;
        }
        
        private AbstractButton createRadioButton(String s, int spinMode, boolean selected) {
            JToggleButton b = new JToggleButton(s, selected);
            b.setFocusPainted(false);
            b.addActionListener((e) -> state.setSpinMode(spinMode));
            state.addObserver((o, a) -> {
                final boolean newState = (state.getSpinMode() == spinMode);
                SwingUtilities.invokeLater(() -> {
                    if (newState != b.isSelected()) {
                        b.setSelected(newState);
                    }
                });
            });
            buttonGroup.add(b);
            return b;
        }
        
        private HackerPanel() {
            super(new GridLayout(2, 4));

            add(createOnOffButton("Heat",       state::heat,  state::isHeating));
            add(createOnOffButton("Fill",       state::fill,  state::isFilling));
            add(createOnOffButton("Drain",      state::drain, state::isDraining));
            add(createOnOffButton("Lock",       state::lock,  state::isLocked));
            add(createRadioButton("Spin OFF",   WashingIO.SPIN_IDLE,  true));
            add(createRadioButton("Spin LEFT",  WashingIO.SPIN_LEFT,  false));
            add(createRadioButton("Spin RIGHT", WashingIO.SPIN_RIGHT, false));
            add(createRadioButton("Spin FAST",  WashingIO.SPIN_FAST,  false));
            
            setBounds(HACKER_PANEL_RECT);
            setBorder(PANEL_BORDER);

            setVisible(false);
            setOpaque(false);
        }
    }
}
