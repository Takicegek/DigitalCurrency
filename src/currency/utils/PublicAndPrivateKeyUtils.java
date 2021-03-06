package currency.utils;

import sun.security.rsa.RSAPrivateKeyImpl;
import sun.security.rsa.RSAPublicKeyImpl;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Created by Sorin Nutu on 4/24/2015.
 */
public class PublicAndPrivateKeyUtils {
    public static String getAddress(PublicKey publicKey) {
        if (publicKey instanceof RSAPublicKeyImpl) {
            RSAPublicKeyImpl rsaPublicKey = (RSAPublicKeyImpl) publicKey;
            return rsaPublicKey.getModulus() + "#" + rsaPublicKey.getPublicExponent();
        } else {
            throw new RuntimeException("The public key is not a RSAPublicKeyImpl.");
        }
    }

    public static String getAddress(PrivateKey privateKey) {
        if (privateKey instanceof RSAPrivateKeyImpl) {
            RSAPrivateKeyImpl rsaPrivateKey = (RSAPrivateKeyImpl) privateKey;
            return rsaPrivateKey.getModulus() + "#" + rsaPrivateKey.getPrivateExponent();
        } else {
            throw new RuntimeException("The private key is not a RSAPrivateKeyImpl.");
        }
    }

    public static PublicKey getPublicKey(String address) {
        String[] parts = address.split("#");
        try {
            return new RSAPublicKeyImpl(new BigInteger(parts[0]), new BigInteger(parts[1]));
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Cannot create the public key from address: " + address, e);
        }
    }
}
