package currency;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.*;
import java.util.*;

/**
 * Represents a transaction that is initiated by a client to transfer a certain amount of money.
 * Created by Sorin Nutu on 4/19/2015.
 */
public class Transaction implements Serializable {
    private long id;
    private List<TransactionRecord> inputs;
    private List<TransactionRecord> outputs;
    private byte[] signature;
    private PublicKey senderPublicKey;

    public Transaction(List<TransactionRecord> inputs, List<TransactionRecord> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public List<TransactionRecord> getInputs() {
        return inputs;
    }

    public List<TransactionRecord> getOutputs() {
        return outputs;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public void setSenderPublicKey(PublicKey senderPublicKey) {
        this.senderPublicKey = senderPublicKey;
    }

    public PublicKey getSenderPublicKey() {
        return senderPublicKey;
    }

    /**
     * Verifies the digital signature of this transaction using the sender's public key.
     * The signature was computed when it was first created using the sender's private key.
     * @return true if the digital signature is valid
     */
    public boolean hasValidDigitalSignature() {
        boolean validSignature = false;
        try {
            byte[] data = transformToByteArray(this);
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(senderPublicKey);
            sig.update(data);

            validSignature = sig.verify(signature);
        } catch (Exception e) {
            e.printStackTrace();
            validSignature = false;
        }
        return validSignature;
    }

    public static class Builder {
        private long id;
        private List<TransactionRecord> inputs;
        private List<TransactionRecord> outputs;
        private Set<TransactionRecord> unspentTransactions;
        private double clientBalance;
        private double totalSpentAmount;
        private PublicKey senderPublicKey;
        private PrivateKey senderPrivateKey;
        private PrivateKey receiverPublicKey;

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

        public Builder withPrivateKey(PrivateKey senderPrivateKey) {
            this.senderPrivateKey = senderPrivateKey;
            return this;
        }

        public Builder withClientBalance(double clientBalance) {
            this.clientBalance = clientBalance;
            return this;
        }

        public Builder withPublicKey(PublicKey senderPublicKey) {
            this.senderPublicKey = senderPublicKey;
            return this;
        }

        public Builder withRecipient(PublicKey recipientPublicKey, double amount) {
            if (totalSpentAmount + amount > clientBalance) {
                throw new IllegalArgumentException("The balance is smaller than the amount to be sent.");
            }
            totalSpentAmount += amount;


            TransactionRecord record = new TransactionRecord(senderPublicKey, recipientPublicKey, amount);
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
         * After that, the digital signature is added.
         * @return
         */
        public Transaction build() throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
            double spentAmount = 0;

            Iterator<TransactionRecord> iterator = unspentTransactions.iterator();

            while (iterator.hasNext()) {
                TransactionRecord record = iterator.next();
                if (record.getRecipient().equals(senderPublicKey)) {
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
                TransactionRecord change = new TransactionRecord(senderPublicKey, senderPublicKey, spentAmount - totalSpentAmount);
                outputs.add(change);
            }

            Transaction transaction = new Transaction(inputs, outputs);

            // create the signature and attach it to the transaction, along with the sender's public key
            transaction.setSenderPublicKey(senderPublicKey);
            byte[] signature = computeDigitalSignature(transaction);
            transaction.setSignature(signature);

            return transaction;
        }

        private byte[] computeDigitalSignature(Transaction transaction) throws NoSuchAlgorithmException,
                InvalidKeyException, SignatureException, IOException {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(senderPrivateKey);

            byte[] data = transformToByteArray(transaction);
            sig.update(data);

            return sig.sign();
        }

    }

    private static byte[] transformToByteArray(Transaction transaction) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);

        outputStream.writeObject(transaction.toString());

        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transaction that = (Transaction) o;

        if (id != that.id) return false;
        if (inputs != null ? !inputs.equals(that.inputs) : that.inputs != null) return false;
        if (outputs != null ? !outputs.equals(that.outputs) : that.outputs != null) return false;
        if (senderPublicKey != null ? !senderPublicKey.equals(that.senderPublicKey) : that.senderPublicKey != null)
            return false;
        if (!Arrays.equals(signature, that.signature)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = inputs != null ? inputs.hashCode() : 0;
        result = 31 * result + (outputs != null ? outputs.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "senderPublicKey=" + senderPublicKey +
                ", outputs=" + outputs +
                ", inputs=" + inputs +
                '}';
    }
}
