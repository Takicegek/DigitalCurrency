package network;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
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

    public Dispatcher(Node correspondingNode) {
        this.correspondingNode = correspondingNode;
        nextTag = new AtomicInteger(0);
        futures = new ConcurrentHashMap<Integer, FutureMessage>();
        this.senderThreads = new ConcurrentHashMap<NodeInfo, MessageSenderThread>();
    }

    public synchronized Future<Message> sendMessage(Message messageToSend, boolean waitForAnswer, int fingerTableIndex) {
        return sendMessage(messageToSend, waitForAnswer, correspondingNode.getFingerTable().get(fingerTableIndex));
    }

    public synchronized Future<Message> sendMessage(Message messageToSend, boolean waitForAnswer, NodeInfo destination) {
        int tag = nextTag.getAndIncrement();
        FutureMessage futureMessage = new FutureMessage();
        messageToSend.setTag(tag);

        futures.put(tag, futureMessage);

        if (!senderThreads.containsKey(destination)) {
            try {
                Socket socket = new Socket(destination.getIp(), destination.getPort());
                Streams streams = new Streams(socket);

                MessageSenderThread senderThread = new MessageSenderThread(streams, this);
                senderThread.start();

                MessageReceiverThread receiverThread = new MessageReceiverThread(streams.getObjectInputStream(), this);
                receiverThread.start();

                senderThreads.put(destination, senderThread);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        MessageSenderThread senderThread = senderThreads.get(destination);

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
