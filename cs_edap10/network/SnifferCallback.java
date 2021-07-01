package network;

import java.math.BigInteger;

public interface SnifferCallback {

    /**
     * Called whenever the sniffer intercepts an encrypted message on the network.
     * 
     * @param   m   sniffed message
     * @param   n   sender's public key
     */
    public void onMessageIntercepted(String m, BigInteger n);

}
