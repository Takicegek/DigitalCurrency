package network;

import utils.MessageWrapper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Sorin Nutu on 3/1/2015.
 */
public class MessageSenderThread extends Thread {
    private BlockingQueue<Message> queue;
    private ObjectOutputStream outputStream;
    private Dispatcher dispatcher;

    public MessageSenderThread(Streams streams, Dispatcher dispatcher) {
        this.queue = new ArrayBlockingQueue<Message>(Node.LOG_NODES);
        this.outputStream = streams.getObjectOutputStream();
        this.dispatcher = dispatcher;
        try {
            // a read call on the input stream will block for only 5 seconds
            streams.setSoTimeout(55000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Message toSend = queue.take();
                // fixed bug: writeUnshared instead of writeObject
                outputStream.writeUnshared(toSend);
                outputStream.flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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
