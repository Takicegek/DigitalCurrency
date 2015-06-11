package network;

import currency.Block;
import currency.Client;
import currency.Transaction;
import currency.TransactionRecord;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by Sorin Nutu on 6/7/2015.
 */
public class TransactionTests {
    @Test
    public void testReceivedTransactions() throws InterruptedException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        MockedClient client1 = new MockedClient("localhost", 10000, 100);
        client1.connectToNetwork();
        MockedClient client2 = new MockedClient("localhost", 10001, 200);
        client2.connectToNetwork();
        MockedClient client3 = new MockedClient("localhost", 10002, 300);
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
        Thread.sleep(3000);

        TransactionRecord record = new TransactionRecord(client1.getPublicKey(), client2.getPublicKey(), 5);
        while (!client2.getUnspentTransactions().contains(record)) {
            Thread.sleep(1000);
        }
        System.out.println("Received transaction!");

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
        Thread.sleep(3000);

        assertEquals("Number of received transactions", 1, client1.getReceivedTransactions().size());
        assertEquals("Number of received transactions", 1, client2.getReceivedTransactions().size());
        assertEquals("Number of received transactions", 2, client3.getReceivedTransactions().size());
        assertEquals("Balance", 2, client1.getBalance(), 0);
        assertEquals("Balance", 0, client2.getBalance(), 0);
        assertEquals("Balance", 8, client3.getBalance(), 0);

       /* Set<Block> receivedBlocks = client1.getReceivedBlocks();
        System.out.println("Client 1 received " + receivedBlocks.size() + " blocks.");
        for (Block b : receivedBlocks) {
            System.out.println(b.getTransactions().size());
        }

        receivedBlocks = client2.getReceivedBlocks();
        System.out.println("Client 2 received " + receivedBlocks.size() + " blocks.");
        for (Block b : receivedBlocks) {
            System.out.println(b.getTransactions().size());
        }

        receivedBlocks = client3.getReceivedBlocks();
        System.out.println("Client 3 received " + receivedBlocks.size() + " blocks.");
        for (Block b : receivedBlocks) {
            System.out.println(b.getTransactions().size());
        }*/
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
