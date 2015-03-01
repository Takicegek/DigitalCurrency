package network;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public class SocketListener implements Runnable {

    private Socket client;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private Node correspondingNode;
    private Dispatcher dispatcher;

    public SocketListener(Socket client, Node correspondingNode, Dispatcher dispatcher) {
        this.client = client;
        this.correspondingNode = correspondingNode;
        try {
            reader = new ObjectInputStream(client.getInputStream());
            writer = new ObjectOutputStream(client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        Object messageObject;
        try {
            while((messageObject = reader.readObject()) != null) {
                Message message = (Message) messageObject;

                if (message.getType() == MessageType.FIND_SUCCESSOR_JOIN) {
                    long id = (Long) message.getObject();

                    // if a node with that id already exists, send back a USED_ID message
                    if (id == correspondingNode.getId() || id == correspondingNode.getSuccessor().getKey()) {
                        Message answer = new Message(MessageType.SUCCESSOR_FOUND, null, message.getTag());
                        writer.writeObject(answer);
                        writer.flush();
                    } else {
                        handleFindSuccessor(message);
                    }
                }

                if (message.getType() == MessageType.FIND_SUCCESSOR_FIX_FINGER) {
                    long id = (Long) message.getObject();

                    // if the current node has the sought id, return the current node
                    // this does not mean that a new node with the same id joined but a node is fixing its finger table
                    if (id == correspondingNode.getId()) {
                        Message answer = new Message(MessageType.SUCCESSOR_FOUND, correspondingNode.getNodeInfo());
                        writer.writeObject(answer);
                        writer.flush();
                    } else {
                        handleFindSuccessor(message);
                    }
                }

                // respond with the predecessor of the current node
                if (message.getType() == MessageType.GET_PREDECESSOR) {
                    int tag = message.getTag();
                    if (correspondingNode.getPredecessor() != null) {
                        writer.writeObject(new Message(MessageType.SEND_PREDECESSOR, correspondingNode.getPredecessor().getNodeInfo(), tag));
                    } else {
                        writer.writeObject(new Message(MessageType.SEND_PREDECESSOR, null, tag));
                    }
                    writer.flush();
                }

                // message from predecessor
                if (message.getType() == MessageType.NOTIFY_SUCCESSOR) {
                    handleNotifySuccessor(message);
                }

                if (message.getType() == MessageType.PING) {
                    writer.writeObject(new Message(MessageType.PING, null, message.getTag()));
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.out.println(correspondingNode.getId() + ": Lost contact with a node that closed the socket.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleNotifySuccessor(Message message) throws IOException {
        NodeInfo nodeInfo = (NodeInfo) message.getObject();
        if (correspondingNode.getPredecessor() == null || !correspondingNode.getPredecessor().getNodeInfo().equals(nodeInfo)) {
            // the current node has a new predecessor so save it
            Socket socket = new Socket(nodeInfo.getIp(), nodeInfo.getPort());

            // close the current socket for the predecessor
            if (correspondingNode.getPredecessor() != null && correspondingNode.getPredecessor().getStreams() != null) {
                correspondingNode.getPredecessor().closeSocket();
            }

            correspondingNode.setPredecessor(new CompleteNodeInfo(nodeInfo, new Streams(socket)));

            System.out.println(correspondingNode.getId() + ": I have a new predecessor. It is " + nodeInfo.toString());
        }
    }

    private void handleFindSuccessor(Message message) throws IOException, ClassNotFoundException {
        // store the message tag to attach it back later
        int tag = message.getTag();

        Message answer = null;

        long id = (Long) message.getObject();

        if (belongsToInterval(id, correspondingNode.getId(), correspondingNode.getSuccessor().getKey())) {
            // the node is between this one and its successor, send the successor id
            answer = new Message(MessageType.SUCCESSOR_FOUND, correspondingNode.getSuccessor().getNodeInfo());
            System.out.println(id + " is between " + correspondingNode.getId() + " and my successor " + correspondingNode.getSuccessor().getKey());
            System.out.println("Its successor will be " + correspondingNode.getSuccessor().getNodeInfo());
        } else {
            System.out.println(id + " is NOT between " + correspondingNode.getId() + " and " + correspondingNode.getSuccessor().getKey());

            // find the closest preceding node
            int closestPreceding = closestPrecedingNode(id);
            System.out.println("Send the request further to " +
                    correspondingNode.getFingerTable().get(closestPreceding).getNodeInfo());

            // forward the message and retrieve it from the Future object
            // this is not a response, it is a message for a node to which this node may send messages
            // from other threads, so use the dispatcher
            Future<Message> messageFuture = null;
            try {
                messageFuture = dispatcher.sendMessage(message, closestPreceding);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            try {
                answer = messageFuture.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        // put back the tag
        answer.setTag(tag);

        writer.writeObject(answer);
        writer.flush();

    }

    private int closestPrecedingNode(long key) {
        /*for (int i = correspondingNode.getFingerTable().size() - 1; i >= 0; i--) {
            long fingerId = correspondingNode.getFingerTable().get(i).getKey();

            if (belongsToInterval(fingerId, correspondingNode.getId(), key)) {
                return correspondingNode.getFingerTable().get(i);
            }
        }*/
        return 0; // the successor
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
