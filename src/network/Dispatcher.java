package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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

    public Dispatcher(Node correspondingNode) {
        this.correspondingNode = correspondingNode;
        nextTag = new AtomicInteger(0);
        futures = new ConcurrentHashMap<Integer, FutureMessage>();
    }

    public Future<Message> sendMessage(Message messageToSend, int fingerTableIndex) throws ExecutionException {
        CompleteNodeInfo neighbor = correspondingNode.getFingerTable().get(fingerTableIndex);
        ObjectOutputStream outputStream = neighbor.getOutputStream();
        ObjectInputStream inputStream = neighbor.getInputStream();

        return sendMessage(messageToSend, outputStream, inputStream);
    }

    public Future<Message> sendMessage(Message messageToSend, ObjectOutputStream outputStream,
                                       final ObjectInputStream inputStream) throws ExecutionException {
        int tag = nextTag.getAndIncrement();
        FutureMessage futureMessage = new FutureMessage();
        messageToSend.setTag(tag);

        futures.put(tag, futureMessage);

        try {
            outputStream.writeObject(messageToSend);
        } catch (IOException e) {
            System.err.println("The dispatcher cannot send the message: " + messageToSend.toString());
            futures.remove(tag);
            e.printStackTrace();

            throw new ExecutionException("The dispatcher cannot send the message!", e);
        }

        new Thread() {
            @Override
            public void run() {
                waitForMessage(inputStream);
            }
        }.start();

        return futureMessage;
    }

    public void sendMessageWithoutAnswer(Message messageToSend, int fingerTableIndex) {
        CompleteNodeInfo neighbor = correspondingNode.getFingerTable().get(fingerTableIndex);
        ObjectOutputStream outputStream = neighbor.getOutputStream();

        try {
            outputStream.writeObject(messageToSend);
        } catch (IOException e) {
            System.err.println("The dispatcher cannot send the message: " + messageToSend.toString());
            e.printStackTrace();
        }
    }

    public void waitForMessage(ObjectInputStream inputStream) {
        Message received = null;
        try {
            received = (Message) inputStream.readObject();

            int tag = received.getTag();
            FutureMessage futureMessage = futures.get(tag);
            futures.remove(tag);

            futureMessage.message = received;

            synchronized (futureMessage) {
                futureMessage.notify();
            }

        } catch (IOException e) {
            System.err.println("The dispatcher cannot read the message!");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
