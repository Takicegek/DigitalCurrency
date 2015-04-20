package currency;

import java.io.Serializable;

/**
 * Represents an input or an output in a transaction.
 * Created by Sorin Nutu on 4/19/2015.
 */
public class TransactionRecord implements Serializable {
    private long sender;
    private long recipient;
    private double amount;

    public TransactionRecord(long sender, long recipient, double amount) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
    }

    public long getSender() {
        return sender;
    }

    public long getRecipient() {
        return recipient;
    }

    public double getAmount() {
        return amount;
    }
}
