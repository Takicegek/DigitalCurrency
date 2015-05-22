package currency;

import network.Node;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sorin Nutu on 5/8/2015.
 */
public class HashProofOfWork implements ProofOfWork {
    private List<Transaction> candidateTransactions;
    private Block previousBlock;
    private Node networkNode;
    private Client client;
    private boolean externalStop;
    private int limit = 5;

    public HashProofOfWork(Client client, Node networkNode, Block previousBlock) {
        this.networkNode = networkNode;
        this.client = client;
        candidateTransactions = new ArrayList<>();
        this.previousBlock = previousBlock;
    }

    @Override
    public void mine() {
        boolean stop = false;
        externalStop = false;
        Block block = new Block(previousBlock.hashCode(), previousBlock.getNonce(), previousBlock.getHeight(), candidateTransactions);
        while (!stop && !externalStop) {
            if (verify(block)) {
                stop = true;
            } else {
                block.incrementNonce();
            }
        }

        if (!externalStop) {
            byte[] hash = hashCodeForBlock(block);
            System.out.println(hash.length);
            System.out.println("Am gasit valoarea pt nonce!! Nonce = " + block.getNonce() + ", hash = " + (int)hash[0] + " " + (int)hash[1] + " " + (int)hash[2] + " " + (int)hash[3]);

            // broadcast the block
            networkNode.broadcastBlock(block);

            // remove the transactions embedded in this block from candidate transactions
            client.removeFromCandidateTransactions(candidateTransactions);

            prepareMining(block);
        }
    }

    /**
     * Repopulates the candidate transactions, changes the previous block and starts to find a new
     * proof of work.
     */
    private void prepareMining(Block previous) {
        previousBlock = previous;
        candidateTransactions.clear();

        List<Transaction> transactionsWithoutBlock = client.getTransactionsWithoutBlock();
        for (int i = 0; i < transactionsWithoutBlock.size() && i < limit; i++) {
            candidateTransactions.add(transactionsWithoutBlock.get(i));
        }

        System.out.println("I will start the mining process again!");
        mine();
    }

    @Override
    public boolean accept(Transaction transaction) {
        return candidateTransactions.size() < limit;
    }

    @Override
    public boolean addTransaction(Transaction transaction) {
        if (accept(transaction)) {
            candidateTransactions.add(transaction);
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
        return (hash[0] == 0 && hash[1] == 0 && hash[2] >= 0 && hash[2] <= 10);
    }

    private byte[] hashCodeForBlock(Block block) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(block.stringForHash().getBytes());
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new String("12345").getBytes();
    }

    @Override
    public void stop() {
        externalStop = true;
        System.out.println("External Stop encountered!");
    }

    @Override
    public void run() {
        mine();
    }
}
