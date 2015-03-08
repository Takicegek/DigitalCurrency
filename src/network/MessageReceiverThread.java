package network;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created by Sorin Nutu on 3/8/2015.
 */
public class MessageReceiverThread extends Thread {

    private ObjectInputStream inputStream;
    private Dispatcher dispatcher;

    public MessageReceiverThread(ObjectInputStream inputStream, Dispatcher dispatcher) {
        this.inputStream = inputStream;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        Object receivedObject;
        try {
            while ((receivedObject = inputStream.readObject()) != null) {
                Message receiedMessage = (Message) receivedObject;
                dispatcher.receiveMessage(receiedMessage);
            }
        } catch (IOException e) {
            System.out.println("Socket inchis in MessageReceverThread.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
