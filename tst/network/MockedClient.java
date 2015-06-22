package network;

import currency.Block;
import currency.Client;
import currency.Transaction;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A mock class that creates a MockedNode instead of a Node and saves the transactions received and other data.
 * Created by Sorin Nutu on 6/7/2015.
 */
public class MockedClient extends Client {
    private Set<Transaction> receivedTransactions;
    private Set<Block> receivedBlocks;

    /**
     * The constructor stores the id and uses it in the overridden method initNode, when a MockNode is created.
     * @param ip
     * @param port
     * @param id
     */
    public MockedClient(String ip, int port, String bootstrapIp, int bootstrapPort, long id) {
        super(ip, port, bootstrapIp, bootstrapPort);
        this.id = id;
        receivedTransactions = new HashSet<>();
        receivedBlocks = new HashSet<>();
    }

    @Override
    protected void initNode() {
        networkNode = new MockedNode(ip, port, bootstrapIp, bootstrapPort, id, this);
        initLogger();
    }

    @Override
    protected void initLogger() {
        transactionsLogger = Logger.getLogger("transactions-" + id);
    }

    @Override
    protected void storeReceivedTransaction(Transaction transaction) {
        receivedTransactions.add(transaction);
    }

    @Override
    protected void storeReceivedBlock(Block block) {
        receivedBlocks.add(block);
    }

    public Set<Transaction> getReceivedTransactions() {
        return receivedTransactions;
    }

    public Set<Block> getReceivedBlocks() {
        return receivedBlocks;
    }

    public long getNodeId() {
        return networkNode.getId();
    }

    public long getNodeSuccessor() {
        return networkNode.getSuccessor().getKey();
    }

    public long getNodePredecessor() {
        return networkNode.getPredecessor().getKey();
    }
}
