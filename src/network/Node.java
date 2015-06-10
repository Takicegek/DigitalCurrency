package network;

import currency.Block;
import currency.BlockchainAndTransactionsWrapper;
import currency.Client;
import currency.Transaction;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Represents a network node.
 * The network is similar to Chord Distributed Hash Table.
 *
 * Created by Sorin Nutu on 2/16/2015.
 */
public class Node {
    protected static final int LOG_NODES = 10;
    protected static final long NUMBER_OF_NODES = 1 << LOG_NODES;
    protected static final long STORED_SUCCESSORS = 2;

    protected long id;
    protected int port;
    protected String ip;
    protected List<NodeInfo> fingerTable;
    // the successor list is required to handle node failures
    protected List<Integer> bootstrapNodes;
    protected ServerSocket serverSocket;

    protected NodeInfo predecessor;
    protected NodeInfo successor;
    protected NodeInfo nextSuccessor;

    protected Dispatcher dispatcher;
    // the network node needs to know about the client because other clients could ask for the block chain
    protected Client client;

    protected Logger networkLogger;

    protected Node() {

    }

    public Node(final String ip, final int port, Client client) {
        bootstrapNodes = new ArrayList<>();
        bootstrapNodes.add(10000);

        this.client = client;

        fingerTable = new ArrayList<>(LOG_NODES);
        dispatcher = new Dispatcher(this);

        // the identifier for this node in the ring
        id = (long) ((Math.random() * NUMBER_OF_NODES));
        initLogger();

        networkLogger.info("Am id-ul " + id);
        this.ip = ip;
        this.port = port;

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

    protected void startChordThreads(final String ip, final int port) {
        // run the SocketListener in a separate thread to handle incoming messages
        new Thread() {
            @Override
            public void run() {
                listen(ip, port);
            }
        }.start();

        // periodically run the stabilize procedure to check if a node joined between this node and its successor
        // and to inform the successor that this node is its predecessor (in case the current node just joined)
        new StabilizeThread(this, dispatcher).start();

        // create a separate thread that will fix the fingers
        new FixFingersThread(dispatcher, id, this).start();

        // check for predecessor thread
        new CheckPredecessorThread(this, dispatcher).start();

        // thread that asks the successor about its successor
        new AskForSuccessorsThread(this, dispatcher).start();
    }

    protected NodeInfo findSuccessor(String ip, int port) {
        NodeInfo nodeInfo = null;
        try {
            boolean collision;
            Message message = new Message(MessageType.FIND_SUCCESSOR_JOIN, id);
            NodeInfo info = new NodeInfo(ip, port, -1);

            // loop until an id that is not already present in the ring is found
            do {
                collision = false;

                System.out.println(message);
                Future<Message> messageFuture = dispatcher.sendMessage(message, true, info);
                Message received = messageFuture.get();

                if (received.getObject() != null) {
                    nodeInfo = (NodeInfo) received.getObject();
                } else {
                    // the message body is null so the id is already in use
                    collision = true;
                    id = (long) ((Math.random() * NUMBER_OF_NODES));
                    message.setObject(id);
                }
            } while (collision == true);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return nodeInfo;
    }

    protected void listen(String ip, int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            Socket client;
            try {
                client = serverSocket.accept();
                dealWithClient(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a broadcast message. Here the message is sent only to successor.
     * Using this method, it is not guaranteed that the message will arrive at the destination.
     *
     * The way broadcast is implemented is described in BroadcastMessageWrapper.
     *
     * @param objectToSend
     */
    public void broadcastMessage(Object objectToSend) {
        long successorId = successor.getKey();

        BroadcastMessageWrapper wrapper = new BroadcastMessageWrapper(successorId,
                (id - 1 + NUMBER_OF_NODES) % NUMBER_OF_NODES, objectToSend);
        Message message = new Message(MessageType.BROADCAST_MESSAGE, wrapper);

        dispatcher.sendMessage(message, false, 0);
    }

    public void broadcastTransaction(Transaction transaction) {
        long successorId = successor.getKey();

        BroadcastMessageWrapper wrapper = new BroadcastMessageWrapper(successorId,
                (id - 1 + NUMBER_OF_NODES) % NUMBER_OF_NODES, transaction);
        Message message = new Message(MessageType.BROADCAST_TRANSACTION, wrapper);

        dispatcher.sendMessage(message, false, 0);
    }

    /**
     * Send the block to all the nodes, including the sender.
     * @param block
     */
    public void broadcastBlock(Block block) {
        long successorId = successor.getKey();

        BroadcastMessageWrapper wrapper = new BroadcastMessageWrapper(successorId,
                (id - 1 + NUMBER_OF_NODES) % NUMBER_OF_NODES, block);
        Message message = new Message(MessageType.BROADCAST_BLOCK, wrapper);

        dispatcher.sendMessage(message, false, 0);
    }

    public void handleReceivedMessage(BroadcastMessageWrapper message) {
        System.out.println("Am primit un mesaj broadcast: " + message.getMessage());
    }


    public void handleReceivedTransaction(Transaction transaction) {
        client.handleReceivedTransaction(transaction);
    }

    public void handleReceivedBlock(Block block) {
        client.handleReceivedBlock(block);
    }

    /**
     * Ask the successor for its block chain and unspent transactions database.
     * They will be wrapped in a BlockchainAndTransactionsWrapper object.
     * @return
     */
    public BlockchainAndTransactionsWrapper askSuccessorForBlockchain() {
        Message message = new Message(MessageType.GET_BLOCKCHAIN, null);
        Message answer = null;

        try {
            answer = sendReliable(message, 0);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return (BlockchainAndTransactionsWrapper) answer.getObject();
    }

    /**
     * Send a message and waits for an answer. If an error occurs and the message is not successfully sent or
     * received, the message is sent again.
     *
     * @param message
     * @param finger
     * @return the received message
     */
    protected Message sendReliable(Message message, int finger)
            throws ExecutionException, InterruptedException {
        boolean success = false;
        Future<Message> future;
        Message answer;

        do {
            future = dispatcher.sendMessage(message, true, finger);
            answer = future.get();

            if (answer.getType() != MessageType.RETRY) {
                success = true;
            }
        } while (!success);

        return answer;
    }

    public BlockchainAndTransactionsWrapper getBlockchainAndTransactions() {
        return new BlockchainAndTransactionsWrapper(client.getUnspentTransactions(),
                client.getBlockchain(), client.getOrphanBlocks(),
                client.getTransactionsWithoutBlock(), client.getLastBlockInChain());
    }

    protected void initLogger() {
        try {
            networkLogger = Logger.getLogger("node-" + id);
            Handler fileHandler = new FileHandler("./logs/network-node-" + id + ".log", true);
            fileHandler.setFormatter(new SimpleFormatter());

            networkLogger.addHandler(fileHandler);
            // do not print to console
            networkLogger.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void dealWithClient(Socket client) {
        new Thread(new SocketListener(client, this, dispatcher)).start();
    }

    public long getId() {
        return id;
    }

    public NodeInfo getSuccessor() {
        return successor;
    }

    public NodeInfo getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(NodeInfo predecessor) {
        this.predecessor = predecessor;
    }

    public void setSuccessor(NodeInfo successor) {
        this.successor = successor;
    }

    public List<NodeInfo> getFingerTable() {
        return fingerTable;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public NodeInfo getNodeInfo() {
        return new NodeInfo(ip, port, id);
    }

    public NodeInfo getNextSuccessor() {
        return nextSuccessor;
    }

    public void setNextSuccessor(NodeInfo nextSuccessor) {
        this.nextSuccessor = nextSuccessor;
    }

    public Logger getNetworkLogger() {
        return networkLogger;
    }
}
