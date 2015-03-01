package network;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Sorin Nutu on 2/26/2015.
 */
public class StabilizeThread extends Thread {

    private Node correspondingNode;
    private long id;
    private int port;
    private Dispatcher dispatcher;

    public StabilizeThread(Node correspondingNode, Dispatcher dispatcher) {
        this.correspondingNode = correspondingNode;
        this.id = correspondingNode.getId();
        this.port = correspondingNode.getPort();
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try {
            stabilize();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * The current node asks the successor about its predecessor.
     * It verifies if n's immediate successor is consistent, and tells the successor about n.
     *
     * The described task is run periodically.
     */
    private void stabilize() throws IOException, ClassNotFoundException, InterruptedException {

        while (true) {
            if (correspondingNode.getSuccessor().getKey() == id && correspondingNode.getSuccessor().getPort() == port) {
                System.out.println("Stabilize first node . . ");
                stabilizeFirstNodeInRing();
            } else {
                System.out.println("Stabilize node (ask successor about predecessor and notify predecessor) ");
                // ask the successor for its predecessor
                // reuse the streams associated with the successor
                boolean ioException = false;
                ObjectOutputStream outputStream = null;

                /*
                    If an IOException occurs, it means that the successor left.
                    A FIND_SUCCESSOR_JOIN message is sent to a bootstrap node and a new successor is found.
                    However, it may also leave and another exception would be thrown.
                    Solution: loop until the message is actually sent.
                 */
                Future<Message> messageFuture = null;

                Message message = new Message(MessageType.GET_PREDECESSOR, null);
                try {
                    messageFuture = dispatcher.sendMessage(message, 0);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                // get the answer
                // exit the current iteration of the while loop if an exception is caught
                Message received = null;
                try {
                    received = messageFuture.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                // we know that the object inside the message is a NodeInfo
                NodeInfo receivedNode = (NodeInfo) received.getObject();
                System.out.println("Asked my successor " + correspondingNode.getSuccessor().getNodeInfo() + " about its predecessor and the answer is: " + receivedNode);

                // if my successor has a predecessor different by, there are two cases
                // 1. I just joined and my successor does not know about me
                // 2. Another node joined between me and my successor
                // Should determine here if I change the successor.
                if (receivedNode != null && receivedNode.getKey() != id &&
                        SocketListener.belongsToInterval(receivedNode.getKey(), id, correspondingNode.getSuccessor().getKey())) {

                    // the predecessor received from my successor is in front of me, so it becomes my successor
                    Socket socket = new Socket(receivedNode.getIp(), receivedNode.getPort());
                    Streams streams = new Streams(socket);

                    // close the old socket
                    correspondingNode.getSuccessor().closeSocket();

                    CompleteNodeInfo successor = new CompleteNodeInfo(receivedNode, streams);
                    correspondingNode.setSuccessor(successor);

                    correspondingNode.getFingerTable().remove(0);
                    correspondingNode.getFingerTable().add(0, successor);

                    System.out.println(id + ": I have a new successor! It is " + receivedNode.toString());
                }

                // notify the successor about its predecessor, which is the current node
                Message notifyMessage = new Message(MessageType.NOTIFY_SUCCESSOR, new NodeInfo(correspondingNode.getIp(), port, id));
                dispatcher.sendMessageWithoutAnswer(notifyMessage, 0);
            }

            Thread.sleep(5000);
        }
    }


    /**
     * The stabilize() method sends a message through a Socket to the successor. When the network has only one node,
     * it cannot send messages to itself. This method implements the same behaviour as stabilize(), but it does not
     * send any messages. The node's successor is the node itself.
     */
    private void stabilizeFirstNodeInRing() {
        if (correspondingNode.getPredecessor() == null) {
            correspondingNode.setPredecessor(new CompleteNodeInfo(new NodeInfo(correspondingNode.getIp(), port, id), null));
            System.out.println("Stabilized the first node in ring.");
        } else {
            // if another node joined and it has this node as its successor, it notifies and the current
            // node changes its predecessor; check if the predecessor is changed.
            if (correspondingNode.getPredecessor().getKey() != id && correspondingNode.getPredecessor().getPort() != port) {
                // another node joined and it is between the current node and its successor
                // set that node as a successor
                correspondingNode.getSuccessor().setNodeInfo(correspondingNode.getPredecessor().getNodeInfo());
                Socket socket = null;
                try {
                    socket = new Socket(correspondingNode.getSuccessor().getIp(), correspondingNode.getSuccessor().getPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                correspondingNode.getSuccessor().setStreams(new Streams(socket));
            }
        }
    }
}