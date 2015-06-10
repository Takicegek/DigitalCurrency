package network;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * A thread that periodically checks if the predecessor is alive. If it cannot receive messages,
 * the dispatcher returns a RETRY message and the predecessor is set to null.
 *
 * The predecessor should be removed to let another node to be accepted as predecessor by the current node.
 *
 * Created by Sorin Nutu on 3/20/2015.
 */
public class CheckPredecessorThread extends Thread {
    private Node correspondingNode;
    private Dispatcher dispatcher;
    private Logger networkLogger;

    public CheckPredecessorThread(Node correspondingNode, Dispatcher dispatcher) {
        this.correspondingNode = correspondingNode;
        this.networkLogger = correspondingNode.getNetworkLogger();
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        networkLogger.info("Check Predecessor Thread started!");
        while (true) {
            networkLogger.info("Check predecessor, the current is: " + correspondingNode.getPredecessor());
            if (correspondingNode.getPredecessor() != null && !correspondingNode.getPredecessor().
                    equals(correspondingNode.getNodeInfo())) {
                Message message = new Message(MessageType.CHECK_PREDECESSOR, null);
                Future<Message> future = dispatcher.sendMessage(message, false, correspondingNode.getPredecessor());

                try {
                    networkLogger.info("Astept mesajul de la predecesor.");
                    Message answer = future.get();
                    if (answer.getType() == MessageType.RETRY) {
                        correspondingNode.setPredecessor(null);
                    }
                    networkLogger.info("Predecesorul meu este " + correspondingNode.getPredecessor() + " si mesajul este " + answer.getType());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
