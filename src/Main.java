import currency.Client;
import currency.Transaction;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Client client = new Client("localhost", 10006);

        Transaction transaction = Transaction.Builder.getBuilder()
                .withClientBalance(10)
                .withClientPublicKey(1989)
                .withUnspentTransactions(client.getUnspentTransactions())
                .withRecipient(123123, 1.4)
                .withRecipient(123, 6.6)
                .withRecipient(124, 1)
                .build();

        client.broadcastTransaction(transaction);
    }
}
