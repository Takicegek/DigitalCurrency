package network;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sorin Nutu on 6/1/2015.
 */
public class ChordTestScenarios {
    @Test
    public void testTwoNodes() throws InterruptedException {
        MockedNode node1 = new MockedNode("localhost", 10000, 500, null);
        Thread.sleep(5000);

        MockedNode node2 = new MockedNode("localhost", 10001, 300, null);
        Thread.sleep(5000);

        node1.broadcastMessage("Message from node 1.");
        Thread.sleep(5000);

        long firstNodePredecessor = node1.getPredecessor().getKey();
        assertEquals("First node predecessor", 300, firstNodePredecessor);
        long firstNodeSuccessor = node1.getPredecessor().getKey();
        assertEquals("First node successor", 300, firstNodeSuccessor);
        long secondNodePredecessor = node2.getPredecessor().getKey();
        assertEquals("Second node predecessor", 500, secondNodePredecessor);
        long secondNodeSuccessor = node2.getPredecessor().getKey();
        assertEquals("Second node successor", 500, secondNodeSuccessor);

        List<String> receivedBroadcasts = node1.getReceivedBroadcastMessages();
        assertEquals("Received broadcasts", 0, receivedBroadcasts.size());

        receivedBroadcasts = node2.getReceivedBroadcastMessages();
        assertEquals("Received broadcasts", 1, receivedBroadcasts.size());

        assertTrue("Broadcast message", receivedBroadcasts.contains("Message from node 1."));
    }

    @Test
    public void testFourNodes() throws InterruptedException {
        MockedNode node1 = new MockedNode("localhost", 10000, 500, null);
        Thread.sleep(5000);

        MockedNode node2 = new MockedNode("localhost", 10001, 300, null);
        Thread.sleep(5000);

        MockedNode node3 = new MockedNode("localhost", 10002, 800, null);
        Thread.sleep(5000);

        node3.broadcastMessage("Message from node 3.");
        node2.broadcastMessage("Message from node 2.");

        MockedNode node4 = new MockedNode("localhost", 10003, 200, null);
        node4.broadcastMessage("Message from node 4.");
        Thread.sleep(10000);

        node2.broadcastMessage("Second message from node 2.");
        Thread.sleep(10000);

        long predecessor, successor, nextSuccessor;

        predecessor = node1.getPredecessor().getKey();
        successor = node1.getSuccessor().getKey();
        nextSuccessor = node1.getNextSuccessor().getKey();
        assertEquals("Predecessor", 300, predecessor);
        assertEquals("Successor", 800, successor);
        assertEquals("Next successor", 200, nextSuccessor);

        predecessor = node2.getPredecessor().getKey();
        successor = node2.getSuccessor().getKey();
        nextSuccessor = node2.getNextSuccessor().getKey();
        assertEquals("Predecessor", 200, predecessor);
        assertEquals("Successor", 500, successor);
        assertEquals("Next successor", 800, nextSuccessor);

        predecessor = node3.getPredecessor().getKey();
        successor = node3.getSuccessor().getKey();
        nextSuccessor = node3.getNextSuccessor().getKey();
        assertEquals("Predecessor", 500, predecessor);
        assertEquals("Successor", 200, successor);
        assertEquals("Next successor", 300, nextSuccessor);

        predecessor = node4.getPredecessor().getKey();
        successor = node4.getSuccessor().getKey();
        nextSuccessor = node4.getNextSuccessor().getKey();
        assertEquals("Predecessor", 800, predecessor);
        assertEquals("Successor", 300, successor);
        assertEquals("Next successor", 500, nextSuccessor);

        // test broadcast messages
        List<String> receivedByNode1 = node1.getReceivedBroadcastMessages();
        List<String> receivedByNode2 = node2.getReceivedBroadcastMessages();
        List<String> receivedByNode3 = node3.getReceivedBroadcastMessages();
        List<String> receivedByNode4 = node4.getReceivedBroadcastMessages();

        assertEquals("Messages received by node 1", 4, receivedByNode1.size());
        assertTrue("Broadcast message", receivedByNode1.contains("Message from node 2."));
        assertTrue("Broadcast message", receivedByNode1.contains("Second message from node 2."));
        assertTrue("Broadcast message", receivedByNode1.contains("Message from node 3."));
        assertTrue("Broadcast message", receivedByNode1.contains("Message from node 4."));

        assertEquals("Messages received by node 2", 2, receivedByNode2.size());
        assertTrue("Broadcast message", receivedByNode2.contains("Message from node 3."));
        assertTrue("Broadcast message", receivedByNode2.contains("Message from node 4."));

        assertEquals("Messages received by node 3", 3, receivedByNode3.size());
        assertTrue("Broadcast message", receivedByNode3.contains("Message from node 2."));
        assertTrue("Broadcast message", receivedByNode3.contains("Second message from node 2."));
        assertTrue("Broadcast message", receivedByNode3.contains("Message from node 4."));

        assertEquals("Messages received by node 4", 1, receivedByNode4.size());
        assertTrue("Broadcast message", receivedByNode4.contains("Second message from node 2."));
    }
}
