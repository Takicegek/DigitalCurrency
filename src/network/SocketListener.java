package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public class SocketListener implements Runnable {

    private Socket client;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private Node correspondingNode;
    private Dispatcher dispatcher;
    ThreadPoolExecutor executor;

    public SocketListener(Socket client, Node correspondingNode, Dispatcher dispatcher) {
        this.client = client;
        this.correspondingNode = correspondingNode;
        try {
            reader = new ObjectInputStream(client.getInputStream());
            writer = new ObjectOutputStream(client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.dispatcher = dispatcher;

        // use a unbounded queue because all the messages should be processed
        executor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    }

    @Override
    public void run() {
        Object messageObject;
        try {
            while((messageObject = reader.readObject()) != null) {
                Message message = (Message) messageObject;

                System.err.println("Am primit in SocketListener:" + message);

                // handle the message in a separate thread, so the caller should not wait until the current
                // message is entirely processed
                SocketListenerMessageHandlingThread thread = new SocketListenerMessageHandlingThread(message,
                        correspondingNode, writer, dispatcher);
                executor.execute(thread);

            }
        } catch (IOException e) {
            System.out.println((new Date()).toString() + " " + correspondingNode.getId() + ": Lost contact with a node that closed the socket." + client.getPort() + " " + client.getLocalPort());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param id
     * @param nodeId
     * @param successor
     * @return true if id is between nodeId and successor
     */
    protected static boolean belongsToOpenInterval(long id, long nodeId, long successor) {
        if (nodeId < successor && nodeId < id && id < successor) {
            return true;
        }
        if (nodeId > successor && (id > nodeId || id < successor)) {
            return true;
        }
        // this is the single node in the network
        if (nodeId == successor) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the id belongs to (nodeId, successor].
     * @param id
     * @param nodeId
     * @param successor
     * @return
     */
    protected static boolean belongsToIntervalForBroadcast(long id, long nodeId, long successor) {
        if (nodeId < successor && nodeId < id && id <= successor) {
            return true;
        }
        if (nodeId > successor && (id > nodeId || id <= successor)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the id is in the interval [nodeId, successor]
     * @param id
     * @param nodeId
     * @param successor
     * @return true if id is between nodeId and successor
     */
    protected static boolean belongsToClosedInterval(long id, long nodeId, long successor) {
        if (nodeId < successor && nodeId <= id && id <= successor) {
            return true;
        }
        if (nodeId > successor && (id >= nodeId || id <= successor)) {
            return true;
        }
        // this is the single node in the network
        if (nodeId == successor) {
            return true;
        }
        return false;
    }
}
