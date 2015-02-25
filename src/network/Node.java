package network;

import utils.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a network node.
 * The network is similar to Chord Distributed Hash Table.
 *
 * Created by Sorin Nutu on 2/16/2015.
 */
public class Node {

    protected static final int LOG_NODES = 5;
    protected static final long NUMBER_OF_NODES = 1 << LOG_NODES;
    protected static final long STORED_SUCCESSORS = 2;

    private long id;
    private int port;
    private String ip;
    private List<Pair<NodeInfo, Streams>> fingerTable;
    // the successor list is required to handle node failures
    private List<Pair<NodeInfo, Streams>> successors;
    private List<Integer> bootstrapNodes;
    private ServerSocket serverSocket;

    private Pair<NodeInfo, Streams> predecessor;
    private Pair<NodeInfo, Streams> successor;

    private HashSet<Message> messagePool;

    public Node(final String ip, final int port) {
        // todo get bootstrap nodes from configuration
        bootstrapNodes = new ArrayList<Integer>();
        bootstrapNodes.add(10000);

        fingerTable = new ArrayList<Pair<NodeInfo, Streams>>(LOG_NODES);
        successors = new ArrayList<Pair<NodeInfo, Streams>>(LOG_NODES);

        messagePool = new HashSet<Message>();

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
                successor = new Pair<NodeInfo, Streams>();

                NodeInfo info = findSuccessor(ip, 10000);
                successor.setFirst(info);
                Socket successorSocket = new Socket(info.getIp(), info.getPort());
                successor.setSecond(new Streams(successorSocket));

                System.out.println(id + ": My successor is " + successor.getFirst());
                fingerTable.add(successor);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

        // create a separate thread that will fix the fingers
//        new FixFingersThread(fingerTable, id, this).start();
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
                boolean ioException = false;
                ObjectOutputStream outputStream = null;

                /*
                    If an IOException occurs, it means that the successor left.
                    A FIND_SUCCESSOR_JOIN message is sent to a bootstrap node and a new successor is found.
                    However, it may also leave and another exception would be thrown.
                    Solution: loop until the message is actually sent.
                 */
                // todo set as successor the next node in fingertable
                do {
                    ioException = false;
                    try {
                        outputStream = successor.getSecond().getObjectOutputStream();
                        // an exception will be thrown if the successor exited
                        outputStream.writeObject(new Message(MessageType.GET_PREDECESSOR, null)); // todo error if successor exited
                        outputStream.flush();
                    } catch (IOException e) {
                        System.out.println(id + ": Changing the successor because of an IOException.");
                        // the successor exited the network
                        // get a new successor
//                        NodeInfo nodeInfo = findSuccessor("localhost", 10000);
//                        successor.setFirst(nodeInfo);
//                        successor.setSecond(new Streams(new Socket(nodeInfo.getIp(), nodeInfo.getPort())));

                        break;
//                        ioException = true; // repeat the loop
                    }
                } while(ioException);

                // get the answer
                // exit the current iteration of the while loop if an exception is caught
                Message received = null;
                try {
                    ObjectInputStream inputStream = successor.getSecond().getObjectInputStream();

                    synchronized (this) {
                        received = (Message) inputStream.readObject(); // todo aici e blocat
                    }

                    // add the message in the message pool
                    insertMessage(received);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                received = getMessage(MessageType.GET_PREDECESSOR);
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

                    // close the old socket
                    successor.getSecond().closeSocket();

                    successor = new Pair<NodeInfo, Streams>(receivedNode, streams);
                    fingerTable.remove(0);
                    fingerTable.add(0, successor);
                    System.out.println(id + ": I have a new successor! It is " + receivedNode.toString());
                }

                // notify the successor about its predecessor, which is the current node
                Message message = new Message(MessageType.NOTIFY_SUCCESSOR, new NodeInfo(ip, port, id));
                outputStream = successor.getSecond().getObjectOutputStream();
                outputStream.writeObject(message);
//                System.out.println("Notified my successor about my presence.");
            }

            Thread.sleep(5000);
        }
    }

    private NodeInfo findSuccessor(String ip, int port) {
        NodeInfo nodeInfo = null;
        try {
            Socket s = new Socket(ip, port);
            ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream reader = new ObjectInputStream(s.getInputStream());

            boolean collision;

            // loop until an id that is not already present in the ring is found
            do {
                collision = false;

                writer.writeObject(new Message(MessageType.FIND_SUCCESSOR_JOIN, id));
                writer.flush();

                Message received = (Message) reader.readObject();

                if (received.getType() == MessageType.SUCCESSOR_FOUND) {
                    nodeInfo = (NodeInfo) received.getObject();
                } else {
                    // the message type is USED_ID
                    collision = true;
                    id = (long) ((Math.random() * NUMBER_OF_NODES));
                    System.out.println("Am id-ul " + id);
                }
            } while (collision == true);

            s.close();
        } catch(IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return nodeInfo;
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
            // node changes its predecessor; check if the predecessor is changed.
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

    public synchronized void insertMessage(Message message) {
        messagePool.add(message);
    }

    /**
     * Get a message from the message pool that has a certain type.
     * When this method is invoked the message may not be present. Loop until the message is found.
     *
     * @param type The type of the message to be returned
     * @return The received message
     */
    public synchronized Message getMessage(MessageType type) {
        Iterator<Message> iterator;
        Message current, received = null;
        boolean found = false;

        while(found == false) {
            iterator = messagePool.iterator();
            while (iterator.hasNext()) {
                current = iterator.next();
                if (current.getType() == type) {
                    received = current;
                    iterator.remove();
                    found = true;
                    break;
                }
            }

            try {
                if (!found) {
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return received;
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

    public Pair<NodeInfo, Streams> getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Pair<NodeInfo, Streams> predecessor) {
        this.predecessor = predecessor;
    }

    public List<Pair<NodeInfo, Streams>> getFingerTable() {
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
