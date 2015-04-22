package network;

import currency.BlockchainAndTransactionsWrapper;
import currency.Transaction;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Sorin Nutu on 3/8/2015.
 */
public class SocketListenerMessageHandlingThread implements Runnable {

    private Message message;
    private Node correspondingNode;
    private ObjectOutputStream writer;
    private Dispatcher dispatcher;

    public SocketListenerMessageHandlingThread(Message message, Node correspondingNode,
                                               ObjectOutputStream writer, Dispatcher dispatcher) {
        this.message = message;
        this.correspondingNode = correspondingNode;
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
                    writeAnswer(answer);
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
                    writeAnswer(answer);
                } else {
                    if (id == correspondingNode.getSuccessor().getKey()) {
                        Message answer = new Message(MessageType.SUCCESSOR_FOUND, correspondingNode.getSuccessor(), tag);
                        writeAnswer(answer);
                    } else {
                        handleFindSuccessor(message);
                    }
                }
            }

            // respond with the predecessor of the current node
            if (message.getType() == MessageType.GET_PREDECESSOR) {
                int tag = message.getTag();
                if (correspondingNode.getPredecessor() != null) {
                    writeAnswer(new Message(MessageType.SEND_PREDECESSOR, correspondingNode.getPredecessor(), tag));
                } else {
                    writeAnswer(new Message(MessageType.SEND_PREDECESSOR, null, tag));
                }
                System.out.println("Raspund cu predecesorul meu: " + correspondingNode.getPredecessor());

            }

            // message from predecessor
            if (message.getType() == MessageType.NOTIFY_SUCCESSOR) {
                handleNotifySuccessor(message);
            }

            if (message.getType() == MessageType.GET_SUCCESSOR) {
                int tag = message.getTag();
                writeAnswer(new Message(MessageType.SEND_SUCCESSOR, correspondingNode.getSuccessor(), tag));
            }

            if (message.getType() == MessageType.CHECK_PREDECESSOR) {
                // this is a ping from my successor to see if I am alive, send the message back
                writeAnswer(message);
            }

            if (message.getType() == MessageType.BROADCAST_MESSAGE) {
                BroadcastMessageWrapper wrapper = (BroadcastMessageWrapper) message.getObject();
                correspondingNode.handleReceivedMessage(wrapper);

                // send the wrapper further with the same type it was received
                handleBroadcastMessage(wrapper, MessageType.BROADCAST_MESSAGE);
            }

            if (message.getType() == MessageType.BROADCAST_TRANSACTION) {
                BroadcastMessageWrapper wrapper = (BroadcastMessageWrapper) message.getObject();
                correspondingNode.handleReceivedTransaction((Transaction) wrapper.getMessage());

                // send the wrapper further with the same type it was received
                handleBroadcastMessage(wrapper, MessageType.BROADCAST_TRANSACTION);
            }

            if (message.getType() == MessageType.GET_BLOCKCHAIN) {
                // add the wrapper to the existing message, the tag should be the same
                BlockchainAndTransactionsWrapper wrapper = correspondingNode.getBlockchainAndTransactions();
                message.setObject(wrapper);
                writeAnswer(message);
            }

        } catch (IOException e) {
            System.out.println((new Date()).toString() + " " + correspondingNode.getId() + ": Lost contact with a node that closed the socket.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * todo add description
     *
     * The same method is called for all broadcasts (messages, transactions, blocks). The type parameter is needed
     * in order to send the message further with the same type. Only the interval limits will be changed.
     * @param wrapper
     */
    private void handleBroadcastMessage(BroadcastMessageWrapper wrapper, MessageType type) {
        long start = wrapper.getStart();
        long end = wrapper.getEnd();

        System.err.println("**Am primit un mesaj broadcast cu start = " + start + " si end = " + end);

        long successorKey = correspondingNode.getFingerTable().get(0).getKey();

        // send the message further only if the successor is in the given interval
        if (SocketListener.belongsToClosedInterval(successorKey, start, end)) {
            int finger = getFingerForBroadcast(start, end);

            // if the closest finger to the middle is the same with successor, find another one
            if (correspondingNode.getFingerTable().get(finger).getKey() == correspondingNode.getFingerTable().get(0).getKey()) {
                finger = getLastFingerInInterval(start, end);

                // if the last finger in interval points to the same node as the successor, send the message only once
                if (successorKey == correspondingNode.getFingerTable().get(finger).getKey()) {
                    wrapper.setStart(correspondingNode.getFingerTable().get(0).getKey());
                    wrapper.setEnd(end);

                    Message first = new Message(type, wrapper);
                    System.err.println("**Il trimit doar spre succesor cu start = " + correspondingNode.getFingerTable().get(0).getKey() + " si end = " + end);
                    dispatcher.sendMessage(first, false, 0);
                } else {
                    // send a message to successor and one to the newly found finger
                    long fingerKey = correspondingNode.getFingerTable().get(finger).getKey();

                    System.err.println("****Il trimit spre succesor cu start = " + correspondingNode.getFingerTable().get(0).getKey() + " si end = " + (fingerKey-1));
                    System.err.println("****Il trimit spre un finger cu start = " + fingerKey + " si end = " + end);

                    wrapper.setStart(correspondingNode.getFingerTable().get(0).getKey());
                    wrapper.setEnd(fingerKey - 1);

                    Message first = new Message(type, wrapper);
                    dispatcher.sendMessage(first, false, 0);

                    BroadcastMessageWrapper secondWrapper = new BroadcastMessageWrapper(fingerKey, end, wrapper.getMessage());
                    Message second = new Message(type, secondWrapper);
                    dispatcher.sendMessage(second, false, finger);
                }
            } else {
                // send the message to the successor and to the finger
                long fingerKey = correspondingNode.getFingerTable().get(finger).getKey();

                System.err.println("**Il trimit spre succesor cu start = " + correspondingNode.getFingerTable().get(0).getKey() + " si end = " + (fingerKey-1));
                System.err.println("**Il trimit spre un finger cu start = " + fingerKey + " si end = " + end);


                wrapper.setStart(correspondingNode.getFingerTable().get(0).getKey());
                wrapper.setEnd(fingerKey - 1);

                Message first = new Message(type, wrapper);
                dispatcher.sendMessage(first, false, 0);

                BroadcastMessageWrapper secondWrapper = new BroadcastMessageWrapper(fingerKey, end, wrapper.getMessage());
                Message second = new Message(type, secondWrapper);
                dispatcher.sendMessage(second, false, finger);
            }
        }
    }

    /**
     * Computes the finger to which the broadcast message will be sent - the one in the middle.
     * The finger should be inside the [start, end] interval.
     *
     * @param start
     * @param end
     * @return the finger id or -1 if there is no finger in the interval
     */
    private int getFingerForBroadcast(long start, long end) {
        long min = Long.MAX_VALUE;
        int finger = -1;

        long distance = (end - start + Node.NUMBER_OF_NODES) % Node.NUMBER_OF_NODES;
        long middle = (start + distance / 2) % Node.NUMBER_OF_NODES;

        for (int i = 0; i < Node.LOG_NODES; i++) {
            NodeInfo node = correspondingNode.getFingerTable().get(i);
            // check if the finger is populated!
            if (node.getKey() != -1 && SocketListener.belongsToClosedInterval(node.getKey(), start, end)) {
                if (Math.abs(node.getKey() - middle) < min) {
                    min = Math.abs(node.getKey() - middle);
                    finger = i;
                }
            }
        }

        if (finger == -1) {
            System.out.println("Finger = -1 !!!!!!!!!!!!!!!!! start = " + start + ", end = " + end);
        }

        return finger;
    }

    private int getLastFingerInInterval(long start, long end) {
        int last = 0;
        for (int i = 0; i < Node.LOG_NODES; i++) {
            NodeInfo node = correspondingNode.getFingerTable().get(i);
            if (node.getKey() != -1 && SocketListener.belongsToIntervalForBroadcast(node.getKey(), start, end)) {
                last = i;
            }
        }
        return last;
    }

    private void handleNotifySuccessor(Message message) throws IOException {
        NodeInfo nodeInfo = (NodeInfo) message.getObject();
        System.out.println((new Date()).toString() + " " + "My predecessor is " + nodeInfo.toString());
        if (correspondingNode.getPredecessor() == null || !correspondingNode.getPredecessor().equals(nodeInfo)) {
            correspondingNode.setPredecessor(nodeInfo);

            System.out.println((new Date()).toString() + " " +correspondingNode.getId() + ": I have a new predecessor. It is " + nodeInfo.toString());
        }
    }

    private void handleFindSuccessor(Message message) throws IOException, ClassNotFoundException {
        // store the message tag to attach it back later
        int tag = message.getTag();
        Message answer = null;
        long id = (Long) message.getObject();

        if (SocketListener.belongsToOpenInterval(id, correspondingNode.getId(), correspondingNode.getSuccessor().getKey())) {
            // the node is between this one and its successor, send the successor id
            answer = new Message(MessageType.SUCCESSOR_FOUND, correspondingNode.getSuccessor());
            System.out.println((new Date()).toString() + " " +id + " is between " + correspondingNode.getId() + " and my successor " + correspondingNode.getSuccessor().getKey());
            System.out.println((new Date()).toString() + " " + "Its successor will be " + correspondingNode.getSuccessor());
        } else {
            System.out.println((new Date()).toString() + " " +id + " is NOT between " + correspondingNode.getId() + " and " + correspondingNode.getSuccessor().getKey());

            // find the closest preceding node
            int closestPreceding = closestPrecedingNode(id);
            System.out.println((new Date()).toString() + " " +"Send the request further to " +
                    correspondingNode.getFingerTable().get(closestPreceding));

            // forward the message and retrieve it from the Future object
            // this is not a response, it is a message for a node to which this node may send messages
            // from other threads, so use the dispatcher
            Future<Message> messageFuture = dispatcher.sendMessage(message, true, closestPreceding);
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

        writeAnswer(answer);
    }

    private int closestPrecedingNode(long key) {
        /*for (int i = correspondingNode.getFingerTable().size() - 1; i >= 0; i--) {
            long fingerId = correspondingNode.getFingerTable().get(i).getKey();

            if (belongsToOpenInterval(fingerId, correspondingNode.getId(), key)) {
                return correspondingNode.getFingerTable().get(i);
            }
        }*/
        return 0; // the successor
    }

    private void writeAnswer(Message message) {
        synchronized (writer) {
            try {
                writer.writeUnshared(message);
            } catch (IOException e) {
                System.err.println("Could not send the answer back!");
                e.printStackTrace();
            }
        }
    }
}
