package currency;

/**
 * Interface for all proof of work implementations.
 * A concrete implementation will store the current transactions included in block and it will solve
 * a hard problem to prove that it spent resources before broadcasting a block. If there is a
 * fake transaction in the list, the entire block will be rejected and the work done will not have any
 * result.
 * Created by Sorin Nutu on 5/8/2015.
 */
public interface ProofOfWork extends Runnable {
    /**
     * Start from the beginning to mine the candidate transitions for this block.
     * @return the block, with the final list of transactions and the nonce
     */
    void mine();

    /**
     * Decides if the given transaction will be accepted in this block. This implies that the computation
     * will start from the beginning.
     * @param transaction
     * @return
     */
    boolean accept(Transaction transaction);

    /**
     * Adds the transaction in the list. Before that, a call to accept() is made. If the transaction
     * is not accepted, the method returns false.
     * @param transaction
     * @return
     */
    boolean addTransaction(Transaction transaction);

    /**
     * Stops the current try because of an external event (a block with a higher height has arrived).
     */
    void stop();

    /**
     * Tests if a block has a correct value for the nonce field and the hard problem is solved.
     * @return
     */
    boolean verify(Block block);
}
