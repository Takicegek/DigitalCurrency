package network;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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
        Assert.assertEquals("First node predecessor", 300, firstNodePredecessor);
        long firstNodeSuccessor = node1.getPredecessor().getKey();
        Assert.assertEquals("First node successor", 300, firstNodeSuccessor);
        long secondNodePredecessor = node2.getPredecessor().getKey();
        Assert.assertEquals("Second node predecessor", 500, secondNodePredecessor);
        long secondNodeSuccessor = node2.getPredecessor().getKey();
        Assert.assertEquals("Second node successor", 500, secondNodeSuccessor);

        List<String> receivedBroadcasts = node1.getReceivedBroadcastMessages();
        Assert.assertEquals("Received broadcasts", 0, receivedBroadcasts.size());

        receivedBroadcasts = node2.getReceivedBroadcastMessages();
        Assert.assertEquals("Received broadcasts", 1, receivedBroadcasts.size());

        Assert.assertEquals("Broadcast message", true, receivedBroadcasts.contains("Message from node 1."));
    }

    @Test
    public void testFourNodes() throws InterruptedException {
        MockedNode node1 = new MockedNode("localhost", 10000, 500, null);
        Thread.sleep(5000);

        MockedNode node2 = new MockedNode("localhost", 10001, 300, null);
        Thread.sleep(5000);

        MockedNode node3 = new MockedNode("localhost", 10002, 800, null);
        Thread.sleep(5000);

        MockedNode node4 = new MockedNode("localhost", 10003, 200, null);
        Thread.sleep(10000);

        long predecessor, successor, nextSuccessor;

        predecessor = node1.getPredecessor().getKey();
        successor = node1.getSuccessor().getKey();
        nextSuccessor = node1.getNextSuccessor().getKey();
        Assert.assertEquals("Predecessor", 300, predecessor);
        Assert.assertEquals("Successor", 800, successor);
        Assert.assertEquals("Next successor", 200, nextSuccessor);

        predecessor = node2.getPredecessor().getKey();
        successor = node2.getSuccessor().getKey();
        nextSuccessor = node2.getNextSuccessor().getKey();
        Assert.assertEquals("Predecessor", 200, predecessor);
        Assert.assertEquals("Successor", 500, successor);
        Assert.assertEquals("Next successor", 800, nextSuccessor);

        predecessor = node3.getPredecessor().getKey();
        successor = node3.getSuccessor().getKey();
        nextSuccessor = node3.getNextSuccessor().getKey();
        Assert.assertEquals("Predecessor", 500, predecessor);
        Assert.assertEquals("Successor", 200, successor);
        Assert.assertEquals("Next successor", 300, nextSuccessor);

        predecessor = node4.getPredecessor().getKey();
        successor = node4.getSuccessor().getKey();
        nextSuccessor = node4.getNextSuccessor().getKey();
        Assert.assertEquals("Predecessor", 800, predecessor);
        Assert.assertEquals("Successor", 300, successor);
        Assert.assertEquals("Next successor", 500, nextSuccessor);

    }
}
