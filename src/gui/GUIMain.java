package gui;

import currency.Client;
import currency.Transaction;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

/**
 * Created by Sorin Nutu on 6/13/2015.
 */
public class GUIMain {
    public static void main(String[] args) throws InterruptedException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        Presenter p1 = new Presenter("localhost", 10000);
        Client client1 = p1.getClient();
        Presenter p2 = new Presenter("localhost", 10001);
        Client client2 = p2.getClient();

        Thread.sleep(5000);

        Transaction t1 = Transaction.Builder.getBuilder()
                .withClientBalance(client1.getBalance())
                .withPrivateKey(client1.getPrivateKey())
                .withPublicKey(client1.getPublicKey())
                .withUnspentTransactions(client1.getUnspentTransactions())
                .withRecipient(client2.getPublicKey(), 5)
                .build();

        client1.broadcastTransaction(t1);
    }
}
