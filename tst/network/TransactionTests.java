package network;

import currency.Transaction;
import currency.TransactionRecord;
import org.junit.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import static org.junit.Assert.*;

/**
 * Created by Sorin Nutu on 6/7/2015.
 */
public class TransactionTests {
    @Test
    public void testReceivedTransactions() throws InterruptedException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        MockedClient client1 = new MockedClient("localhost", 10000, "localhost", 10000, 100);
        client1.connectToNetwork();
        MockedClient client2 = new MockedClient("localhost", 10003, "localhost", 10000, 200);
        client2.connectToNetwork();
        MockedClient client3 = new MockedClient("localhost", 10004, "localhost", 10000, 300);
        client3.connectToNetwork();

        waitForStabilization(client1, client2, client3);

        assertEquals(client1.getNodeSuccessor(), client2.getNodeId());
        assertEquals(client2.getNodeSuccessor(), client3.getNodeId());
        assertEquals(client3.getNodeSuccessor(), client1.getNodeId());

        // transaction from client 1 to client 2 and client 3
        Transaction t1 = Transaction.Builder.getBuilder()
                .withClientBalance(client1.getBalance())
                .withPrivateKey(client1.getPrivateKey())
                .withPublicKey(client1.getPublicKey())
                .withUnspentTransactions(client1.getUnspentTransactions())
                .withRecipient(client2.getPublicKey(), 5)
                .withRecipient(client3.getPublicKey(), 5)
                .build();

        client1.broadcastTransaction(t1);

        Thread.sleep(5000);

        assertEquals("Balance", 0, client1.getBalance(), 0);
        assertEquals("Balance", 5, client2.getBalance(), 0);
        assertEquals("Balance", 5, client3.getBalance(), 0);

        // transaction from client 2 to client 1 and client 3
        Transaction t2 = Transaction.Builder.getBuilder()
                .withClientBalance(client2.getBalance())
                .withPrivateKey(client2.getPrivateKey())
                .withPublicKey(client2.getPublicKey())
                .withUnspentTransactions(client2.getUnspentTransactions())
                .withRecipient(client1.getPublicKey(), 2)
                .withRecipient(client3.getPublicKey(), 3)
                .build();

        client2.broadcastTransaction(t2);
        Thread.sleep(10000);

        assertEquals("Balance", 2, client1.getBalance(), 0);
        assertEquals("Balance", 0, client2.getBalance(), 0);
        assertEquals("Balance", 8, client3.getBalance(), 0);

        // transaction from client 2 to client 1 and client 3
        Transaction t3 = Transaction.Builder.getBuilder()
                .withClientBalance(client3.getBalance())
                .withPrivateKey(client3.getPrivateKey())
                .withPublicKey(client3.getPublicKey())
                .withUnspentTransactions(client3.getUnspentTransactions())
                .withRecipient(client1.getPublicKey(), 3)
                .withRecipient(client2.getPublicKey(), 5)
                .build();

        client3.broadcastTransaction(t3);
        Thread.sleep(10000);

        assertEquals("Balance", 5, client1.getBalance(), 0);
        assertEquals("Balance", 5, client2.getBalance(), 0);
        assertEquals("Balance", 0, client3.getBalance(), 0);
    }

    private void waitForStabilization(MockedClient... clients) throws InterruptedException {
        while (!checkLinks(clients)) {
            Thread.sleep(2000);
        }
    }

    private boolean checkLinks(MockedClient... clients) {
        boolean connected = true;
        for (int i = 1; i < clients.length; i++) {
            if (clients[i].getNodeId() != clients[i-1].getNodeSuccessor()) {
                connected = false;
                break;
            }
        }
        if (clients[clients.length-1].getNodeSuccessor() != clients[0].getNodeId()) {
            connected = false;
        }
        return connected;
    }
}
