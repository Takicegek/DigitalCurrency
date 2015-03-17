package network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Sorin Nutu on 3/1/2015.
 */
public class MessageSenderThread extends Thread {
    private BlockingQueue<Message> queue;
    private ObjectOutputStream outputStream;
    private Dispatcher dispatcher;
    private NodeInfo destination;

    public MessageSenderThread(Streams streams, Dispatcher dispatcher, NodeInfo destination) {
        this.queue = new ArrayBlockingQueue<Message>(Node.LOG_NODES);
        this.outputStream = streams.getObjectOutputStream();
        this.dispatcher = dispatcher;
        this.destination = destination;
    }

    @Override
    public void run() {
        while (true) {
            Message toSend = null;
            try {
                toSend = queue.take();
                // fixed bug: writeUnshared instead of writeObject
                outputStream.writeUnshared(toSend);
                outputStream.flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("MessageSenderthread: Could not send the message. Notifying the dispatcher " +
                        "and closing the thread.");
                dispatcher.handleMessageFailure(toSend, destination);
                break;
            }
        }
    }

    public void sendMessage(Message message) {
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
