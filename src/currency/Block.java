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

    public Block(Block previousBlock, long height) {
        this.previousBlock = previousBlock;
        this.height = height;
        transactions = new ArrayList<>();
    }
}
