package network;

import utils.Pair;

import java.io.*;
import java.net.Socket;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public class SocketListener implements Runnable {

    private Socket client;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private Node correspondingNode;

    public SocketListener(Socket client, Node correspondingNode) {
        this.client = client;
        this.correspondingNode = correspondingNode;
        try {
            reader = new ObjectInputStream(client.getInputStream());
            writer = new ObjectOutputStream(client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Object messageObject;
        try {
            while((messageObject = reader.readObject()) != null) {
                Message message = (Message) messageObject;


                if (message.getType() == MessageType.FIND_SUCCESSOR) {
                    handleFindSuccessor(message);
                }

                // respond with the predecessor of the current node
                if (message.getType() == MessageType.GET_PREDECESSOR) {
                    writer.writeObject(new Message(MessageType.GET_PREDECESSOR, correspondingNode.getPredecessor()));
                }

                // message from predecessor
                if (message.getType() == MessageType.NOTIFY_SUCCESSOR) {
                    NodeInfo nodeInfo = (NodeInfo) message.getObject();
                    if (correspondingNode.getPredecessor() == null || !correspondingNode.getPredecessor().equals(nodeInfo)) {
                        // the current node has a new predecessor so save it
                        Socket socket = new Socket(nodeInfo.getIp(), nodeInfo.getPort());
                        correspondingNode.setPredecessor(new Pair<NodeInfo, Streams>(nodeInfo, new Streams(socket)));

                        System.out.println("I have a new predecessor. It is " + nodeInfo.toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleFindSuccessor(Message message) throws IOException, ClassNotFoundException {
        Message answer = null;
        if (message.getObject() instanceof Long) {
            long id = (Long) message.getObject();
            if (belongsToInterval(id, correspondingNode.getId(), correspondingNode.getSuccessor().getKey())) {
                // the node is between this one and its successor, send the successor id
                answer = new Message(MessageType.FIND_SUCCESSOR, correspondingNode.getSuccessor());
                System.out.println(id + " is between " + correspondingNode.getId() + " and my successor " + correspondingNode.getSuccessor().getKey());
                System.out.println("Its successor will be " + correspondingNode.getSuccessor());
            } else {
                System.out.println(id + " is NOT between " + correspondingNode.getId() + " and " + correspondingNode.getSuccessor().getKey());

                // find the closest preceding node
                Pair<NodeInfo, Streams> closestPreceding = closestPrecedingNode(id);
                System.out.println("Send the request further to " + closestPreceding.getFirst());

                // forward the message
                closestPreceding.getSecond().getObjectOutputStream().writeObject(message);
                answer = (Message) closestPreceding.getSecond().getObjectInputStream().readObject();
            }

            if (answer == null) {
                throw new RuntimeException("The answer I have to return is null.");
            } else {
                System.out.println(id + " asked me about its successor. It is " + answer.getObject());
            }
            writer.writeObject(answer);
        } else {
            throw new RuntimeException("Find Successor message does not contain a long.");
        }
    }

    private Pair<NodeInfo, Streams> closestPrecedingNode(long key) {
        for (int i = correspondingNode.getFingerTable().size() - 1; i >= 0; i--) {
            long fingerId = correspondingNode.getFingerTable().get(i).getFirst().getKey();

            if (belongsToInterval(fingerId, correspondingNode.getId(), key)) {
                return correspondingNode.getFingerTable().get(i);
            }
        }
        return correspondingNode.getFingerTable().get(0);
    }

    protected static boolean belongsToInterval(long id, long nodeId, long successor) {
        if (nodeId < successor && nodeId < id && id < successor) {
            return true;
        }
        if (nodeId > successor && (id > nodeId || id < successor)) {
            return true;
        }
        // this is the single node in the network
        if (nodeId == successor) {
            return true;
        }
        return false;
    }
}
