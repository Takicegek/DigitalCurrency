package gui;

import currency.Client;
import currency.Transaction;

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
        System.err.println("HOH HOHOHO HOHOHOHOOO");
        UpdateMessage message = (UpdateMessage) arg;
        switch (message.getUpdateType()) {
            case TRANSACTION:
                Transaction transaction = (Transaction) message.getData();
                String desc = "Transaction with " + transaction.getInputs().size() + " inputs and " + transaction.getOutputs().size() + " outputs\n";
                view.appendReceivedTransaction(desc);
                break;
        }
    }

    public Client getClient() {
        return client;
    }
}
