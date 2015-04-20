package currency;

import java.io.Serializable;
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

    public BlockchainAndTransactionsWrapper(Set<TransactionRecord> unspentTransactions, Set<Block> blockchain) {
        this.unspentTransactions = unspentTransactions;
        this.blockchain = blockchain;
    }

    public Set<TransactionRecord> getUnspentTransactions() {
        return unspentTransactions;
    }

    public Set<Block> getBlockchain() {
        return blockchain;
    }
}
