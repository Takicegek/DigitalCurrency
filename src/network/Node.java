package network;

import utils.NodeGUI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
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
    private volatile List<CompleteNodeInfo> fingerTable;
    // the successor list is required to handle node failures
    private List<Integer> bootstrapNodes;
    private ServerSocket serverSocket;

    private CompleteNodeInfo predecessor;
    private CompleteNodeInfo successor;

    private Dispatcher dispatcher;

    public Node(final String ip, final int port) {
        // todo get bootstrap nodes from configuration
        bootstrapNodes = new ArrayList<Integer>();
        bootstrapNodes.add(10000);

        fingerTable = new ArrayList<CompleteNodeInfo>(LOG_NODES);
        dispatcher = new Dispatcher(this);

        // the identifier for this node in the ring
        id = (long) ((Math.random() * NUMBER_OF_NODES));
        System.out.println("Am id-ul " + id);
        this.ip = ip;
        this.port = port;

        if (bootstrapNodes.contains(port)) {
            successor = new CompleteNodeInfo(new NodeInfo(ip, port, id), null);
        } else {
            try {
                successor = new CompleteNodeInfo();

                NodeInfo info = findSuccessor(ip, 10000);
                successor.setNodeInfo(info);
                Socket successorSocket = new Socket(info.getIp(), info.getPort());
                successor.setStreams(new Streams(successorSocket));

                System.out.println(id + ": My successor is " + successor.getKey());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // fill the fingertable
        for (int i = 0; i < LOG_NODES; i++) {
            fingerTable.add(successor);
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
//        new FixFingersThread(dispatcher, id, this).start();
    }



    private NodeInfo findSuccessor(String ip, int port) {
        NodeInfo nodeInfo = null;
        try {
            Socket s = new Socket(ip, port);
            boolean collision;
            Message message = new Message(MessageType.FIND_SUCCESSOR_JOIN, id);
            NodeInfo info = new NodeInfo(ip, port, -1);
            Streams streams = new Streams(s);

            // loop until an id that is not already present in the ring is found
            do {
                collision = false;

                System.out.println(message);
                Future<Message> messageFuture = dispatcher.sendMessage(message, true, new CompleteNodeInfo(info, streams));
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

            s.close();
        } catch(IOException e) {
            e.printStackTrace();
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

    private void dealWithClient(Socket client) {
        new Thread(new SocketListener(client, this, dispatcher)).start();
    }

    public long getId() {
        return id;
    }

    public CompleteNodeInfo getSuccessor() {
        return successor;
    }

    public CompleteNodeInfo getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(CompleteNodeInfo predecessor) {
        this.predecessor = predecessor;
    }

    public void setSuccessor(CompleteNodeInfo successor) {
        this.successor = successor;
    }

    public List<CompleteNodeInfo> getFingerTable() {
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
}
