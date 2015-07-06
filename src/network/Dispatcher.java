package network;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * The dispatcher helps with multiplexing a communication channel between multiple threads.
 *
 * When a thread sends a message, it will return a Future object.
 * Internally, the dispatcher assigns a tag to the message and maps the tag to the returned future.
 * When the answer to a message is received, its tag is retrieved and the message is assigned
 * to the corresponding future in the right thread.
 *
 * Created by Sorin Nutu on 2/27/2015.
 */
public class Dispatcher {
    private AtomicInteger nextTag;
    private Map<Integer, FutureMessage> futures;
    private Node correspondingNode;
    private Logger networkLogger;
    private Map<NodeInfo, MessageSenderThread> senderThreads;
    /*
        Store the messages that wait for an answer. When the connection with a NodeInfo is closed,
        remove all the messages that are waiting from the futures map, set their state to RETRY and
        release them.
     */
    private Map<NodeInfo, Collection<Integer>> messagesWaitingForAnswer;

    public Dispatcher(Node correspondingNode) {
        this.correspondingNode = correspondingNode;
        this.networkLogger = correspondingNode.getNetworkLogger();
        nextTag = new AtomicInteger(0);
        futures = new ConcurrentHashMap<Integer, FutureMessage>();
        senderThreads = new ConcurrentHashMap<NodeInfo, MessageSenderThread>();
        messagesWaitingForAnswer = new HashMap<NodeInfo, Collection<Integer>>();
    }

    public synchronized Future<Message> sendMessage(Message messageToSend, boolean waitForAnswer, int fingerTableIndex) {
        return sendMessage(messageToSend, waitForAnswer, correspondingNode.getFingerTable().get(fingerTableIndex));
    }

    public synchronized Future<Message> sendMessage(Message messageToSend, boolean waitForAnswer, NodeInfo destination) {
        int tag;
        FutureMessage futureMessage = null;

        // do not create a tag and a future if an answer is not required
        if (MessageType.waitForAnswer(messageToSend.getType())) {
            tag = nextTag.getAndIncrement();
            futureMessage = new FutureMessage();
            messageToSend.setTag(tag);
            futures.put(tag, futureMessage);

            if (messagesWaitingForAnswer.containsKey(destination)) {
                messagesWaitingForAnswer.get(destination).add(tag);
            } else {
                Collection<Integer> tags = new ArrayList<Integer>();
                tags.add(tag);
                messagesWaitingForAnswer.put(destination, tags);
            }
        }

        if (!senderThreads.containsKey(destination)) {
            Streams streams = null;
            try {
                Socket socket = new Socket(destination.getIp(), destination.getPort());
                streams = new Streams(socket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // the socket could not be opened; tell the node to retry
                handleMessageFailure(messageToSend, destination);
                // do not start threads or try to send messages if the socket could not be opened
                return futureMessage;
            }

            MessageSenderThread senderThread = new MessageSenderThread(streams, this, destination);
            senderThread.start();

            MessageReceiverThread receiverThread = new MessageReceiverThread(streams.getObjectInputStream(), this, destination);
            receiverThread.start();

            senderThreads.put(destination, senderThread);
        }

        MessageSenderThread senderThread = senderThreads.get(destination);

        if (networkLogger != null) {
            networkLogger.info((new Date()).toString() + " " + "Trimit mesajul " + messageToSend + " catre " + destination.getKey() + " cu waitanswer = " + waitForAnswer);
        } else {
            networkLogger = correspondingNode.getNetworkLogger();
        }
        senderThread.sendMessage(messageToSend);

        return futureMessage;
    }

    public synchronized void receiveMessage(Message received, NodeInfo source) {
        int tag = received.getTag();
        FutureMessage futureMessage = futures.get(tag);
        futures.remove(tag);

        if (networkLogger != null) {
            networkLogger.info((new Date()).toString() + " " + "Am primit mesajul " + received);
        } else {
            networkLogger = correspondingNode.getNetworkLogger();
        }

        // this also releases the semaphore and permits the message to be read
        futureMessage.setMessage(received);
        removeMessageFromWaitingMap(received, source);
    }

    /**
     * Method called when a message cannot be sent.
     * @param message the message that was not delivered
     */
    public synchronized void handleMessageFailure(Message message, NodeInfo destination) {
        int tag = message.getTag();
        FutureMessage futureMessage = futures.get(tag);
        futures.remove(tag);

        System.err.println((new Date()).toString() + " " + "The message could not be sent: " + message);

        if (futureMessage != null) {
            message.setType(MessageType.RETRY);
            futureMessage.setMessage(message);

            System.out.println("Dispatcher: Schimb tipul in RETRY pe mesajul cu tagul " + tag + " pentru ca nu s-a putut trimite.");

            removeMessageFromWaitingMap(message, destination);
        }

        // remove the destination threads; it is finished.
        senderThreads.remove(destination);
    }

    private void removeMessageFromWaitingMap(Message message, NodeInfo nodeInfo) {
        int tag = message.getTag();
        Iterator<Integer> iterator = messagesWaitingForAnswer.get(nodeInfo).iterator();
        while (iterator.hasNext()) {
            int messageTag = iterator.next();
            if (messageTag == tag) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Method called from MessageReceiverThread when the other node closes the socket.
     * Change the state of all the waiting messages to RETRY and release their futures.
     */
    public synchronized void handleConnectionError(NodeInfo nodeInfo) {
        Message message = new Message(MessageType.RETRY, null);
        Iterator<Integer> iterator = messagesWaitingForAnswer.get(nodeInfo).iterator();
        while (iterator.hasNext()) {
            int tag = iterator.next();
            FutureMessage futureMessage = futures.get(tag);
            futures.remove(tag);

            futureMessage.setMessage(message);
            iterator.remove();

            System.out.println("Dispatcher: Trimit RETRY pe mesajul cu tagul " + tag);
        }
        messagesWaitingForAnswer.remove(nodeInfo);
    }
}
