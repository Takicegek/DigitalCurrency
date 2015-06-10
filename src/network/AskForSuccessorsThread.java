package network;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Created by Sorin Nutu on 3/17/2015.
 */
public class AskForSuccessorsThread extends Thread {
    private Node correspondingNode;
    private Dispatcher dispatcher;
    private Logger networkLogger;

    public AskForSuccessorsThread(Node correspondingNode, Dispatcher dispatcher) {
        this.correspondingNode = correspondingNode;
        this.networkLogger = correspondingNode.getNetworkLogger();
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        // create a reference for every message, the dispatcher may return the same message with the RETRY tag
        // and the next message that will be sent will be incorrect
//        Message message = new Message(MessageType.GET_SUCCESSOR, null);

        while (true) {
            Message message = new Message(MessageType.GET_SUCCESSOR, null);
            if (!correspondingNode.getSuccessor().equals(correspondingNode.getNodeInfo())) {
                networkLogger.info("Trimite mesaj catre " + correspondingNode.getFingerTable().get(0) + " pentru a-l intreba de succesor.");
                Future<Message> future = dispatcher.sendMessage(message, true, 0);

                Message answer = null;
                try {
                    networkLogger.info("Astept succesorul succesorului . . .");
                    answer = future.get();
                    networkLogger.info("Succesorul succesorului raspuns: " + answer.getType());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                if (answer.getType() != MessageType.RETRY) {
                    NodeInfo nextSuccessor = (NodeInfo) answer.getObject();
                    correspondingNode.setNextSuccessor(nextSuccessor);
                    networkLogger.info("Succesorul succesorului este " + nextSuccessor);
                }
            }
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
