package network;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Sorin Nutu on 3/17/2015.
 */
public class AskForSuccessorsThread extends Thread {
    private Node correspondingNode;
    private Dispatcher dispatcher;

    public AskForSuccessorsThread(Node correspondingNode, Dispatcher dispatcher) {
        this.correspondingNode = correspondingNode;
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
                System.out.println("Trimite mesaj catre " + correspondingNode.getFingerTable().get(0) + " pentru a-l intreba de succesor.");
                Future<Message> future = dispatcher.sendMessage(message, true, 0);

                Message answer = null;
                try {
                    System.out.println("Astept succesorul succesorului . . .");
                    answer = future.get();
                    System.out.println("Succesorul succesorului raspuns: " + answer.getType());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                if (answer.getType() != MessageType.RETRY) {
                    NodeInfo nextSuccessor = (NodeInfo) answer.getObject();
                    correspondingNode.setNextSuccessor(nextSuccessor);
                    System.out.println("Succesorul succesorului este " + nextSuccessor);
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
