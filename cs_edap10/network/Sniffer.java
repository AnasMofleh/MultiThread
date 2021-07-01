package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rsa.impl.PublicKey;
import rsa.impl.RSA;

/**
 * Sniffs packets on the network to find announced public keys and messages
 * encrypted with that key.
 */
public class Sniffer extends Thread {

    private static final long MESSAGE_PERIOD = 5000;  // simulate an interception every 5s
    private static final int KEY_SIZE = 56;

    private List<String> quotes = new ArrayList<>();
    
    private final SnifferCallback callback;

    public Sniffer(SnifferCallback callback) {
        super("sniffer");

        this.callback = callback;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/quotes.txt")))) {
            reader.lines().forEach(quotes::add);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Override
    public void run() {
        try {
            Random rand = new Random();

            while (true) {
                String message = quotes.get(rand.nextInt(quotes.size()));
                RSA rsa = new RSA(KEY_SIZE);
                PublicKey publicKey = rsa.publicKey();

                String encrypted = RSA.encrypt(message, publicKey);

                callback.onMessageIntercepted(encrypted, publicKey.getN());

                Thread.sleep(MESSAGE_PERIOD);
            }
        } catch (InterruptedException unexpected) {
            throw new Error(unexpected);
        }
    }   
}
