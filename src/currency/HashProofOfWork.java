package currency;

import gui.UpdateMessage;
import gui.UpdateType;
import network.Node;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by Sorin Nutu on 5/8/2015.
 */
public class HashProofOfWork implements ProofOfWork {
    private Block currentBlock;
    private Node networkNode;
    private Client client;
    private volatile boolean externalStop;
    private int limit = 5;

    public HashProofOfWork(Client client) {
        if (client != null) {
            this.networkNode = client.getNetworkNode();
        }
        this.client = client;
    }

    @Override
    public void mine() {
        boolean stop = false;
        externalStop = false;

        client.setChanged();
        client.notifyObservers(new UpdateMessage(UpdateType.INFO, "Start to create a new block with " + currentBlock.transactionCount() + " transactions\n"));
        System.out.println("Node " + networkNode.getId() + ": Started mining a block with " + currentBlock.transactionCount() + " transactions.\n");
        System.out.println("Node " + networkNode.getId() + ": I have " + client.getTransactionsWithoutBlock().size() + " transactions without block!!!\n");
        while (!stop && !externalStop) {
            if (verify(currentBlock)) {
                stop = true;
            } else {
                currentBlock.incrementNonce();
                if (currentBlock.getNonce() % 5000 == 0) {
                    System.out.println("Nodul " + networkNode.getId() + ": nonce = " + currentBlock.getNonce());
                    client.setChanged();
                    client.notifyObservers(new UpdateMessage(UpdateType.INFO, "Searching for nonce. Current value = " + currentBlock.getNonce() + "\n"));
                }
            }
        }

        if (!externalStop) {
            System.out.println("Nodul " + networkNode.getId() + ": Am gasit valoarea pt nonce!! Nonce = " + currentBlock.getNonce() + ", hash = " + currentBlock.hashCode());
            System.out.println("Blockul are " + currentBlock.getTransactions().size() + " tranzactii.");
            byte[] hash = hashCodeForBlock(currentBlock);
            System.out.println("Hash = " + hash[0] + " " + hash[1] + " " + hash[2] + " " + hash[3]);
            System.out.println(currentBlock.stringForHash());

            // broadcast the block
            networkNode.broadcastBlock(currentBlock);
            System.out.println("Nodul " + networkNode.getId() + ": Am facut broadcast la un block cu tranzactii = " + currentBlock.getTransactions().size());

            String message = "The proof of work was solved for the block " + currentBlock.hashCode() + "\n";
            message += "The block contains " + currentBlock.transactionCount() + " transactions and it has been broadcasted.\n";

            client.setChanged();
            client.notifyObservers(new UpdateMessage(UpdateType.INFO, message));

//            do not continue to mine; the process will start again when the node handles the block
//            prepareMining();
        }
    }

    /**
     * Repopulates the candidate transactions, changes the previous block and starts to find a new
     * proof of work.
     */
    private void prepareMining() {
        Block previousBlock = client.getLastBlockInChain();
        currentBlock = new Block(previousBlock.hashCode(), previousBlock.getNonce(),
                previousBlock.getHeight(), networkNode.getId());
        List<Transaction> transactionsWithoutBlock = client.getTransactionsWithoutBlock();
        System.out.println("Nodul " + networkNode.getId() + ": Am " + transactionsWithoutBlock.size() + " tranzactii fara block!");
        for (int i = 0; i < transactionsWithoutBlock.size() && currentBlock.getTransactions().size() < limit; i++) {
            addTransaction(transactionsWithoutBlock.get(i));
        }

        System.out.println("I will start the mining process again if there are enough transactions!");
        if (currentBlock.transactionCount() > 0) {
            mine();
        }
    }

    /**
     * Accept a transaction only if there is enough space in the current block and it does not have inputs
     * that are already used by a transaction in the block.
     * @param transaction
     * @return
     */
    @Override
    public boolean accept(Transaction transaction) {
        boolean accepted = (currentBlock.transactionCount() < limit);
        if (accepted) {
            for (Transaction transactionFromBlock : currentBlock.getTransactions()) {
                for (TransactionRecord record : transactionFromBlock.getInputs()) {
                    if (transaction.getInputs().contains(record)) {
                        accepted = false;
                    }
                }
            }
        }
        return accepted;
    }

    @Override
    public boolean addTransaction(Transaction transaction) {
        if (accept(transaction)) {
            currentBlock.addTransaction(transaction);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Computes the hash for the block and checks if the first three bytes are 0.
     * @param block
     * @return
     */
    @Override
    public boolean verify(Block block) {
        byte[] hash = hashCodeForBlock(block);
        return (hash[0] == 0 && hash[1] == 0);
    }

    protected byte[] hashCodeForBlock(Block block) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            // the string generated by stringForHash method is large when there are many transactions, so hash it before sha256
//            messageDigest.update(("" + block.stringForHash().hashCode()).getBytes());
            String p = block.stringForHash();
            messageDigest.update(p.substring(0, 10).getBytes());
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "".getBytes();
        }
    }

    @Override
    public void stop() {
        externalStop = true;
        System.out.println("External Stop encountered!");
        client.setChanged();
        client.notifyObservers(new UpdateMessage(UpdateType.INFO, "The mining process was interrupted by an external event.\n"));
    }

    @Override
    public void run() {
        prepareMining();
    }
}
