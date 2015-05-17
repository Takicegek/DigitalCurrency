package currency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A block in the blockchain.
 * Created by Sorin Nutu on 4/19/2015.
 */
public class Block implements Serializable {
    private List<Transaction> transactions;
    private Block previousBlock;
    private long nonce;
    private long height;

    public Block(Block previousBlock, List<Transaction> transactions) {
        this.previousBlock = previousBlock;
        this.transactions = transactions;
        this.height = previousBlock.getHeight();
    }

    public Block(Block previousBlock) {
        this.previousBlock = previousBlock;
        this.height = previousBlock.getHeight() + 1;
        transactions = new ArrayList<>();
    }

    private Block() {
        transactions = new ArrayList<>();
        height = 0;
    }

    public static Block createGenesisBlock() {
        return new Block();
    }

    public long getNonce() {
        return nonce;
    }

    public void incrementNonce() {
        nonce++;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public long getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Block block = (Block) o;

        if (height != block.height) return false;
        if (nonce != block.nonce) return false;
        if (previousBlock != null ? !previousBlock.equals(block.previousBlock) : block.previousBlock != null)
            return false;
        if (transactions != null ? !transactions.equals(block.transactions) : block.transactions != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = transactions != null ? transactions.hashCode() : 0;
        result = 31 * result + (previousBlock != null ? previousBlock.hashCode() : 0);
        result = 31 * result + (int) (nonce ^ (nonce >>> 32));
        result = 31 * result + (int) (height ^ (height >>> 32));
        return result;
    }

    public String stringForHash() {
        if (previousBlock == null) {
            return nonce + " " + height + " " + transactions.toString();
        }
        return nonce + " " + height + " " + previousBlock.toString() + " " + transactions.toString();
    }

    @Override
    public String toString() {
        return "Block{" +
                "transactions=" + transactions +
                ", previousBlock=" + previousBlock +
                ", nonce=" + nonce +
                ", height=" + height +
                '}';
    }
}
