package currency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Represents a transaction that is initiated by a client to transfer a certain amount of money.
 * Created by Sorin Nutu on 4/19/2015.
 */
public class Transaction implements Serializable {
    private long id;
    private List<TransactionRecord> inputs;
    private List<TransactionRecord> outputs;
    private long signature;
    private long sendersPublicKey;

    public Transaction(List<TransactionRecord> inputs, List<TransactionRecord> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public static class Builder {
        private long id;
        private List<TransactionRecord> inputs;
        private List<TransactionRecord> outputs;
        private long signature;
        private long sendersPublicKey;
        private Set<TransactionRecord> unspentTransactions;
        private double clientBalance;
        private double totalSpentAmount;

        public static Builder getBuilder() {
            return new Builder();
        }

        public Builder() {
            inputs = new ArrayList<>();
            outputs = new ArrayList<>();
        }

        public Builder withUnspentTransactions(Set<TransactionRecord> unspentTransactions) {
            this.unspentTransactions = unspentTransactions;
            return this;
        }

        public Builder withClientBalance(double clientBalance) {
            this.clientBalance = clientBalance;
            return this;
        }

        public Builder withClientPublicKey(long sendersPublicKey) {
            this.sendersPublicKey = sendersPublicKey;
            return this;
        }

        public Builder withRecipient(long recipientPublicKey, double amount) {
            if (totalSpentAmount + amount > clientBalance) {
                throw new IllegalArgumentException("The balance is smaller than the amount to be sent.");
            }
            totalSpentAmount += amount;


            TransactionRecord record = new TransactionRecord(sendersPublicKey, recipientPublicKey, amount);
            outputs.add(record);

            return this;
        }

        /**
         * Build the actual transaction.
         * The output TransactionRecords will be copied from builder, but the inputs will be computed. The inputs
         * list will contain the first TransactionRecords which are sent to the client and have the sum greater than
         * totalSpentAmount. Every record that is added as an input to this transaction is removed from the
         * unspentTransactions list.
         *
         * After that, the digital signature should be added.
         * @return
         */
        public Transaction build() {
            double spentAmount = 0;

            Iterator<TransactionRecord> iterator = unspentTransactions.iterator();

            while (iterator.hasNext()) {
                TransactionRecord record = iterator.next();
                if (record.getRecipient() == sendersPublicKey) {
                    spentAmount += record.getAmount();
                    inputs.add(record);
                    iterator.remove();
                }
                if (spentAmount >= totalSpentAmount) {
                    break;
                }
            }

            // send the change back
            if (spentAmount > totalSpentAmount) {
                TransactionRecord change = new TransactionRecord(sendersPublicKey, sendersPublicKey, spentAmount - totalSpentAmount);
                outputs.add(change);
            }

            // todo add the digital signature

            return new Transaction(inputs, outputs);
        }
    }
}
