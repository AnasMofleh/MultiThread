package rsa.impl;

import java.math.BigInteger;

public class PublicKey {

    private final BigInteger n;
    private final BigInteger key;

    public PublicKey(BigInteger n, BigInteger key) {
        this.n = n;
        this.key = key;
    }

    public BigInteger getN() {
        return n;
    }

    public BigInteger getKey() {
        return key;
    }

}
