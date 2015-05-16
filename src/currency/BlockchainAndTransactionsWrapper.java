package currency;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * When a new client joins, he asks the successor for the entire block chain and the unspent transactions.
 *
 * This class wraps both.
 *
 * Created by Sorin Nutu on 4/20/2015.
 */
public class BlockchainAndTransactionsWrapper implements Serializable {
    private Set<TransactionRecord> unspentTransactions;
    private Set<Block> blockchain;
    private List<Transaction> transactionsWithoutblock;

    public BlockchainAndTransactionsWrapper(Set<TransactionRecord> unspentTransactions,
                                            Set<Block> blockchain,
                                            List<Transaction> transactionsWithoutblock) {
        this.unspentTransactions = unspentTransactions;
        this.blockchain = blockchain;
        this.transactionsWithoutblock = transactionsWithoutblock;
    }

    public Set<TransactionRecord> getUnspentTransactions() {
        return unspentTransactions;
    }

    public Set<Block> getBlockchain() {
        return blockchain;
    }

    public List<Transaction> getTransactionsWithoutblock() {
        return transactionsWithoutblock;
    }
}
