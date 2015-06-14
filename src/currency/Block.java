package currency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A block in the blockchain. It is identified by its nonce.
 * Created by Sorin Nutu on 4/19/2015.
 */
public class Block implements Serializable {
    private List<Transaction> transactions;
    private int previousBlockHash;
    private long previousBlockNonce;
    private long nonce;
    private long height;
    private long minerId;
    // helpful for plotting; this should not be included in the hash
    private List<Block> children;

    public Block(int previousBlockHash, long previousBlockNonce, long previousBlockHeight, long minerId) {
        this.previousBlockHash = previousBlockHash;
        this.previousBlockNonce = previousBlockNonce;
        this.transactions = new ArrayList<>();
        this.height = previousBlockHeight + 1;
        this.minerId = minerId;
        children = new ArrayList<>();
    }

    private Block() {
        transactions = new ArrayList<>();
        children = new ArrayList<>();
        height = 0;
        nonce = 0;
    }

    public static Block createGenesisBlock() {
        return new Block();
    }

    public long getNonce() {
        return nonce;
    }

    public long getPreviousBlockNonce() {
        return previousBlockNonce;
    }

    public void incrementNonce() {
        nonce++;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public int transactionCount() {
        return transactions.size();
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public long getHeight() {
        return height;
    }

    public int getPreviousBlockHash() {
        return previousBlockHash;
    }

    public boolean validateTransactionsInBlock() {
        boolean validBlock = true;
        for (int i = 0; i < transactions.size() && validBlock; i++) {
            validBlock = validBlock & transactions.get(i).hasValidDigitalSignature();
        }
        return validBlock;
    }

    public void addChildren(Block block) {
        children.add(block);
    }

    public List<Block> getChildren() {
        return children;
    }

    public long getMinerId() {
        return minerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Block block = (Block) o;

        if (height != block.height) return false;
        if (minerId != block.minerId) return false;
        if (nonce != block.nonce) return false;
        if (previousBlockHash != block.previousBlockHash) return false;
        if (transactions != null ? !transactions.equals(block.transactions) : block.transactions != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = transactions != null ? transactions.hashCode() : 0;
        result = 31 * result + previousBlockHash;
        result = 31 * result + (int) (previousBlockNonce ^ (previousBlockNonce >>> 32));
        result = 31 * result + (int) (nonce ^ (nonce >>> 32));
        result = 31 * result + (int) (height ^ (height >>> 32));
        result = 31 * result + (int) (minerId ^ (minerId >>> 32));
        return result;
    }

    public String stringForHash() {
        return nonce + " " + height + " " + minerId + " " + previousBlockHash + " " + transactions.toString();
    }

    @Override
    public String toString() {
        return "Block{" +
                "transactions=" + transactions +
                ", previousBlockHash=" + previousBlockHash +
                ", minerId=" + minerId +
                ", nonce=" + nonce +
                ", height=" + height +
                '}';
    }
}
