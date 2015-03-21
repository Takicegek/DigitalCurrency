package network;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    public CheckPredecessorThread(Node correspondingNode, Dispatcher dispatcher) {
        this.correspondingNode = correspondingNode;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        System.out.println("Check Predecessor Thread started!");
        while (true) {
            System.out.println("Check predecessor, the current is: " + correspondingNode.getPredecessor());
            if (correspondingNode.getPredecessor() != null && !correspondingNode.getPredecessor().
                    equals(correspondingNode.getNodeInfo())) {
                Message message = new Message(MessageType.CHECK_PREDECESSOR, null);
                Future<Message> future = dispatcher.sendMessage(message, false, correspondingNode.getPredecessor());

                try {
                    System.out.println("Astept mesajul de la predecesor.");
                    Message answer = future.get();
                    if (answer.getType() == MessageType.RETRY) {
                        correspondingNode.setPredecessor(null);
                    }
                    System.out.println("Predecesorul meu este " + correspondingNode.getPredecessor() + " si mesajul este " + answer.getType());
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
