package network;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created by Sorin Nutu on 3/8/2015.
 */
public class MessageReceiverThread extends Thread {

    private ObjectInputStream inputStream;
    private Dispatcher dispatcher;
    private NodeInfo source;

    public MessageReceiverThread(ObjectInputStream inputStream, Dispatcher dispatcher, NodeInfo source) {
        this.inputStream = inputStream;
        this.dispatcher = dispatcher;
        this.source = source;
    }

    @Override
    public void run() {
        Object receivedObject;
        try {
            while ((receivedObject = inputStream.readObject()) != null) {
                if (receivedObject instanceof Message) {
                    Message receviedMessage = (Message) receivedObject;
                    dispatcher.receiveMessage(receviedMessage, source);
                } else {
                    System.err.println("Received an object that is not a message: " + receivedObject.toString());
                }
            }
        } catch (IOException e) {
            System.out.println("MessageReceverThread: Encountered an IO Exception.");
            // Notify the dispatcher that all the messages from source will not arrive
            dispatcher.handleConnectionError(source);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
