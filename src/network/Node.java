package network;

import java.io.IOException;
import java.net.*;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    private long id;
    private int port;
    private String ip;
    private List<NodeInfo> fingerTable;
    // the successor list is required to handle node failures
    private List<Integer> bootstrapNodes;
    private ServerSocket serverSocket;

    private NodeInfo predecessor;
    private NodeInfo successor;
    private NodeInfo nextSuccessor;

    private Dispatcher dispatcher;

    public Node(final String ip, final int port) {
        bootstrapNodes = new ArrayList<Integer>();
        bootstrapNodes.add(10000);

        fingerTable = new ArrayList<NodeInfo>(LOG_NODES);
        dispatcher = new Dispatcher(this);

        // the identifier for this node in the ring
        id = (long) ((Math.random() * NUMBER_OF_NODES));
        System.out.println("Am id-ul " + id);
        this.ip = ip;
        this.port = port;

        if (bootstrapNodes.contains(port)) {
            successor = new NodeInfo(ip, port, id);
        } else {
            successor = findSuccessor(ip, 10000);
            System.out.println(id + ": My successor is " + successor.getKey());

            dispatcher.sendMessage(new Message(MessageType.NOTIFY_SUCCESSOR, getNodeInfo()), false, successor);
        }

        fingerTable.add(successor);
        // fill the fingertable
        for (int i = 1; i < LOG_NODES; i++) {
            fingerTable.add(new NodeInfo("localhost", -1, -1));
        }

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



    private NodeInfo findSuccessor(String ip, int port) {
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
                    System.out.println("Coliziune. Noul id: " + id);
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

    private void listen(String ip, int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Listening at port " + port + " . . . ");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            Socket client;
            try {
                client = serverSocket.accept();
                System.out.println("PORNESC UN LISTENER PENTRU " + client.getRemoteSocketAddress().toString());
                dealWithClient(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a broadcast message. Here the message is sent only to successor.
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

    public void handleReceivedMessage(BroadcastMessageWrapper message) {
        System.out.println("Am primit un mesaj broadcast: " + message.getMessage());
    }

    private void dealWithClient(Socket client) {
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
}
