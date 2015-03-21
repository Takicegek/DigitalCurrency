package network;
import utils.NodeGUI;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
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
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * The current node asks the successor about its predecessor.
     * It verifies if n's immediate successor is consistent, and tells the successor about n.
     *
     * The described task is run periodically.
     */
    private void stabilize() throws ClassNotFoundException, InterruptedException, ExecutionException {

        while (true) {
            if (correspondingNode.getSuccessor().getKey() == id && correspondingNode.getSuccessor().getPort() == port) {
                System.out.println((new Date()).toString() + " " +"Stabilize first node . . ");
                stabilizeFirstNodeInRing();
            } else {
                System.out.println((new Date()).toString() + " " + "Stabilize node (ask successor about predecessor and notify predecessor) ");
                // ask the successor for its predecessor
                Message message = new Message(MessageType.GET_PREDECESSOR, null);
                Future<Message> messageFuture = dispatcher.sendMessage(message, true, 0);

                /*
                If the dispatcher cannot send or receive the message, the Message received by the node has
                the type MessageType.RETRY.
                Change the successor and try again until the message is successfully transmitted.
                 */

                boolean success;
                Message received;
                do {
                    success = true;
                    System.out.println("StabilizeThread: Waiting for message future . . ");
                    received = messageFuture.get();
                    System.out.println("StabilizeThread: Message read - " + received);
                    if (received.getType() == MessageType.RETRY) {
                        success = false;
                        // change the successor
                        synchronized (correspondingNode.getFingerTable()) {
                            correspondingNode.setSuccessor(correspondingNode.getNextSuccessor());
                            correspondingNode.getFingerTable().remove(0);
                            correspondingNode.getFingerTable().add(0, correspondingNode.getNextSuccessor());
                        }
                        message = new Message(MessageType.GET_PREDECESSOR, null);
                        messageFuture = dispatcher.sendMessage(message, true, 0);
                        System.out.println("Trimit un nou mesaj la " + correspondingNode.getFingerTable().get(0));
                    }
                    System.out.println("Stabilize Thread: Message type = " + received.getType());
                } while (!success);

                // we know that the object inside the message is a NodeInfo
                NodeInfo receivedNode = (NodeInfo) received.getObject();
                System.out.println((new Date()).toString() + " " +"Asked my successor " + correspondingNode.getSuccessor() + " about its predecessor and the answer is: " + receivedNode);

                // if my successor has a predecessor different by me, there are two cases
                // 1. I just joined and my successor does not know about me
                // 2. Another node joined between me and my successor
                // Should determine here if I change the successor.
                if (receivedNode != null && receivedNode.getKey() != id &&
                        SocketListener.belongsToInterval(receivedNode.getKey(), id, correspondingNode.getSuccessor().getKey())) {

                    synchronized (correspondingNode.getFingerTable()) {
                        // the predecessor received from my successor is in front of me, so it becomes my successor
                        correspondingNode.setSuccessor(receivedNode);

                        // update the finger table
                        correspondingNode.getFingerTable().remove(0);
                        correspondingNode.getFingerTable().add(0, receivedNode);
                    }
                    System.out.println((new Date()).toString() + " " + id + ": I have a new successor! It is " + receivedNode.toString());
                }
                // notify the successor about its predecessor, which is the current node
                Message notifyMessage = new Message(MessageType.NOTIFY_SUCCESSOR, new NodeInfo(correspondingNode.getIp(), port, id));
                dispatcher.sendMessage(notifyMessage, false, 0);
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
            correspondingNode.setPredecessor(new NodeInfo(correspondingNode.getIp(), port, id));
            System.out.println((new Date()).toString() + " " +"Stabilized the first node in ring.");
        } else {
            // if another node joined and it has this node as its successor, it notifies and the current
            // node changes its predecessor; check if the predecessor is changed.
            if (correspondingNode.getPredecessor().getKey() != id && correspondingNode.getPredecessor().getPort() != port) {
                // another node joined and it is between the current node and its successor
                // set that node as a successor
                correspondingNode.setSuccessor(correspondingNode.getPredecessor());
                correspondingNode.getFingerTable().remove(0);
                correspondingNode.getFingerTable().add(0, correspondingNode.getSuccessor());
            }
        }
    }
}
