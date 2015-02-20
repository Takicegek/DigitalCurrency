package network;

import utils.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * This class represents a thread that refreshed the finger table for a node.
 * Every 5 seconds a new index is checked, by sending the FIND_SUCCESSOR message with the id:
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

    public FixFingersThread(List<Pair<NodeInfo, Streams>> fingerTable, long currentNodeId) {
        this.fingerTable = fingerTable;
        this.currentNodeId = currentNodeId;
        next = -1;
    }

    @Override
    public void run() {
        while (true) {
            next = next + 1;
            if (next >= fingerTable.size()) {
                next = 0;
            }
            try {
                // prepare the streams to send the message to the successor
                // suppose the successor is always available if the network has more than 2 nodes
                if (fingerTable.get(0).getSecond() != null) {

                    ObjectOutputStream outputStream = fingerTable.get(0).getSecond().getObjectOutputStream();
                    ObjectInputStream inputStream = fingerTable.get(0).getSecond().getObjectInputStream();

                    // fingerId = currentNodeId + 2 ^ (next - 1)
                    long fingerId = (currentNodeId + (1 << (next - 1))) % (Node.NUMBER_OF_NODES);
                    Message message = new Message(MessageType.FIND_SUCCESSOR, fingerId);

                    outputStream.writeObject(message);
                    Message received = (Message) inputStream.readObject();

                    NodeInfo nodeInfo = (NodeInfo) received.getObject();
                    // if the finger is not the current one, change it in the finger table
                    if (!fingerTable.get((int) next).equals(nodeInfo)) {
                        fingerTable.remove(next);
                        Socket socket = new Socket(nodeInfo.getIp(), nodeInfo.getPort());
                        Streams streams = new Streams(socket);

                        fingerTable.add((int) next, new Pair<NodeInfo, Streams>(nodeInfo, streams));
                    }
                }
                Thread.sleep(5000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
