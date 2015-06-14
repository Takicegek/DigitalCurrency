package gui;

import currency.Block;
import currency.Client;
import currency.Transaction;
import gui.treelayout.BlockExtentProvider;
import gui.treelayout.TreePane;
import org.abego.treelayout.Configuration;
import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.DefaultConfiguration;
import org.abego.treelayout.util.DefaultTreeForTreeLayout;

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
public class Presenter implements Observer {

    private Client client;
    private View view;

    public Presenter(String ip, int port) {
        client = new Client(ip, port);
        client.connectToNetwork();
        client.addObserver(this);

        view = new View();
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
                desc = "Block mined by node " + block.getMinerId() + " with " + block.getTransactions().size() + " transactions.";
                view.appendReceivedBlock(desc);
                break;
            case BLOCKCHAIN:
                Block root = (Block) message.getData();

                DefaultTreeForTreeLayout<Block> treeForTreeLayout = new DefaultTreeForTreeLayout<>(root);
                addEdgesDepthFirst(treeForTreeLayout, root);

                TreeLayout<Block> layout = new TreeLayout<>(treeForTreeLayout, new BlockExtentProvider(),
                        new DefaultConfiguration<Block>(25, 10, Configuration.Location.Left));

                // put the tree layout on a swing container that knows how to paint the tree
                TreePane pane = new TreePane(layout);

                view.changeBlockchain(pane);
        }
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
}
