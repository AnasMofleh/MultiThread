package chat.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** A window-based (Swing) chat client. */
@SuppressWarnings("serial")
public class ChatWindow extends JFrame {

    private static final Dimension TOPICS_SIZE = new Dimension(300, 500);
    private static final Dimension MESSAGES_SIZE = new Dimension(600, 500);

    private static final int TOPIC_HEIGHT = 36;
    private static final int MESSAGE_HEIGHT = 20;

    // all state confined to Swing EDT
    
    private final JTextField inputField = new JTextField();
    private final JButton postButton = new JButton("Post message");
    private final JButton topicButton = new JButton("New topic");
    private final Container topicPanel = new ScrollingPanel(TOPICS_SIZE, "Topic", TOPIC_HEIGHT);
    private final Container messagePanel = new ScrollingPanel(MESSAGES_SIZE, "Messages", MESSAGE_HEIGHT);

    private AbstractClient client;
    
    private int currentTopic = -1;
    
    public ChatWindow() {
        super("Chat client");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.add(topicPanel, JSplitPane.LEFT);
        splitPane.add(messagePanel, JSplitPane.RIGHT);
        splitPane.setBackground(Color.WHITE);
        add(splitPane);

        Box hbox = Box.createHorizontalBox();
        hbox.add(inputField);
        hbox.add(postButton);
        hbox.add(topicButton);
        postButton.addActionListener(e -> onPostClicked());
        topicButton.addActionListener(e -> onNewTopicClicked());
        
        add(hbox, BorderLayout.SOUTH);

        updateButtonVisibility();
        
        pack();
        setVisible(true);
    }

    // -----------------------------------------------------------------------

    public void login() {
        String username = JOptionPane.showInputDialog(this, "Enter username", "Login", JOptionPane.QUESTION_MESSAGE);

        if (username == null) {
            System.exit(0);
        }

        setTitle(getTitle() + " [" + username + "]");
        client = new Client(username.replace(' ', '_'));
        new Thread(client::handleIncoming).start();
    }

    // -----------------------------------------------------------------------

    private void updateButtonVisibility() {
        if (currentTopic >= 0) {
            getRootPane().setDefaultButton(postButton);
            postButton.setVisible(true);
        } else {
            getRootPane().setDefaultButton(topicButton);
            postButton.setVisible(false);
        }
        inputField.grabFocus();
    }

    // -----------------------------------------------------------------------

    private void onPostClicked() {
        String text = inputField.getText().trim();
        if (currentTopic >= 0 && text.length() > 0) {
            client.postMessage(text);
        }
        inputField.setText("");
    }

    // -----------------------------------------------------------------------

    private void onNewTopicClicked() {
        String text = inputField.getText().trim();
        if (text.length() > 0) {
            client.createTopic(text);
        }
        inputField.setText("");
    }

    // =======================================================================

    /**
     * A vertically scrolling list of items. Automatically scrolls to bottom
     * when an item is added.
     */
    private class ScrollingPanel extends JScrollPane {
        private final JPanel content = new JPanel();
        private final int itemHeight;
        private int max = -1;
        
        public ScrollingPanel(Dimension dims, String title, int itemHeight) {
            super.add(content);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(Color.WHITE);
            setPreferredSize(dims);
            setBorder(BorderFactory.createTitledBorder(title));
            setViewportView(content);
            this.itemHeight = itemHeight;
            JScrollBar verticalBar = getVerticalScrollBar();
            BoundedRangeModel model = verticalBar.getModel(); 
            model.addChangeListener(ev -> {
                int m = model.getMaximum();
                if (max != m) {
                    max = m;
                    verticalBar.setValue(max);
                }
            });
        }
        
        @Override
        public Component add(Component c) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, itemHeight));
            c.setPreferredSize(new Dimension(itemHeight, itemHeight));
            c.setMinimumSize(new Dimension(itemHeight, itemHeight));
            content.add(c);
            content.revalidate();
            return c;
        }
        
        @Override
        public void removeAll() {
            content.removeAll();
            content.revalidate();
            repaint();
        }
    }

    // =======================================================================

    private static final Color SELECTION_COLOR = new Color(224, 240, 255);

    private ButtonGroup buttonGroup = new ButtonGroup();
    
    private class TopicButton extends JRadioButton implements ActionListener {
        private final int topicId;
        private final String username;
        private final String text;
        private final Font font;

        TopicButton(int topicId, String username, String text) {
            this.topicId = topicId;
            this.username = username;
            this.text = text;
            
            int fontHeight = (TOPIC_HEIGHT / 4) * Toolkit.getDefaultToolkit().getScreenResolution() / 72;
            font = new Font(Font.SANS_SERIF, Font.PLAIN, fontHeight);

            addActionListener(this);
            buttonGroup.add(this);
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setColor(isSelected() ? SELECTION_COLOR : Color.WHITE);
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(Color.BLACK);
            g2d.setFont(font);
            g2d.setColor(isSelected() ? Color.BLACK : Color.DARK_GRAY);
            g2d.drawString("@" + username + ":", 2, (TOPIC_HEIGHT / 2) - 2);
            g2d.drawString(text, 2, TOPIC_HEIGHT - 4);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentTopic != topicId) {
                currentTopic = topicId;
                messagePanel.removeAll();
                client.selectTopic(topicId);
                updateButtonVisibility();
            }
        }
    }

    // =======================================================================
    
    private class Client extends AbstractClient {
        private final String username;

        protected Client(String username) {
            super(username);
            this.username = username;
        }

        @Override
        protected void onNewTopic(int topicId, int nbrMessages, String username, String text) {
            SwingUtilities.invokeLater(() -> {
                TopicButton button = new TopicButton(topicId, username, text);
                topicPanel.add(button);
                if (username.equals(this.username)) {
                    button.doClick();
                }
            });
        }

        @Override
        protected void onNewMessage(int topicId, int messageId, String username, String text) {
            SwingUtilities.invokeLater(() -> {
                JLabel f = new JLabel("@" + username + ": " + text);
                if (username.equals(this.username)) {
                    f.setOpaque(true);
                    f.setBackground(SELECTION_COLOR);
                }
                messagePanel.add(f);
            });
        }

        @Override 
        protected void onDisconnected(Throwable cause) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    JOptionPane.showMessageDialog(ChatWindow.this,
                            cause.getClass().getSimpleName() + ": " + cause.getMessage(), "Server disconnected",
                            JOptionPane.ERROR_MESSAGE);
                });
                super.onDisconnected(cause);
            } catch (Throwable t) {
                super.onDisconnected(t);
            }
        }
    }
}
