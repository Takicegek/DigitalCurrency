package currency;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * When a new client joins, he asks the successor for its state, which is represented by the unspent transactions,
 * the blockchain, the orphan blocks, the transactions which are not embedded in a block and the block that
 * is the last in the chain.
 *
 * This class wraps both.
 *
 * Created by Sorin Nutu on 4/20/2015.
 */
public class BlockchainAndTransactionsWrapper implements Serializable {
    private Set<TransactionRecord> unspentTransactions;
    private Map<Long, Block> blockchain;
    private Map<Long, Block> orphanBlocks;
    private List<Transaction> transactionsWithoutblock;
    private Block lastBlockInChain;

    public BlockchainAndTransactionsWrapper(Set<TransactionRecord> unspentTransactions,
                                            Map<Long, Block> blockchain,
                                            Map<Long, Block> orphanBlocks,
                                            List<Transaction> transactionsWithoutblock,
                                            Block lastBlockInChain) {
        this.unspentTransactions = unspentTransactions;
        this.blockchain = blockchain;
        this.orphanBlocks = orphanBlocks;
        this.transactionsWithoutblock = transactionsWithoutblock;
        this.lastBlockInChain = lastBlockInChain;
    }

    public Block getLastBlockInChain() {
        return lastBlockInChain;
    }

    public Set<TransactionRecord> getUnspentTransactions() {
        return unspentTransactions;
    }

    public Map<Long, Block> getBlockchain() {
        return blockchain;
    }

    public Map<Long, Block> getOrphanBlocks() {
        return orphanBlocks;
    }

    public List<Transaction> getTransactionsWithoutblock() {
        return transactionsWithoutblock;
    }
}
