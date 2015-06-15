package currency;

import currency.utils.PublicAndPrivateKeyUtils;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an input or an output in a transaction.
 * Created by Sorin Nutu on 4/19/2015.
 */
public class TransactionRecord implements Serializable {
    private static final AtomicLong ID_CREATOR = new AtomicLong(0);

    private long id;
    private PublicKey sender;
    private PublicKey recipient;
    private double amount;

    public TransactionRecord(PublicKey sender, PublicKey recipient, double amount) {
        this.id = ID_CREATOR.getAndIncrement();
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
        if (id != record.id) return false;
        if (recipient != null ? !recipient.equals(record.recipient) : record.recipient != null) return false;
        if (sender != null ? !sender.equals(record.sender) : record.sender != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (id ^ (id >>> 32));
        result = 31 * result + (sender != null ? sender.hashCode() : 0);
        result = 31 * result + (recipient != null ? recipient.hashCode() : 0);
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
                "id=" + id +
                ", sender=" + sender +
                ", recipient=" + recipient +
                ", amount=" + amount +
                '}';
    }
}
