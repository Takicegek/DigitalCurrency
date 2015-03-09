package network;

import utils.MessageWrapper;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The dispatcher helps with multiplexing a communication channel between multiple threads.
 *
 * todo update this
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
    private Map<NodeInfo, MessageSenderThread> senderThreads;
    private Map<NodeInfo, MessageReceiverThread> receiverThreads;

    public Dispatcher(Node correspondingNode) {
        this.correspondingNode = correspondingNode;
        nextTag = new AtomicInteger(0);
        futures = new ConcurrentHashMap<Integer, FutureMessage>();
        this.senderThreads = new ConcurrentHashMap<NodeInfo, MessageSenderThread>();
        this.receiverThreads = new ConcurrentHashMap<NodeInfo, MessageReceiverThread>();
    }

    public synchronized Future<Message> sendMessage(Message messageToSend, boolean waitForAnswer, int fingerTableIndex) {
        return sendMessage(messageToSend, waitForAnswer, correspondingNode.getFingerTable().get(fingerTableIndex));
    }

    public synchronized Future<Message> sendMessage(Message messageToSend, boolean waitForAnswer, CompleteNodeInfo destination) {
        int tag = nextTag.getAndIncrement();
        FutureMessage futureMessage = new FutureMessage();
        messageToSend.setTag(tag);

        futures.put(tag, futureMessage);

        if (!senderThreads.containsKey(destination.getNodeInfo())) {
            MessageSenderThread thread = new MessageSenderThread(destination.getStreams(), this);
            thread.start();
            senderThreads.put(destination.getNodeInfo(), thread);
        }

        if (!receiverThreads.containsKey(destination.getNodeInfo())) {
            MessageReceiverThread thread = new MessageReceiverThread(destination.getInputStream(), this);
            thread.start();
            receiverThreads.put(destination.getNodeInfo(), thread);
        }

        MessageSenderThread senderThread = senderThreads.get(destination.getNodeInfo());

        System.err.println((new Date()).toString() + " " + "Trimit mesajul " + messageToSend + " catre " + destination.getKey() + " cu waitanswer = " + waitForAnswer);
        senderThread.sendMessage(messageToSend);

        return futureMessage;
    }

    public synchronized void receiveMessage(Message received) {
        int tag = received.getTag();
        FutureMessage futureMessage = futures.get(tag);
        futures.remove(tag);

        System.err.println((new Date()).toString() + " " + "Am primit mesajul " + received);

        // this also releases the semaphore and permits the message to be read
        futureMessage.setMessage(received);
    }
}
