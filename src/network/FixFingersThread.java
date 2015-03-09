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
            System.out.println(currentNodeId + ": Current value of finger: " + correspondingNode.getFingerTable().get(next).getNodeInfo());

            try {
                // prepare the streams to send the message to the successor
                // suppose the successor is always available if the network has more than 2 nodes
                if (correspondingNode.getFingerTable().get(0).getStreams() != null) {
                    // fingerId = currentNodeId + 2 ^ (next - 1)
                    long fingerId = (currentNodeId + (1 << next)) % Node.NUMBER_OF_NODES;

                    Message message = new Message(MessageType.FIND_SUCCESSOR_FIX_FINGER, fingerId);
                    Future<Message> messageFuture = dispatcher.sendMessage(message, true, 0);
                    Message received = messageFuture.get();

                    NodeInfo nodeInfo = (NodeInfo) received.getObject();

                    System.out.println("Finger " + next + " = " + fingerId + "  == " + nodeInfo.getKey());

                    // check if the found finger is already present in the table
                    if (!correspondingNode.getFingerTable().get(next).getNodeInfo().equals(nodeInfo)) {

                        System.out.println(correspondingNode.getFingerTable().get(next).getNodeInfo().toString() + " nu e egal cu " + nodeInfo.toString());

                        // replace the finger
                        Socket socket = new Socket(nodeInfo.getIp(), nodeInfo.getPort());
                        Streams streams = new Streams(socket);

                        // avoid remove and add in ArrayList
                        correspondingNode.getFingerTable().get(next).setNodeInfo(nodeInfo);
                        correspondingNode.getFingerTable().get(next).setStreams(streams);

                        System.out.println("Connexion created with " + nodeInfo.getKey() + ". Finger = " + correspondingNode.getFingerTable().get(next).getNodeInfo().toString());
                    }

                    System.out.println(currentNodeId + ": Fixed the fingertable on position + " + next + ". It points to " + nodeInfo.getKey() + ".");
                }
            } catch (InterruptedException e) {
                // MessageFuture.get()
                e.printStackTrace();
            } catch (ExecutionException e) {
                // MessageFuture.get()
                e.printStackTrace();
            } catch (UnknownHostException e) {
                // new Socket()
                e.printStackTrace();
            } catch (IOException e) {
                // new Socket()
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
