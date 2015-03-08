package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Sorin Nutu on 3/8/2015.
 */
public class SocketListenerMessageHandlingThread extends Thread {

    private Message message;
    private Node correspondingNode;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private Dispatcher dispatcher;

    public SocketListenerMessageHandlingThread(Message message, Node correspondingNode, ObjectInputStream reader,
                                               ObjectOutputStream writer, Dispatcher dispatcher) {
        this.message = message;
        this.correspondingNode = correspondingNode;
        this.reader = reader;
        this.writer = writer;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try {
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
                int tag = message.getTag();

                // if the current node has the sought id, return the current node
                // this does not mean that a new node with the same id joined but a node is fixing its finger table
                if (id == correspondingNode.getId()) {
                    Message answer = new Message(MessageType.SUCCESSOR_FOUND, correspondingNode.getNodeInfo(), tag);
                    writer.writeObject(answer);
                } else {
                    if (id == correspondingNode.getSuccessor().getKey()) {
                        Message answer = new Message(MessageType.SUCCESSOR_FOUND, correspondingNode.getSuccessor().getNodeInfo(), tag);
                        writer.writeObject(answer);
                    } else {
                        handleFindSuccessor(message);
                    }
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
        } catch (IOException e) {
            System.out.println((new Date()).toString() + " " + correspondingNode.getId() + ": Lost contact with a node that closed the socket.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleNotifySuccessor(Message message) throws IOException {
        NodeInfo nodeInfo = (NodeInfo) message.getObject();
        System.out.println((new Date()).toString() + " " + "My predecessor is " + nodeInfo.toString());
        if (correspondingNode.getPredecessor() == null || !correspondingNode.getPredecessor().getNodeInfo().equals(nodeInfo)) {
            // the current node has a new predecessor so save it
//            Socket socket = new Socket(nodeInfo.getIp(), nodeInfo.getPort());

            // bugfix: do not close the socket, other messages may still come and the predecessor must be informed
            // close the current socket for the predecessor
//            if (correspondingNode.getPredecessor() != null && correspondingNode.getPredecessor().getStreams() != null) {
//                correspondingNode.getPredecessor().closeSocket();
//            }
            correspondingNode.setPredecessor(new CompleteNodeInfo(nodeInfo, null));

            System.out.println((new Date()).toString() + " " +correspondingNode.getId() + ": I have a new predecessor. It is " + nodeInfo.toString());
        }
    }

    private void handleFindSuccessor(Message message) throws IOException, ClassNotFoundException {
        // store the message tag to attach it back later
        int tag = message.getTag();
        Message answer = null;
        long id = (Long) message.getObject();

        if (SocketListener.belongsToInterval(id, correspondingNode.getId(), correspondingNode.getSuccessor().getKey())) {
            // the node is between this one and its successor, send the successor id
            answer = new Message(MessageType.SUCCESSOR_FOUND, correspondingNode.getSuccessor().getNodeInfo());
            System.out.println((new Date()).toString() + " " +id + " is between " + correspondingNode.getId() + " and my successor " + correspondingNode.getSuccessor().getKey());
            System.out.println((new Date()).toString() + " " + "Its successor will be " + correspondingNode.getSuccessor().getNodeInfo());
        } else {
            System.out.println((new Date()).toString() + " " +id + " is NOT between " + correspondingNode.getId() + " and " + correspondingNode.getSuccessor().getKey());

            // find the closest preceding node
            int closestPreceding = closestPrecedingNode(id);
            System.out.println((new Date()).toString() + " " +"Send the request further to " +
                    correspondingNode.getFingerTable().get(closestPreceding).getNodeInfo());

            // forward the message and retrieve it from the Future object
            // this is not a response, it is a message for a node to which this node may send messages
            // from other threads, so use the dispatcher
            Future<Message> messageFuture = null;
            messageFuture = dispatcher.sendMessage(message, true, closestPreceding);
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
}
