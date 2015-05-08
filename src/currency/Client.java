package currency;

import currency.utils.PublicKeyUtils;
import network.Node;

import java.io.IOException;
import java.security.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Represents a client that has a balance, holds the block chain and can initiate transactions.
 *
 * If the node is the bootstrap one, create an unspent transaction so he can create the first transaction. Also,
 * create a block with height 0.
 *
 * Created by Sorin Nutu on 4/19/2015.
 */
public class Client {
    private Logger transactionsLogger;
    private Set<TransactionRecord> unspentTransactions;
    private Set<Block> blockchain;
    private double balance;
    // the current node in the peer to peer network
    private Node networkNode;
    private long id;
    private PublicKey publicKey;
    private PrivateKey privateKey;


    public Client(String ip, int port) {
        networkNode = new Node(ip, port, this);
        this.id = networkNode.getId();
        initLogger();

        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot instantiate the KeyPairGenerator." + e);
        }
        KeyPair pair = generator.generateKeyPair();
        publicKey = pair.getPublic();
        System.out.println("My address is " + PublicKeyUtils.getAddress(publicKey));
        transactionsLogger.info("My address is " + PublicKeyUtils.getAddress(publicKey));
        privateKey = pair.getPrivate();

        if (port == 10000) { // todo change the condition
            // create the block chain and add the initial block
            unspentTransactions = new HashSet<>();
            blockchain = new HashSet<>();
            blockchain.add(new Block(null, 0));

            // create a fake transaction to introduce money in the network
            TransactionRecord record = new TransactionRecord(publicKey, publicKey, 10);
            unspentTransactions.add(record);
        } else {
            // ask the successor for the unspentTransactions and the block chain
            BlockchainAndTransactionsWrapper wrapper = networkNode.askSuccessorForBlockchain();
            unspentTransactions = wrapper.getUnspentTransactions();
            blockchain = wrapper.getBlockchain();

            String logMessage = "Received the blockchain from successor.\n";
            logMessage = logMessage + "Unspent transactions size = " + unspentTransactions.size() + "\n";
            for (TransactionRecord t : unspentTransactions) {
                logMessage = logMessage + "Transaction: " + t.getAmount() + "\n";
            }
            transactionsLogger.info(logMessage);

            // create a fake transaction to introduce money in the network (this should be removed and left only
            // for the bootstrap node) !!
            TransactionRecord record = new TransactionRecord(publicKey, publicKey, 10);
            unspentTransactions.add(record);
        }
    }

    private void initLogger() {
        try {
            transactionsLogger = Logger.getLogger("transactions-" + id);
            Handler fileHandler = new FileHandler("./logs/transactions-" + id + ".log", true);
            fileHandler.setFormatter(new SimpleFormatter());

            transactionsLogger.addHandler(fileHandler);
            // do not print to console
            transactionsLogger.setUseParentHandlers(false);

            transactionsLogger.info("Transactions logger for client with network node " + id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastTransaction(Transaction transaction) {
        networkNode.broadcastTransaction(transaction);
    }

    public Set<Block> getBlockchain() {
        return blockchain;
    }

    public Set<TransactionRecord> getUnspentTransactions() {
        return unspentTransactions;
    }

    public void handleReceivedTransaction(Transaction transaction) {
        String logMessage = "I received a transaction from " + PublicKeyUtils.getAddress(transaction.getSenderPublicKey()) + "\n";
        logMessage = logMessage + "Number of inputs: " + transaction.getInputs().size() + "\n";
        logMessage = logMessage + "Number of outputs: " + transaction.getOutputs().size() + "\n";

        // Before it is added in the unspent transactions set, it should be verified
        // This is done in two steps:
        // 1. Check the digital signature
        // 2. Check that all the inputs from the transaction are not already spent
        if (transaction.hasValidDigitalSignature()) {
            if (verifyTransactionInputs(transaction)) {
                removeTransactionInputsFromUnspentSet(transaction.getInputs());
                logMessage = logMessage + "The transaction is accepted and it will be added in a block.";

                // todo: add in block
            } else {
                logMessage = logMessage + "The transaction has inputs that are already spend or are not addressed to the sender!";
            }
        } else {
            logMessage = logMessage + "The transaction does not have a valid digital signature!";
        }

        transactionsLogger.info(logMessage);
    }

    /**
     * Checks that every input record from a transaction is addressed to the sender (so he has the right to spend it)
     * and it is not already spent.
     * @param transaction
     * @return
     */
    private boolean verifyTransactionInputs(Transaction transaction) {
        boolean valid = true;
        for (TransactionRecord record : transaction.getInputs()) {
            if (!transaction.getSenderPublicKey().equals(record.getRecipient()) || !unspentTransactions.contains(record)) {
                valid = false;
            }
        }
        return valid;
    }

    private void removeTransactionInputsFromUnspentSet(List<TransactionRecord> inputs) {
        for (TransactionRecord record : inputs) {
            unspentTransactions.remove(record);
        }
    }
}
