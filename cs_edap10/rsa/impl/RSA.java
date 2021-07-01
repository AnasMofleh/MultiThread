package rsa.impl;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RSA {

    private final static SecureRandom random = new SecureRandom();

    /**
     * As encoding, each character of a message is converted to its Unicode value.
     * Pad with zeroes so that each character has this many digits.
     * 
     * Note: 3 is enough for the normal Unicode characters, but the quotation
     * marks � and � have Unicode values 8220 and 8221, so pad to 4.
     */
    public static final int ENCODE_PAD_LENGTH = 4;

    private BigInteger privateKey;
    private BigInteger publicKey;
    private BigInteger n;
    private BigInteger phi;
    private BigInteger p;
    private BigInteger q;
    private int chunkSize;

    public RSA(BigInteger p, BigInteger q) {

        this.p = p;
        this.q = q;

        init();
    }

    /**
     * Creates an RSA instance, and generates a key of the specified bit length
     * (roughly). Increasing key length will make breaking the encryption more
     * difficult to break.
     * 
     * A key size of 50-60 might be appropriate for testing (depending on
     * hardware!), and should take less than a minute to break.
     */
    public RSA(int bits) {

        // generate big prime numbers (bits)
        this.p = BigInteger.probablePrime(bits / 2, random);
        this.q = BigInteger.probablePrime(bits / 2, random);

        init();
    }

    private void init() {

        // phi = (p - 1)*(q - 1)
        phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        // n = p*q
        n = p.multiply(q);

        if (n.bitLength() < 16) {
            throw new IllegalArgumentException("Key is too short!");
        }
        chunkSize = n.bitLength() / 8;

        publicKey = new BigInteger("65537"); // e = common prime = 2^16 + 1
        privateKey = publicKey.modInverse(phi); // d = (publicKey^-1) mod(phi)
    }

    public PublicKey publicKey() {
        return new PublicKey(n, publicKey);
    }

    public String encrypt(String message) {

        StringBuilder encrypted = new StringBuilder();
        String encoded = encode(message);
        int step = chunkSize - 1; // Reduce by 1 to compensate for extra "1"

        for (int i = 0; i < encoded.length(); i += step) {
            int endIndex = Math.min(i + step, encoded.length());
            String numStr = "1" + encoded.substring(i, endIndex);
            BigInteger num = new BigInteger(numStr);
            num = num.modPow(publicKey, n);
            encrypted.append(num.toString()).append(".");
        }

        return encrypted.toString();
    }

    public String decrypt(String encrypted) {

        StringBuilder encoded = new StringBuilder();
        String[] chunks = encrypted.split("\\.");

        for (String chunk : chunks) {
            BigInteger num = new BigInteger(chunk);
            num = num.modPow(privateKey, n);
            String part = num.toString();
            if (part.charAt(0) != '1') {
                throw new IllegalArgumentException(
                    "Message was not properly encrypted, cannot decrypt it.");
            }
            part = part.substring(1);
            encoded.append(part);
        }
        return decode(encoded.toString());
    }

    private static String encode(String message) {

        StringBuffer numberString = new StringBuffer();

        for (int i = 0; i < message.length(); ++i) {
            char c = message.charAt(i);
            int asc = (int) c;

            int len = String.valueOf(asc).length();
            while (len++ < ENCODE_PAD_LENGTH) {
                numberString.append("0");
            }

            numberString.append(asc);
        }

        return numberString.toString();
    }

    private String decode(String encoded) {

        StringBuffer message = new StringBuffer();

        for (int i = 0; i < encoded.length(); i += ENCODE_PAD_LENGTH) {
            String blockString = encoded.substring(i, i + ENCODE_PAD_LENGTH);
            int block = Integer.parseInt(blockString);
            message.append((char) block);
        }

        return message.toString();
    }

    public static String encrypt(String message, PublicKey publicKey) {

        StringBuilder encrypted = new StringBuilder();
        String encoded = encode(message);
        int chunkSize = publicKey.getN().bitLength() / 8;
        int step = chunkSize - 1; // Reduce by 1 to compensate for extra "1"

        for (int i = 0; i < encoded.length(); i += step) {
            int endIndex = Math.min(i + step, encoded.length());
            String numStr = "1" + encoded.substring(i, endIndex);
            BigInteger num = new BigInteger(numStr);
            num = num.modPow(publicKey.getKey(), publicKey.getN());
            encrypted.append(num.toString()).append(".");
        }

        return encrypted.toString();

    }

    public static void main(String[] args) {

        int bits = 60; // Integer.parseInt(args[0]);

        RSA rsa = new RSA(bits);

        String message = "Congratulations, you got it right!";

        System.out.println("-");

        System.out.println("p: " + rsa.p);
        System.out.println("q: " + rsa.q);
        System.out.println("n = p*q: " + rsa.n);
        System.out.println("phi = (p - 1)*(q - 1): " + rsa.phi);
        System.out.println(
            "Public key: " + rsa.publicKey + " (most common prime 2^16 + 1)");
        System.out.println("Private key: " + rsa.privateKey);

        System.out.println("-");

        System.out.println("Original message: " + message);

        String encryptedMessage = rsa.encrypt(message);
        String decryptedMessage = rsa.decrypt(encryptedMessage);

        System.out.println("Encrypted message: " + encryptedMessage);
        System.out.println("Decrypted message: " + decryptedMessage);

    }
}
