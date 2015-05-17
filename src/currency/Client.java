package currency;

import currency.utils.PublicKeyUtils;
import network.Node;

import java.io.IOException;
import java.security.*;
import java.util.*;
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
    private List<Transaction> transactionsWithoutBlock;
    // store the blockchain indexed by blocks' nonces
    private Map<Long, Block> blockchain;
    // received blocks that do not have a parent yet
    private Map<Long, Block> orphanBlocks;
    private double balance;
    // the current node in the peer to peer network
    private Node networkNode;
    private long id;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private ProofOfWork proofOfWorkInstance;
    private Thread proofOfWorkThread;
    // a leaf in the blockchain - defines the client's state
    private Block lastBlockInChain;
    private final ProofOfWork PROOF_OF_WORK_VERIFIER = new HashProofOfWork(null, null, null);

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
            blockchain = new HashMap<>();
            orphanBlocks = new HashMap<>();
            transactionsWithoutBlock = new ArrayList<>();

            lastBlockInChain = Block.createGenesisBlock();
            blockchain.put(0L, lastBlockInChain);

            // create a fake transaction to introduce money in the network
            TransactionRecord record = new TransactionRecord(publicKey, publicKey, 10);
            unspentTransactions.add(record);
        } else {
            // ask the successor for the unspentTransactions and the block chain
            BlockchainAndTransactionsWrapper wrapper = networkNode.askSuccessorForBlockchain();
            unspentTransactions = wrapper.getUnspentTransactions();
            blockchain = wrapper.getBlockchain();
            orphanBlocks = wrapper.getOrphanBlocks();
            transactionsWithoutBlock = wrapper.getTransactionsWithoutblock();
            lastBlockInChain = wrapper.getLastBlockInChain();

            String logMessage = "Received the blockchain from successor.\n";
            logMessage = logMessage + "Unspent transactions size = " + unspentTransactions.size() + "\n";
            for (TransactionRecord t : unspentTransactions) {
                logMessage = logMessage + "Transaction: " + t.getAmount() + "\n";
            }
            logMessage = logMessage + "There are " + transactionsWithoutBlock.size() + " transactions without a block.\n";
            transactionsLogger.info(logMessage);

            // create a fake transaction to introduce money in the network (this should be removed and left only
            // for the bootstrap node) !!
            TransactionRecord record = new TransactionRecord(publicKey, publicKey, 10);
            unspentTransactions.add(record);
        }

        proofOfWorkInstance = new HashProofOfWork(this, networkNode, lastBlockInChain);
        startProofOfWorkThread();
    }

    /**
     * The ProofOfWork instance has a state, which is represented by its members.
     * Start a new thread (run the mine() method) using the same object with the old state.
     * The transactions will not be lost.
     */
    private void startProofOfWorkThread() {
        proofOfWorkThread = new Thread(proofOfWorkInstance);
        proofOfWorkThread.start();
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

    /**
     * todo: question
     * The method is run on a thread started in network.SocketListener. Is it correct to do this way?
     * Same question for handleReceivedBlock.
     * @param transaction
     */
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

                transactionsWithoutBlock.add(transaction);
                // If the transaction is accepted, stop the proof of work thread, wait for it,
                // add the new transaction and start it again
                if (proofOfWorkInstance.accept(transaction)) {
                    proofOfWorkInstance.stop();

                    try {
                        proofOfWorkThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    proofOfWorkInstance.addTransaction(transaction);
                    startProofOfWorkThread();
                }
            } else {
                logMessage = logMessage + "The transaction has inputs that are already spend or are not addressed to the sender!";
            }
        } else {
            logMessage = logMessage + "The transaction does not have a valid digital signature!";
        }

        transactionsLogger.info(logMessage);
    }

    public void handleReceivedBlock(Block block) {
        System.out.println("I received a block!" + block);

        // validate the proof of work and check the signatures for the transactions in the block
        // there is one more step that is deferred - verifying that there is no double spend
        // this last step will be done when the block will be on the actual chain (now it is a leaf in a tree)
        if (PROOF_OF_WORK_VERIFIER.verify(block) && block.validateTransactionsInBlock()) {
            long previousId = block.getPreviousBlock().getNonce();
            if (blockchain.containsKey(previousId)) {
                blockchain.put(block.getNonce(), block);

                // check if there is an orphaned block that has the current block as predecessor and return
                // the child with the higher height
                block = addOrphanedBlocks(block);

                if (block.getHeight() > lastBlockInChain.getHeight()) {
                    // stop the mining process

                    // change the lastBlockInChain to be the longest block
                    changeLastBlockInChain(block);

                    // start mining

                }
            } else {
                orphanBlocks.put(block.getNonce(), block);
            }
        }
    }

    /**
     * A higher block arrived on other branch and the chain of blocks will be changed.
     *
     * The unspent transactions and the transactions without block data structures need to be updated. This will be
     * done in the following way:
     *
     * For every block from the lastBlockInChain to the lowest common ancestor add all the transactions in the
     * transactions without block hash, remove the transaction outputs from the unspentTransactions and add the
     * transaction inputs in the unspentTransactions.
     *
     * For every block from the lowest common ancestor to the longerBlock verify that all the input records
     * are not already spent, remove them from unspentTransactions and add the outputs in unspentTransactions.
     * The other verifications for a block (proof of work and transactions' digital signatures) were verified
     * when the block was received and added in the block tree.
     *
     * If there is an attempt of double spending the initial state will be restored and the lastBlockInChain will
     * remain the same. The block where the double spending was discovered will be removed with all its successors.
     *
     * @param longerBlock
     */
    private void changeLastBlockInChain(Block longerBlock) {

    }

    /**
     * Given a new, longer block, finds the height of the lowest common ancestor of the two nodes.
     * Illustration:
     *               - e -       f - g - new_block
     * a - b - c - d - h - current
     *
     * The lowest common ancestor of new_block and current is d.
     *
     * Algorithm:
     * Start from the new_block and go up to the root new_block.height() - current.height() steps. This way,
     * there will be two nodes on different branches at the same height (f and current). After that, go up with
     * both, one step at a time, until they have the same parent. When this happens, their parent is the lowest
     * common ancestor.
     *
     * @param longerBlock
     * @return
     */
    private long findLowestCommonAncestor(Block longerBlock) {
        long difference = longerBlock.getHeight() - lastBlockInChain.getHeight();
        Block ancestor = longerBlock;
        Block auxiliary;

        while (difference != 0) {
            ancestor = ancestor.getPreviousBlock();
            difference--;
        }

        auxiliary = lastBlockInChain;
        while (!ancestor.equals(auxiliary)) {
            ancestor = ancestor.getPreviousBlock();
            auxiliary = auxiliary.getPreviousBlock();
        }

        return ancestor.getHeight();
    }

    /**
     * When a block does not have its parent in the blockchain, it is added in the orphanBlocks map.
     * For every new block, the orphanBlocks map is scanned in order to find blocks that are children of the
     * received block, but they arrived earlier.
     *
     * If a block is found, it is removed from orphanedBlocks and added in the blockchain. After that, the search is
     * run again to find children of the block that is not an orphan anymore.
     *
     * The method returns the block with the higher height. This could be the received block, a child of it or
     * a child of a child of it.
     *
     * @param receivedBlock
     */
    private Block addOrphanedBlocks(Block receivedBlock) {
        boolean orphanFound;
        Block child = null;
        do {
            orphanFound = false;
            synchronized (orphanBlocks) {
                for (Block block : orphanBlocks.values()) {
                    if (block.getPreviousBlock().getNonce() == receivedBlock.getNonce()) {
                        orphanFound = true;
                        child = block;
                    }
                }
                if (orphanFound) {
                    orphanBlocks.remove(child.getNonce());
                    blockchain.put(child.getNonce(), child);
                    receivedBlock = child;
                }
            }
        } while (orphanFound == true);
        return receivedBlock;
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

    public void removeFromCandidateTransactions(List<Transaction> processed) {
        for (Transaction transaction : processed) {
            transactionsWithoutBlock.remove(transaction);
        }
    }

    public Map<Long, Block> getBlockchain() {
        return blockchain;
    }

    public Set<TransactionRecord> getUnspentTransactions() {
        return unspentTransactions;
    }

    public List<Transaction> getTransactionsWithoutBlock() {
        return transactionsWithoutBlock;
    }

    public Block getLastBlockInChain() {
        return lastBlockInChain;
    }

    public Map<Long, Block> getOrphanBlocks() {
        return orphanBlocks;
    }
}
