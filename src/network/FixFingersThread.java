package network;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This class represents a thread that refreshed the finger table for a node.
 * Every 5 seconds a new index is checked, by sending the FIND_SUCCESSOR_JOIN message with the id:
 *   currentId + 2 ^ (index - 1), index = 1 to LOG_NODES - 1
 *
 * The message is sent to the successor of this node.
 * If the answer different from the node in the finger table, the finger table is updated.
 *
 * Created by Sorin Nutu on 2/20/2015.
 */
public class FixFingersThread extends Thread {
    private long currentNodeId;
    private int next;
    private Node correspondingNode;
    private Dispatcher dispatcher;

    public FixFingersThread(Dispatcher dispatcher, long currentNodeId, Node correspondingNode) {
        this.dispatcher = dispatcher;
        this.currentNodeId = currentNodeId;
        this.correspondingNode = correspondingNode;
        next = 0;
    }

    @Override
    public void run() {
        while (true) {
            next = next + 1;
            if (next >= Node.LOG_NODES) {
                next = 1;
            }
            System.out.println(currentNodeId + ": Fixing finger: " + next);
            System.out.println(currentNodeId + ": Current value of finger: " + correspondingNode.getFingerTable().get(next));

            try {
                // prepare the streams to send the message to the successor
                // suppose the successor is always available if the network has more than 2 nodes
                if (!correspondingNode.getFingerTable().get(0).equals(correspondingNode.getNodeInfo())) {
                    // fingerId = currentNodeId + 2 ^ (next - 1)
                    long fingerId = (currentNodeId + (1 << next)) % Node.NUMBER_OF_NODES;

                    Message message = new Message(MessageType.FIND_SUCCESSOR_FIX_FINGER, fingerId);
                    Future<Message> messageFuture = dispatcher.sendMessage(message, true, 0);
                    Message received = messageFuture.get();

                    /*
                    If the dispatcher cannot send the message, the received message will have the RETRY type.
                    If so, do not try to change the finger. Wait for the StabilizeThread to fix the
                    successor and then the message will be successfully sent.
                    The finger will be fixed in the next iteration.
                     */

                    if (received.getType() != MessageType.RETRY) {
                        NodeInfo nodeInfo = (NodeInfo) received.getObject();

                        System.out.println("Finger " + next + " = " + fingerId + "  == " + nodeInfo.getKey());

                        // check if the found finger is already present in the table
                        if (!correspondingNode.getFingerTable().get(next).equals(nodeInfo)) {

                            System.out.println(correspondingNode.getFingerTable().get(next).toString() + " nu e egal cu " + nodeInfo.toString());

                            // replace the finger
                            correspondingNode.getFingerTable().remove(next);
                            correspondingNode.getFingerTable().add(next, nodeInfo);

                            System.out.println("Finger " + next + " == " + nodeInfo);
                        }

                        System.out.println(currentNodeId + ": Fixed the fingertable on position + " + next + ". It points to " + nodeInfo.getKey() + ".");
                    }
                }
            } catch (InterruptedException e) {
                // MessageFuture.get()
                e.printStackTrace();
            } catch (ExecutionException e) {
                // MessageFuture.get()
                e.printStackTrace();
            } finally {
                // sleep even though an exception was caught
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
