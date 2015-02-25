package network;

import utils.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * This class represents a thread that refreshed the finger table for a node.
 * Every 5 seconds a new index is checked, by sending the FIND_SUCCESSOR_JOIN message with the id:
 *   currentId + 2 ^ (index - 1)
 *
 * The message is sent to the successor of this node.
 * If the answer different from the node in the finger table, the finger table is updated.
 *
 * Created by Sorin Nutu on 2/20/2015.
 */
public class FixFingersThread extends Thread {
    private List<Pair<NodeInfo, Streams>> fingerTable;
    private long currentNodeId;
    private long next;
    private Node correspondingNode;

    public FixFingersThread(List<Pair<NodeInfo, Streams>> fingerTable, long currentNodeId, Node correspondingNode) {
        this.fingerTable = fingerTable;
        this.currentNodeId = currentNodeId;
        this.correspondingNode = correspondingNode;
        next = -1;
    }

    @Override
    public void run() {
        while (true) {
            next = next + 1;
            if (next >= Node.LOG_NODES) {
                next = 0;
            }
            System.out.println(currentNodeId + ": Fixing finger: " + next);

            try {
                // prepare the streams to send the message to the successor
                // suppose the successor is always available if the network has more than 2 nodes
                if (fingerTable.get(0).getSecond() != null) {

                    ObjectOutputStream outputStream = fingerTable.get(0).getSecond().getObjectOutputStream();
                    ObjectInputStream inputStream = fingerTable.get(0).getSecond().getObjectInputStream();

                    // fingerId = currentNodeId + 2 ^ (next - 1)
                    long fingerId = (currentNodeId + (1 << (next))) % (Node.NUMBER_OF_NODES);
                    System.out.println();
//                    System.err.println(currentNodeId + ": The id of finger " + next + " is: " + fingerId);
                    Message message = new Message(MessageType.PING, fingerId);
                    outputStream.writeObject(message); // todo what if another successor was found and the socket was closed

                    Object received = null;

                    synchronized (correspondingNode) {
                        received = inputStream.readObject(); // todo exception java.io.StreamCorruptedException: invalid type code: 00
                    }
                    if (received instanceof Message) {
                        Message m = (Message) received;
                        correspondingNode.insertMessage(m);
                    }

                    Message t = correspondingNode.getMessage(MessageType.PING);
//                    NodeInfo nodeInfo = (NodeInfo) received.getObject();

                    // if the finger is not the current one, change it in the finger table
                    // check if the finger table size is smaller than the next value - get or remove throw IndexOutOfBounds
//                    if (fingerTable.size() <= next || !fingerTable.get((int) next).getFirst().equals(nodeInfo)) {
//
//                        fingerTable.remove(nodeInfo);
//                        Socket socket = new Socket(nodeInfo.getIp(), nodeInfo.getPort());
//                        Streams streams = new Streams(socket);
//
//                        fingerTable.add((int) next, new Pair<NodeInfo, Streams>(nodeInfo, streams));
//                    }
//                    System.out.println(currentNodeId + ": Fixed the fingertable on position + " + next + ". It points to " + nodeInfo.getKey() + ".");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
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
