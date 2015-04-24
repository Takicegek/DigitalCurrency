import currency.Client;
import currency.Transaction;
import currency.utils.PublicKeyUtils;

import java.io.IOException;
import java.security.*;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException, SignatureException, NoSuchAlgorithmException,
            InvalidKeyException, IOException {
        Client client = new Client("localhost", 10003);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        KeyPair pair = generator.generateKeyPair();
        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();

        String recipient = "614835079256879953188043280898648387641949603197259025854664994939634042491754682163221681091871240723898208802409859861863300871246508699768039881843091519063623717769037589774908067637223044201466913381182793707842104636081309928138071514989785509033197980746581866814529321230486948064403347063#65537";
        PublicKey recipientPublicKey = PublicKeyUtils.getPublicKey(recipient);

        Transaction transaction = Transaction.Builder.getBuilder()
                .withClientBalance(10)
                .withUnspentTransactions(client.getUnspentTransactions())
                .withPublicKey(publicKey)
                .withPrivateKey(privateKey)
                .withRecipient(recipientPublicKey, 5)
                .build();

        client.broadcastTransaction(transaction);
    }
}
