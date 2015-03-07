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
    private Map<NodeInfo, CommunicationThread> threads;

    public Dispatcher(Node correspondingNode) {
        this.correspondingNode = correspondingNode;
        nextTag = new AtomicInteger(0);
        futures = new ConcurrentHashMap<Integer, FutureMessage>();
        this.threads = new ConcurrentHashMap<NodeInfo, CommunicationThread>();
    }

    public synchronized Future<Message> sendMessage(Message messageToSend, boolean waitForAnswer, int fingerTableIndex) {
        return sendMessage(messageToSend, waitForAnswer, correspondingNode.getFingerTable().get(fingerTableIndex));
    }

    public synchronized Future<Message> sendMessage(Message messageToSend, boolean waitForAnswer, CompleteNodeInfo destination) {
        int tag = nextTag.getAndIncrement();
        FutureMessage futureMessage = new FutureMessage();
        messageToSend.setTag(tag);

        futures.put(tag, futureMessage);

        if (!threads.containsKey(destination.getNodeInfo())) {
            CommunicationThread thread = new CommunicationThread(destination.getStreams(), this);
            thread.start();
            threads.put(destination.getNodeInfo(), thread);
        }

        CommunicationThread thread = threads.get(destination.getNodeInfo());

        System.err.println((new Date()).toString() + " " + "Trimit mesajul " + messageToSend + " catre " + destination.getKey() + " cu waitanswer = " + waitForAnswer);
        thread.sendMessage(new MessageWrapper(messageToSend, waitForAnswer));

        return futureMessage;
    }

    public void receiveMessage(Message received) {
        int tag = received.getTag();
        FutureMessage futureMessage = futures.get(tag);
        futures.remove(tag);

        futureMessage.message = received;

        System.err.println((new Date()).toString() + " " + "Am primit mesajul " + received);
        try {
            Thread.sleep(10);
            int i = 0;
            Thread.sleep(30);
            i++;
            Thread.sleep(400);
            Thread.sleep(30);
            int j = i;
            Thread.sleep(50);
            i = j;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (futureMessage) {
            futureMessage.notify();
        }
        System.err.println((new Date()).toString() + " " +"Am livrat mesajul " + received);
    }
}
