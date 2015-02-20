package network;

import com.sun.corba.se.impl.io.OutputStreamHook;
import utils.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a network node.
 * The network is similar to Chord Distributed Hash Table.
 *
 * Created by Sorin Nutu on 2/16/2015.
 */
public class Node {

    private static final int LOG_NODES = 10;
    private static final long NUMBER_OF_NODES = (long) Math.pow(2, LOG_NODES);

    private long id;
    private int port;
    private String ip;
    private List<Pair<NodeInfo, Streams>> fingerTable;
    private List<Integer> bootstrapNodes;
    private ServerSocket serverSocket;

    private Pair<NodeInfo, Streams> predecessor;
    private Pair<NodeInfo, Streams> successor;

    public Node(final String ip, final int port) {
        // todo get bootstrap nodes from configuration
        bootstrapNodes = new ArrayList<Integer>();
        bootstrapNodes.add(10000);

        fingerTable = new ArrayList<Pair<NodeInfo, Streams>>();
        // the identifier for this node in the ring
        id = (long) ((Math.random() * NUMBER_OF_NODES));
        System.out.println("Am id-ul " + id);
        this.ip = ip;
        this.port = port;

        if (bootstrapNodes.contains(port)) {
            successor = new Pair<NodeInfo, Streams>(new NodeInfo(ip, port, id), null);
            fingerTable.add(successor);
        } else {
            try {
                Socket s = new Socket(ip, 10000);
                ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());

                writer.writeObject(new Message(MessageType.FIND_SUCCESSOR, id));
                writer.flush();

                ObjectInputStream reader = new ObjectInputStream(s.getInputStream());
                Message received = (Message) reader.readObject(); // todo this may return null

                if (received.getType() == MessageType.FIND_SUCCESSOR) {
                    successor = new Pair<NodeInfo, Streams>();

                    NodeInfo info = (NodeInfo) received.getObject();
                    successor.setFirst(info);
                    Socket successorSocket = new Socket(info.getIp(), info.getPort());
                    successor.setSecond(new Streams(successorSocket));
                }

                System.out.println("Succesorul meu este " + successor.getFirst());
                fingerTable.add(successor);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        new Thread() {
            @Override
            public void run() {
                listen(ip, port);
            }
        }.start();

        new Thread() {
            @Override
            public void run() {
                try {
                    stabilize();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * The current node asks the successor about its predecessor.
     * It verifies if n's immediate successor is consistent, and tells the successor about n.
     *
     * The described task is run periodically.
     */
    private void stabilize() throws IOException, ClassNotFoundException, InterruptedException {
        while (true) {
            if (successor.getFirst().getKey() == id && successor.getFirst().getPort() == port) {
                stabilizeFirstNodeInRing();
            } else {
                // ask the successor for its predecessor
                // reuse the streams associated with the successor
                ObjectOutputStream outputStream = successor.getSecond().getObjectOutputStream();
                outputStream.writeObject(new Message(MessageType.GET_PREDECESSOR, null));

                // get the answer
                ObjectInputStream inputStream = successor.getSecond().getObjectInputStream();
                Message received = (Message) inputStream.readObject();

                // we know that the object inside the message is a NodeInfo
                NodeInfo receivedNode = (NodeInfo) received.getObject();
                System.out.println("Asked my successor " + successor.getFirst() + " about its predecessor and the answer is: " + receivedNode);

                // if my successor has a predecessor different by, there are two cases
                // 1. I just joined and my successor does not know about me
                // 2. Another node joined between me and my successor
                // Should determine here if I change the successor.
                if (receivedNode != null && receivedNode.getKey() != id &&
                        SocketListener.belongsToInterval(receivedNode.getKey(), id, successor.getFirst().getKey())) {
                    // the predecessor received from my successor is in front of me, so it becomes my successor
                    Socket socket = new Socket(receivedNode.getIp(), receivedNode.getPort());
                    Streams streams = new Streams(socket);
                    successor = new Pair<NodeInfo, Streams>(receivedNode, streams);
                    fingerTable.remove(0);
                    fingerTable.add(successor);
                    System.out.println("I have a new successor! It is " + receivedNode.toString());
                }

                // notify the successor about its predecessor, which is the current node
                Message message = new Message(MessageType.NOTIFY_SUCCESSOR, new NodeInfo(ip, port, id));
                outputStream = successor.getSecond().getObjectOutputStream();
                outputStream.writeObject(message);
                System.out.println("Notified my successor about my presence.");
            }

            Thread.sleep(5000);
        }
    }

    /**
     * The stabilize() method sends a message through a Socket to the successor. When the network has only one node,
     * it cannot send messages to itself. This method implements the same behaviour as stabilize(), but it does not
     * send any messages. The node's successor is the node itself.
     */
    private void stabilizeFirstNodeInRing() {
        if (predecessor == null) {
            predecessor = new Pair<NodeInfo, Streams>(new NodeInfo(ip, port, id), null);
            System.out.println("Stabilized the first node in ring.");
        } else {
            // if another node joined and it has this node as its successor, it notifies and the current
            // node changes its predecessor.
            if (predecessor.getFirst().getKey() != id && predecessor.getFirst().getPort() != port) {
                // another node joined and it is between the current node and its successor
                // set that node as a successor
                successor.setFirst(predecessor.getFirst());
                Socket socket = null;
                try {
                    socket = new Socket(successor.getFirst().getIp(), successor.getFirst().getPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                successor.setSecond(new Streams(socket));
            }
        }
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
                dealWithClient(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dealWithClient(Socket client) {
        new Thread(new SocketListener(client, this)).start();
    }

    public long getId() {
        return id;
    }

    public NodeInfo getSuccessor() {
        return successor.getFirst();
    }

    public NodeInfo getPredecessor() {
        return predecessor == null ? null : predecessor.getFirst();
    }

    public void setPredecessor(Pair<NodeInfo, Streams> predecessor) {
        this.predecessor = predecessor;
    }

    public List<Pair<NodeInfo, Streams>> getFingerTable() {
        return fingerTable;
    }
}
