package currency;

import network.Node;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a client that has a balance, holds the block chain and can initiate transactions.
 *
 * If the node is the bootstrap one, create an unspent transaction so he can create the first transaction. Also,
 * create a block with height 0.
 *
 * Created by Sorin Nutu on 4/19/2015.
 */
public class Client {
    private Set<TransactionRecord> unspentTransactions;
    private Set<Block> blockchain;
    private double balance;
    // the current node in the peer to peer network
    private Node networkNode;
    private long id;

    public Client(String ip, int port) {
        networkNode = new Node(ip, port, this);
        this.id = networkNode.getId();
        if (port == 10000) { // todo change the condition
            // create the block chain and add the initial block
            unspentTransactions = new HashSet<>();
            blockchain = new HashSet<>();
            blockchain.add(new Block(null, 0));

            // create a fake transaction to introduce money in the network
            TransactionRecord record = new TransactionRecord(0, id, 10);
            unspentTransactions.add(record);
        } else {
            // ask the successor for the unspentTransactions and the block chain
            BlockchainAndTransactionsWrapper wrapper = networkNode.askSuccessorForBlockchain();
            unspentTransactions = wrapper.getUnspentTransactions();
            blockchain = wrapper.getBlockchain();

            System.out.println("Unspent transactions size = " + unspentTransactions.size());
            for (TransactionRecord t : unspentTransactions) {
                System.out.println("Transaction: " + t.getAmount());
            }
        }
    }

    public Set<Block> getBlockchain() {
        return blockchain;
    }

    public Set<TransactionRecord> getUnspentTransactions() {
        return unspentTransactions;
    }
}
