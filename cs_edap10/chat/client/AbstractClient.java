package chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** Superclass for all chat clients, holding common functionality. */
public abstract class AbstractClient {
    private Socket socket;
    private PrintStream out;
    private BufferedReader in;

    // these must match the corresponding values in client_handler.c
    private static final String PACKET_LOGIN            = "L";
    private static final String PACKET_CREATE_TOPIC     = "C";
    private static final String PACKET_SELECT_TOPIC     = "S";
    private static final String PACKET_POST_MESSAGE     = "P";
    private static final String PACKET_LOGOUT           = "O";

    private static final String PACKET_PUBLISH_TOPIC    = "T";
    private static final String PACKET_PUBLISH_MESSAGE  = "M";
    private static final String PACKET_LOGGED_OUT       = "O";

    /* package */ static final int PORT = 9000;
    
    private Thread handlerThread = new Thread(this::handleIncoming);

    private boolean loggedOut = false;
    
    protected AbstractClient(String username) {
        try {
            socket = new Socket(InetAddress.getLoopbackAddress(), PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintStream(socket.getOutputStream(), true, StandardCharsets.UTF_8.name());
            out.println(PACKET_LOGIN + username);
        } catch (IOException e) {
            onDisconnected(e);
        }
    }

    public void start() {
        handlerThread.start();
    }


    public void join() throws InterruptedException {
        handlerThread.join();
    }

    public synchronized void postMessage(String text) {
        out.println(PACKET_POST_MESSAGE + text);
    }

    public synchronized void createTopic(String text) {
        out.println(PACKET_CREATE_TOPIC + text);
    }

    public synchronized void selectTopic(int topicId) {
        out.println(PACKET_SELECT_TOPIC + topicId);
    }

    public synchronized void logOut() throws InterruptedException {
        out.println(PACKET_LOGOUT);
        
        while (! loggedOut) {
            wait();
        }
    }

    public void handleIncoming() {
        try {
            while (true) {
                String s = in.readLine();
                if (s == null) {
                    throw new IOException("server disconnected");
                }
                Scanner scan = new Scanner(s);
                String type = scan.next();
                int topicId;
                String user, text;

                switch (type) {

                case PACKET_PUBLISH_TOPIC:
                    topicId = scan.nextInt();
                    int nbrMessages = scan.nextInt();
                    user = scan.next();
                    text = scan.nextLine();
                    onNewTopic(topicId, nbrMessages, user, text);
                    break;

                case PACKET_PUBLISH_MESSAGE:                    
                    topicId = scan.nextInt();
                    int messageId = scan.nextInt();
                    user = scan.next();
                    text = scan.nextLine();
                    onNewMessage(topicId, messageId, user, text);
                    break;
                case PACKET_LOGGED_OUT:
                    onLoggedOut();
                    socket.close();
                    return;

                default:
                    throw new Error("unexpected message type: " + type);
                }
            }
        } catch (Throwable t) {
            if (!loggedOut) {
                onDisconnected(t);
            }
        }
    }

    protected abstract void onNewTopic(int topicId, int nbrMessages, String username, String text);
    
    protected abstract void onNewMessage(int topicId, int messageId, String username, String text);
    
    protected void onDisconnected(Throwable cause) {
        cause.printStackTrace();
        System.exit(1);
    }

    protected synchronized void onLoggedOut() {
        loggedOut = true;
        notifyAll();
    }
}
