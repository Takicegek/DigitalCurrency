import currency.Client;
import currency.Transaction;
import currency.TransactionRecord;
import currency.utils.PublicKeyUtils;

import java.io.IOException;
import java.security.*;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException, SignatureException, NoSuchAlgorithmException,
            InvalidKeyException, IOException {
        Client client = new Client("localhost", 10000);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        KeyPair pair = generator.generateKeyPair();
        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();

        String recipient = "99231423279787562647082013729631822505007159088237122818737338760181625690169588766193553753561070712374766764319679286166312799411997761721584811680345774163566785206244089527117143220881703277513639877689092604323652079791926600968650756949059701910958988037434099515886658357479622237687497281756983433107#65537";
        PublicKey recipientPublicKey = PublicKeyUtils.getPublicKey(recipient);

        Transaction transaction = Transaction.Builder.getBuilder()
                .withClientBalance(10)
                .withUnspentTransactions(client.getUnspentTransactions())
                .withPublicKey(publicKey)
                .withPrivateKey(privateKey)
                .withRecipient(recipientPublicKey, 5)
                .build();

    //    client.broadcastTransaction(transaction);
    }
}
