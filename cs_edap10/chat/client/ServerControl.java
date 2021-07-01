package chat.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;

public abstract class ServerControl {

    // this must match the corresponding value in client_handler.c
    private static final String PACKET_RESTART = "R";

    // We need to give the server some time to re-open after
    // it's been restarted. This is not ideal, but the best we
    // can do unless we figure out how to somehow wait for the
    // server to come back online.
    private static final long SERVER_RESTART_TIME = 200;

    public static void restartServer() throws IOException, InterruptedException {
        Socket sock = new Socket(InetAddress.getLoopbackAddress(), AbstractClient.PORT);

        InputStream in = sock.getInputStream();
        new PrintStream(sock.getOutputStream()).println(PACKET_RESTART);
        int b = in.read();
        if (b >= 0) {
            throw new Error("unexpected response to PACKET_RESTART");
        }
        Thread.sleep(SERVER_RESTART_TIME);
    }
}
