package currency;

import currency.utils.PublicAndPrivateKeyUtils;
import gui.UpdateMessage;
import gui.UpdateType;
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
public class Client extends Observable {
    protected Logger transactionsLogger;
    private Set<TransactionRecord> unspentTransactions;
    private List<Transaction> transactionsWithoutBlock;
    // store the blockchain indexed by blocks' nonces
    private Map<Integer, Block> blockchain;
    // received blocks that do not have a parent yet
    private Map<Integer, Block> orphanBlocks;
    // the current node in the peer to peer network
    protected Node networkNode;
    protected long id;
    protected String ip;
    protected int port;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private ProofOfWork proofOfWorkInstance;
    private Thread proofOfWorkThread;
    // a leaf in the blockchain - defines the client's state
    private Block lastBlockInChain;
    // the root of the tree; this is used for tree traversal
    private Block genesisBlock;
    protected static final ProofOfWork PROOF_OF_WORK_VERIFIER = new HashProofOfWork(null);

    public Client(String ip, int port) {
        this.ip = ip;
        this.port = port;

        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot instantiate the KeyPairGenerator." + e);
        }
        KeyPair pair = generator.generateKeyPair();
        publicKey = pair.getPublic();
        System.out.println("My address is " + PublicAndPrivateKeyUtils.getAddress(publicKey));
        privateKey = pair.getPrivate();
    }

    public void connectToNetwork() {
        initNode(ip, port);

        if (port == 10000) { // todo change the condition
            // create the block chain and add the initial block
            unspentTransactions = new HashSet<>();
            blockchain = new HashMap<>();
            orphanBlocks = new HashMap<>();
            transactionsWithoutBlock = new ArrayList<>();

            genesisBlock = Block.createGenesisBlock();
            blockchain.put(genesisBlock.hashCode(), genesisBlock);

            lastBlockInChain = genesisBlock;

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
//            transactionsLogger.info(logMessage);
            System.out.println(logMessage);

            // find the genesis block, starting from the last block in chain
            genesisBlock = lastBlockInChain;
            while (blockchain.containsKey(genesisBlock.getPreviousBlockHash())) {
                genesisBlock = blockchain.get(genesisBlock.getPreviousBlockHash());
            }
        }

        proofOfWorkInstance = new HashProofOfWork(this);
        startProofOfWorkThread();
    }

    protected void initNode(String ip, int port) {
        networkNode = new Node(ip, port, this);
        this.id = networkNode.getId();
        initLogger();
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

    protected void initLogger() {
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

    public double getBalance() {
        double balance = 0;
        for (TransactionRecord record : unspentTransactions) {
            if (record.getRecipient().equals(publicKey)) {
                balance += record.getAmount();
            }
        }
        return balance;
    }

    public void broadcastTransaction(Transaction transaction) {
        networkNode.broadcastTransaction(transaction);
    }

    /**
     * The method is run on a thread started in network.SocketListener. Is it correct to do this way?
     * Same question for handleReceivedBlock.
     *
     * Answer: A better solution is to create a blocking queue where the listener puts messages. This way, the listener
     * does not have to be aware of the client, it just posts updates into that queue. The client thread that handles
     * the messages (blocks / transactions) wakes up when a message is received and handles it on a client thread.
     */
    public void handleReceivedTransaction(Transaction transaction) {
        String logMessage = "Node " + networkNode.getId() + ": I received a transaction from " + PublicAndPrivateKeyUtils.getAddress(transaction.getSenderPublicKey()) + "\n";
        logMessage = logMessage + "Number of inputs: " + transaction.getInputs().size() + "\n";
        logMessage = logMessage + "Number of outputs: " + transaction.getOutputs().size() + "\n";

        // update the GUI
        setChanged();
        notifyObservers(new UpdateMessage(UpdateType.TRANSACTION, transaction));

        // Before it is added in the unspent transactions set, it should be verified
        // This is done in two steps:
        // 1. Check the digital signature
        // 2. Check that all the inputs from the transaction are not already spent
        if (transaction.hasValidDigitalSignature()) {
            if (verifyTransactionInputs(unspentTransactions, transaction)) {
//                do not modify the UTXO set now; it is altered when a block on the blockchain arrives!
//                removeTransactionInputsFromUnspentSet(transaction.getInputs());
                logMessage = logMessage + "The transaction is accepted and it will be added in a block.";

                transactionsWithoutBlock.add(transaction);
                // If the transaction is accepted, stop the proof of work thread, wait for it,
                // add the new transaction and start it again
                if (proofOfWorkInstance.accept(transaction)) {
                    stopProofOfWorkThread();

                    proofOfWorkInstance.addTransaction(transaction);
                    startProofOfWorkThread();
                }
            } else {
                logMessage = logMessage + "The transaction has inputs that are already spend or are not addressed to the sender!";
            }
        } else {
            logMessage = logMessage + "The transaction does not have a valid digital signature!";
        }

        storeReceivedTransaction(transaction);
//        transactionsLogger.info(logMessage);
        System.out.println(logMessage);
    }

    protected void storeReceivedTransaction(Transaction transaction) {
    }

    protected void storeReceivedBlock(Block block) {
    }

    // todo: synchronize
    public void handleReceivedBlock(Block block) {
        String logMessage = "Node " + networkNode.getId() + ": I received a block! It was mined by node " + block.getMinerId() +
                ". Hash = " + block.hashCode() + ", height = " + block.getHeight() + ", transactions = " + block.getTransactions().size() + "\n";
        logMessage += "Previous block hash = " + block.getPreviousBlockHash() + ".\n";
        logMessage += "Last block in chain height = " + lastBlockInChain.getHeight() + "\n";

        // update the GUI
        setChanged();
        notifyObservers(new UpdateMessage(UpdateType.BLOCK, block));

        // validate the proof of work and check the signatures for the transactions in the block
        // there are two steps that are deferred - verifying that there is no double spend and that every
        // input record in a transaction is sent to the transaction's initiator
        // these last steps will be done when the block will be on the actual chain (now it is a leaf in a tree)
        if (PROOF_OF_WORK_VERIFIER.verify(block) && block.validateTransactionsInBlock()) {
            int previousId = block.getPreviousBlockHash();
            if (blockchain.containsKey(previousId)) {
                blockchain.put(block.hashCode(), block);

                Block parent = blockchain.get(previousId);
                parent.addChildren(block);

                // check if this is the next block after the lastBlockInChain
                if (block.getPreviousBlockHash() == lastBlockInChain.hashCode()) {
                    if (verifyTransactionRecordsInBlock(block, unspentTransactions)) {
                        lastBlockInChain = block;

                        logMessage += "Before update: The node has " + unspentTransactions.size() + " unspent transactions!\n";
                        updateUnspentTransactions(block, unspentTransactions);
                        logMessage += "After update: The node has " + unspentTransactions.size() + " unspent transactions!\n";

                        stopProofOfWorkThread();

                        updateTransactionsWithoutBlock(block, transactionsWithoutBlock);

                        startProofOfWorkThread();

                        logMessage += "The block with hash = " + block.hashCode() + " is right after the " +
                                "former lastBlockInChain. New block height = " + block.getHeight() + "\n";
                        logMessage += "The node has " + transactionsWithoutBlock.size() + " transactions witout block!\n";
                    } else {
                        logMessage += "The block with hash = " + block.hashCode() + " is right after the " +
                                "lastBlockInChain but is is not accepted. Block height = " + lastBlockInChain.getHeight() + "\n";
                    }
                }

                // check if there is an orphaned block that has the current block as predecessor and return
                // the child with the higher height
                block = addOrphanedBlocks(block);

                if (block.getHeight() > lastBlockInChain.getHeight()) {
                    stopProofOfWorkThread();

                    // change the lastBlockInChain to be the longest block
                    changeLastBlockInChain(block);

                    // create a new proofOfWorkInstance and start mining
                    startProofOfWorkThread();
                }

                // a new block was added in the tree (and maybe orphans blocks found their parents); update the GUI
                setChanged();
                notifyObservers(new UpdateMessage(UpdateType.BLOCKCHAIN, genesisBlock));

                // update the balance on the user interface
                double balance = getBalance();
                setChanged();
                notifyObservers(new UpdateMessage(UpdateType.BALANCE, balance));
            } else {
                logMessage += "This is an orphan block.\n";
                orphanBlocks.put(block.hashCode(), block);
            }
        } else {
            logMessage += "The block is not valid.\n";
        }
        System.out.println(logMessage);
//        transactionsLogger.info(logMessage);
        storeReceivedBlock(block);
    }

    /**
     * Stops the mining process, sending a signal to the thread and waits for it to exit completely from the loop.
     */
    protected void stopProofOfWorkThread() {
        proofOfWorkInstance.stop();

        try {
            proofOfWorkThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the transactions received in a mined block from the list that contains transactions that will be added in
     * a new block.
     */
    private void updateTransactionsWithoutBlock(Block block, List<Transaction> transactionsWithoutBlockList) {
        for (Transaction transaction : block.getTransactions()) {
            transactionsWithoutBlockList.remove(transaction);
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
     * when the block was received and added in the block tree. Also, remove the transactions from these blocks from
     * transactionsWithoutBlock list.
     *
     * If there is an attempt of double spending the initial state will be restored and the lastBlockInChain will
     * remain the same. The block where the double spending was discovered will be removed with all its successors.
     *
     * @param longerBlock the received block that is longer than the lastBlockInChain
     */
    private void changeLastBlockInChain(Block longerBlock) {
        boolean changed;
        changed = changeIfPossible(longerBlock);

        if (changed) {
            transactionsLogger.info("The chain was changed. The last block in chain has nonce = " + longerBlock.getNonce() +
                    ". New height = " + longerBlock.getHeight());
        } else {
            transactionsLogger.info("The attempt to change the chain failed! Last block in chain is still the one with" +
                    "nonce = " + lastBlockInChain.getNonce() + " and height = " + lastBlockInChain.getHeight());
        }
    }

    private boolean changeIfPossible(Block longerBlock) {
        boolean possible = true;
        Set<TransactionRecord> auxiliaryUnspentTransactions = new HashSet<>(unspentTransactions);
        List<Transaction> auxiliaryTransactionsWithoutBlock = new ArrayList<>(transactionsWithoutBlock);
        Block parent = lastBlockInChain;
        long commonAncestorHeight = findLowestCommonAncestor(longerBlock);

        // go to the common ancestor and for every transaction in a block, remove the outputs in the unspentTransactions
        // and add the inputs in unspentTransactions, because they are not spent anymore
        // also, add all the transactions in auxiliaryTransactionsWithoutBlock so that they can be mined and added in the blockchain
        long currentHeight = lastBlockInChain.getHeight();

        // do not include the common ancestor
        while (currentHeight > commonAncestorHeight) {
            for (Transaction transaction : parent.getTransactions()) {

                // remove the outputs because the transaction is not accepted by the blockchain, so this transaction record
                // should not be spent
                for (TransactionRecord output : transaction.getOutputs()) {
                    auxiliaryUnspentTransactions.remove(output);
                }

                // add the input in unspent transactions, so it can be spent in another transaction
                for (TransactionRecord input : transaction.getInputs()) {
                    auxiliaryUnspentTransactions.add(input);
                }

                auxiliaryTransactionsWithoutBlock.add(transaction);
            }

            parent = blockchain.get(parent.getPreviousBlockHash());
            currentHeight--;
        }

        List<Block> pathToNewBlock = getBlocksFromCommonAncestorToLongestBlock(longerBlock, commonAncestorHeight);

        for (Block block : pathToNewBlock) {
            if (acceptBlock(block, auxiliaryUnspentTransactions)) {
                updateUnspentTransactions(block, auxiliaryUnspentTransactions);
                updateTransactionsWithoutBlock(block, auxiliaryTransactionsWithoutBlock);
            } else {
                possible = false;
                break;
            }
        }

        // if the new path is accepted, change the unspentTransactions and the lastBlockInChain variables
        if (possible) {
            unspentTransactions = auxiliaryUnspentTransactions;
            transactionsWithoutBlock = auxiliaryTransactionsWithoutBlock;
            lastBlockInChain = longerBlock;
        }

        return possible;
    }

    /**
     * Check if the block complies the following rules:
     *     0) the block has a valid proof of work - somebody spent time before broadcasting it
     *     1) every transaction has a valid digital signature - this guarantees that they were not altered
     * by an intermediary node
     *     2) for every transaction check that all the inputs are sent to the transaction's author, so somebody
     * cannot use an input that is not addressed to him
     *     3) check that every input record from a transaction is not already spent
     *
     * @return true if the block is accepted
     */
    private boolean acceptBlock(Block block, Set<TransactionRecord> unspent) {
        boolean accepted = PROOF_OF_WORK_VERIFIER.verify(block);

        for (Transaction transaction : block.getTransactions()) {
            accepted &= transaction.hasValidDigitalSignature();
            accepted &= verifyTransactionInputs(unspent, transaction);
            if (!accepted) {
                break;
            }
        }

        return accepted;
    }

    private boolean verifyTransactionRecordsInBlock(Block block, Set<TransactionRecord> unspent) {
        boolean accepted = true;
        for (Transaction transaction : block.getTransactions()) {
            accepted &= verifyTransactionInputs(unspent, transaction);
        }
        return accepted;
    }

    /**
     * Remove all the input records from all the transactions from the unspent set and add all the outputs.
     */
    private void updateUnspentTransactions(Block block, Set<TransactionRecord> unspent) {
        for (Transaction transaction : block.getTransactions()) {
            for (TransactionRecord input : transaction.getInputs()) {
                unspent.remove(input);
            }

            for (TransactionRecord output : transaction.getOutputs()) {
                unspent.add(output);
            }
        }
    }

    /**
     * Computes the blocks from the ancestor to the newly found longest block.
     * @return the list of the blocks, starting with the one after the ancestor
     */
    private List<Block> getBlocksFromCommonAncestorToLongestBlock(Block longerBlock, long ancestorHeight) {
        List<Block> path = new ArrayList<>();
        long currentHeight = longerBlock.getHeight();

        while (currentHeight > ancestorHeight) {
            path.add(longerBlock);
            currentHeight--;
        }

        Collections.reverse(path);
        return path;
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
     * @return the height of the lowest common ancestor
     */
    private long findLowestCommonAncestor(Block longerBlock) {
        long difference = longerBlock.getHeight() - lastBlockInChain.getHeight();
        Block ancestor = longerBlock;
        Block auxiliary;

        while (difference != 0) {
            ancestor = blockchain.get(ancestor.getPreviousBlockHash());
            difference--;
        }

        auxiliary = lastBlockInChain;
        while (!ancestor.equals(auxiliary)) {
            ancestor = blockchain.get(ancestor.getPreviousBlockHash());
            auxiliary = blockchain.get(auxiliary.getPreviousBlockHash());
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
     */
    private Block addOrphanedBlocks(Block receivedBlock) {
        boolean orphanFound;
        Block child = null;
        do {
            orphanFound = false;
            synchronized (orphanBlocks) {
                for (Block block : orphanBlocks.values()) {
                    if (block.getPreviousBlockHash() == receivedBlock.hashCode()) {
                        orphanFound = true;
                        child = block;
                    }
                }
                if (orphanFound) {
                    orphanBlocks.remove(child.hashCode());
                    blockchain.put(child.hashCode(), child);
                    receivedBlock.addChildren(child);

                    receivedBlock = child;
                }
            }
        } while (orphanFound);
        return receivedBlock;
    }

    /**
     * Checks that every input record from a transaction is addressed to the sender (so he has the right to spend it)
     * and it is not already spent.
     *
     * Added a new parameter because this method will be called for both unspentTransactions and
     * auxiliaryUnspentTransactions, when a longer block arrives.
     *
     * @param transaction
     * @return
     */
    private boolean verifyTransactionInputs(Set<TransactionRecord> unspent, Transaction transaction) {
        boolean valid = true;
        for (TransactionRecord record : transaction.getInputs()) {
            if (!transaction.getSenderPublicKey().equals(record.getRecipient()) || !unspent.contains(record)) {
                System.out.println("Node " + networkNode.getId() + ": Probleme la tranzactia cu val " + record.getAmount() + " " + record.getRecipient());
                System.out.println("In hash am " + unspent.size() + " tranzactii.");
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

    public Map<Integer, Block> getBlockchain() {
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

    public Map<Integer, Block> getOrphanBlocks() {
        return orphanBlocks;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public Node getNetworkNode() {
        return networkNode;
    }

    public String getAddress() {
        return PublicAndPrivateKeyUtils.getAddress(publicKey);
    }
}
