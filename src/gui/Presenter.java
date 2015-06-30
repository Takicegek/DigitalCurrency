package gui;

import currency.Block;
import currency.Client;
import currency.Transaction;
import currency.utils.PublicAndPrivateKeyUtils;
import gui.treelayout.BlockExtentProvider;
import gui.treelayout.TreePane;
import org.abego.treelayout.Configuration;
import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.DefaultConfiguration;
import org.abego.treelayout.util.DefaultTreeForTreeLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * The presenter implements the logic for the GUI, as described in the Model-View-Presenter design pattern.
 *
 * It handles requests from the View (such as broadcast a new transaction) and sends them to the Client and receives
 * notification from the Client and updates the View.
 *
 * The View knows nothing about the Client and neither the Client about the View.
 * The Observer pattern is used so the Client can notify the Presenter about internal changes. When it is notified,
 * the presenter fetches the news and prints them on the View.
 *
 * The Model, represented by the Client, and the View are decoupled and the Presenter facilitates the communication
 * between them.
 *
 * Created by Sorin Nutu on 6/13/2015.
 */
public class Presenter implements Observer, ActionListener {

    private Client client;
    private View view;
    private Block rootBlock;

    public Presenter(String ip, int port, String bootstrapIp, int bootstrapPort) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        client = new Client(ip, port, bootstrapIp, bootstrapPort);
        client.connectToNetwork();
        client.addObserver(this);

        view = new View();
        view.setAddressTextField(client.getAddress());
        view.updateBalance(client.getBalance());
        view.setSendButtonActionListener(this);

        rootBlock = client.getGenesisBlock();
        updateBlockchain();
    }

    @Override
    public void update(Observable o, Object arg) {
        UpdateMessage message = (UpdateMessage) arg;
        switch (message.getUpdateType()) {
            case TRANSACTION:
                Transaction transaction = (Transaction) message.getData();
                String desc = "Transaction with " + transaction.getInputs().size() + " inputs and " + transaction.getOutputs().size() + " outputs\n";
                view.appendReceivedTransaction(desc);
                break;
            case BLOCK:
                Block block = (Block) message.getData();
                desc = "Block " + block.hashCode() + " mined by node " + block.getMinerId() + " with " + block.getTransactions().size() + " transactions.\n";
                view.appendReceivedBlock(desc);
                break;
            case BLOCKCHAIN:
                updateBlockchain();
                break;
            case BALANCE:
                Double balance = (Double) message.getData();
                view.updateBalance(balance);
                break;
            case INFO:
                String details = (String) message.getData();
                view.appendDetails(details);
        }
    }

    private void updateBlockchain() {
        DefaultTreeForTreeLayout<Block> treeForTreeLayout = new DefaultTreeForTreeLayout<>(rootBlock);
        addEdgesDepthFirst(treeForTreeLayout, rootBlock);

        TreeLayout<Block> layout = new TreeLayout<>(treeForTreeLayout, new BlockExtentProvider(),
                new DefaultConfiguration<Block>(25, 10, Configuration.Location.Left));

        // put the tree layout on a swing container that knows how to paint the tree
        TreePane pane = new TreePane(layout);

        view.updateBlockchain(pane);
    }

    /**
     * Traverse the tree in depth and add every edge in the tree layout.
     * @param treeLayout
     * @param root
     */
    private void addEdgesDepthFirst(DefaultTreeForTreeLayout<Block> treeLayout, Block root) {
        for (Block child : root.getChildren()) {
            treeLayout.addChild(root, child);
            addEdgesDepthFirst(treeLayout, child);
        }
    }

    public Client getClient() {
        return client;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // retrieve data
        List<PublicKey> keys = new ArrayList<>();
        List<Double> amounts = new ArrayList<>();

        try {
            for (JTextField addressField : view.getRecipientsAdresses()) {
                // some addresses may be empty
                if (!addressField.getText().equals("")) {
                    System.out.println("Address = " + addressField.getText());
                    String address = addressField.getText();
                    PublicKey key = PublicAndPrivateKeyUtils.getPublicKey(address);
                    keys.add(key);
                }
            }
        } catch (Exception e1) {
            view.setMessageLabel("The address cannot be transformed into a public key!");
            return;
        }

        try {
            for (JTextField amountField : view.getAmounts()) {
                String amountString = amountField.getText();
                if (!amountString.equals("")) {
                    double amount = Double.parseDouble(amountString);
                    amounts.add(amount);
                }
            }
        } catch (NumberFormatException e1) {
            view.setMessageLabel("The amount cannot be transformed into a number!");
            return;
        }

        // create a new transaction
        Transaction.Builder builder = Transaction.Builder.getBuilder()
                .withPrivateKey(client.getPrivateKey())
                .withPublicKey(client.getPublicKey())
                .withUnspentTransactions(client.getUnspentTransactions())
                .withClientBalance(client.getBalance());

        try {
            for (int i = 0; i < keys.size(); i++) {
                builder.withRecipient(keys.get(i), amounts.get(i));
            }

            Transaction transaction = builder.build();
            client.broadcastTransaction(transaction);

            view.setMessageLabel("The transaction was successfully sent!");
        } catch (IllegalArgumentException e1) {
            view.setMessageLabel(e1.getMessage());
        } catch (Exception e1) {
            view.setMessageLabel("An error encountered. Please check that the addresses are correct!");
        }

    }

    public static void main(String[] args) {
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String bootstrapIp = args[2];
        int bootstrapPort = Integer.parseInt(args[3]);

        new Presenter(ip, port, bootstrapIp, bootstrapPort);
    }


}
