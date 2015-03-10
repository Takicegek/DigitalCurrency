package network;

import utils.NodeGUI;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public class SocketListener implements Runnable {

    private Socket client;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private Node correspondingNode;
    private Dispatcher dispatcher;

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
    }

    @Override
    public void run() {
        Object messageObject;
        try {
            while((messageObject = reader.readObject()) != null) {
                Message message = (Message) messageObject;

                // handle the message in a separate thread, so the caller should not wait until the current
                // message is entirely processed
                SocketListenerMessageHandlingThread thread = new SocketListenerMessageHandlingThread(message,
                        correspondingNode, writer, dispatcher);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println((new Date()).toString() + " " + correspondingNode.getId() + ": Lost contact with a node that closed the socket." + client.getPort() + " " + client.getLocalPort());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    protected static boolean belongsToInterval(long id, long nodeId, long successor) {
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
}
