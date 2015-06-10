package network;

import currency.Client;

import java.util.ArrayList;
import java.util.List;

/**
 * A network Node implementation that does not randomly chose its id and saves received messages and other details.
 * It is used in various test scenarios.
 *
 * Created by Sorin Nutu on 5/30/2015.
 */
public class MockedNode extends Node {
    private List<String> receivedBroadcastMessages;

    public MockedNode(String ip, int port, Client client) {
        super(ip, port, client);
    }

    public MockedNode(String ip, int port, long id, Client client) {
        bootstrapNodes = new ArrayList<>();
        bootstrapNodes.add(10000);

        this.client = client;

        fingerTable = new ArrayList<>(LOG_NODES);
        dispatcher = new Dispatcher(this);
        receivedBroadcastMessages = new ArrayList<>();

        // the identifier for this node in the ring
        this.id = id;
        this.ip = ip;
        this.port = port;

        initLogger();

        if (bootstrapNodes.contains(port)) {
            successor = new NodeInfo(ip, port, id);
        } else {
            successor = findSuccessor(ip, 10000);
            networkLogger.info(id + ": My successor is " + successor.getKey());

            dispatcher.sendMessage(new Message(MessageType.NOTIFY_SUCCESSOR, getNodeInfo()), false, successor);
        }

        fingerTable.add(successor);
        // fill the fingertable
        for (int i = 1; i < LOG_NODES; i++) {
            fingerTable.add(new NodeInfo("localhost", -1, -1));
        }

        startChordThreads(ip, port);
    }

    @Override
    public void handleReceivedMessage(BroadcastMessageWrapper message) {
        receivedBroadcastMessages.add((String) message.getMessage());
    }

    public List<String> getReceivedBroadcastMessages() {
        return receivedBroadcastMessages;
    }
}
