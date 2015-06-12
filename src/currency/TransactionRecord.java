package currency;

import currency.utils.PublicAndPrivateKeyUtils;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Represents an input or an output in a transaction.
 * Created by Sorin Nutu on 4/19/2015.
 */
public class TransactionRecord implements Serializable {
    private PublicKey sender;
    private PublicKey recipient;
    private double amount;

    public TransactionRecord(PublicKey sender, PublicKey recipient, double amount) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
    }

    public PublicKey getSender() {
        return sender;
    }

    public PublicKey getRecipient() {
        return recipient;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransactionRecord record = (TransactionRecord) o;

        if (Double.compare(record.amount, amount) != 0) return false;
        if (!recipient.equals(record.recipient)) return false;
        if (!sender.equals(record.sender)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = sender != null ? sender.hashCode() : 0;
        result = 31 * result + (recipient != null ? recipient.hashCode() : 0);
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
                "sender=" + PublicAndPrivateKeyUtils.getAddress(sender) +
                ", recipient=" + PublicAndPrivateKeyUtils.getAddress(recipient) +
                ", amount=" + amount +
                '}';
    }
}
