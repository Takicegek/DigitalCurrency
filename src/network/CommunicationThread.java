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
public class CommunicationThread extends Thread {
    private BlockingQueue<MessageWrapper> queue;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Dispatcher dispatcher;

    public CommunicationThread(Streams streams, Dispatcher dispatcher) {
        this.queue = new ArrayBlockingQueue<MessageWrapper>(Node.LOG_NODES);
        this.inputStream = streams.getObjectInputStream();
        this.outputStream = streams.getObjectOutputStream();
        this.dispatcher = dispatcher;
        Thread.currentThread().setName("Communication thread");
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
            MessageWrapper toSend;
            Message received;
            try {
                toSend = queue.take();

                // fixed bug: writeUnshared instead of writeObject
                outputStream.writeUnshared(toSend.getMessage());
                outputStream.flush();


                if (toSend.isWaitForAnswer()) {
                    System.err.println("Am scris, astept raspuns.");
                    received = (Message) inputStream.readObject();
                    dispatcher.receiveMessage(received);
                    System.err.println("Am primit raspuns, l-am trimis la dispatcher.");
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(MessageWrapper message) {
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
