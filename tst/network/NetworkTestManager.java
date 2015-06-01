package network;

import java.io.IOException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * The manager that starts and tests interactions between mocked nodes.
 *
 * Created by Sorin Nutu on 6/1/2015.
 */
public class NetworkTestManager {
    public static void main(String[] args) throws InterruptedException, IOException {
        Logger logger = Logger.getLogger("test");
        Handler fileHandler = new FileHandler("./logs/test.log", true);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);

        logger.info("START!");

        MockedNode node1 = new MockedNode("localhost", 10000, 500, null);
        Thread.sleep(5000);
        MockedNode node2 = new MockedNode("localhost", 10001, 600, null);
        Thread.sleep(5000);
        MockedNode node3 = new MockedNode("localhost", 10002, 400, null);
        Thread.sleep(5000);

        node3.broadcastMessage("Broadcast de la nodul 3.");
        node2.broadcastMessage("Broadcast de la nodul 2.");
        Thread.sleep(10000);

        String result = "The predecessor of node " + node1.getId() + " is " + node1.getPredecessor().getKey() + "\n";
        result += "The successor of node " + node1.getId() + " is " + node1.getSuccessor().getKey() + "\n";
        result += "The predecessor of node " + node2.getId() + " is " + node2.getPredecessor().getKey() + "\n";
        result += "The successor of node " + node2.getId() + " is " + node2.getSuccessor().getKey() + "\n";
        result += "The predecessor of node " + node3.getId() + " is " + node3.getPredecessor().getKey() + "\n";
        result += "The successor of node " + node3.getId() + " is " + node3.getSuccessor().getKey() + "\n";

        result += "Messages received by node 1:\n";
        List<String> messages = node1.getReceivedBroadcastMessages();

        for (String message : messages) {
            result += message + "\n";
        }
        result += "\n";

        messages = node2.getReceivedBroadcastMessages();

        result += "Messages received by node 2:\n";
        for (String message : messages) {
            result += message + "\n";
        }
        result += "\n";

        messages = node3.getReceivedBroadcastMessages();

        result += "Messages received by node 3:\n";
        for (String message : messages) {
            result += message + "\n";
        }
        result += "\n";

        logger.info(result);
    }
}
