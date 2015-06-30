package gui;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The project's user interface which has two tabs, one for real time description of the block chain, received
 * transactions and blocks and one for creating and broadcasting transactions.
 *
 * Created by Sorin Nutu on 6/13/2015.
 */
public class View extends JFrame {

    private JTextArea transactionsTextArea;
    private JTextArea blocksTextArea;
    private JTextArea detailsTextArea;
    private JPanel blockchainPanel;
    private JTextField addressTextField;
    private JTextField balanceTextField;
    private List<JTextField> recipientsAdresses;
    private List<JTextField> amounts;
    private JButton sendButton;
    private JLabel messageLabel;

    public View() {
        recipientsAdresses = new ArrayList<>();
        amounts = new ArrayList<>();

        JTabbedPane tabbedPane = new JTabbedPane();

        JSplitPane blockchainPane = createBlockchainTab();
        JPanel transactionPanel = createTransactionTab();

        tabbedPane.addTab("Real time blockchain", blockchainPane);
        tabbedPane.addTab("Create a transaction", transactionPanel);

        add(tabbedPane);

        setSize(new Dimension(800, 500));
        setLocation(250, 150);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Digital currency client");

        setVisible(true);
    }

    /**
     * Creates the whole window for the blockchain. It will contain four main panels:
     * - the visual representation of the tree
     * - a textarea with the transactions
     * - a textarea with the blocks
     * - a textarea where details about blocks or transactions will be shown
     * @return the JPanel for the block chain tab
     */
    private JSplitPane createBlockchainTab() {
        // the upper part of the tab
        // tree panel
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.add(new JLabel("Real time blockchain"), BorderLayout.NORTH);

        blockchainPanel = new JPanel();
        JScrollPane scrollForBlockchainPanel = new JScrollPane(blockchainPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treePanel.add(scrollForBlockchainPanel, BorderLayout.CENTER);

        // transactions panel
        JPanel transactions = new JPanel(new BorderLayout());
        JLabel transactionsLabel = new JLabel("Recent transactions");
        transactionsTextArea = new JTextArea(10, 25);
        transactionsTextArea.setEditable(false);

        JScrollPane transactionsTextAreaScrollPane = new JScrollPane(transactionsTextArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        transactions.add(transactionsLabel, BorderLayout.NORTH);
        transactions.add(transactionsTextAreaScrollPane, BorderLayout.SOUTH);

        JSplitPane upper = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, transactions);
        upper.setDividerSize(2);
        upper.setResizeWeight(1d);

        // the lower part of the pane
        // details text area
        JPanel details = new JPanel(new BorderLayout());
        JLabel detailsLabel = new JLabel("Details");
        detailsTextArea = new JTextArea(10, 25);
        detailsTextArea.setEditable(false);

        JScrollPane detailsTextAreaScrollPane = new JScrollPane(detailsTextArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        details.add(detailsLabel, BorderLayout.NORTH);
        details.add(detailsTextAreaScrollPane, BorderLayout.CENTER);

        // blocks text area
        JPanel blocks = new JPanel(new BorderLayout());
        JLabel blocksLabel = new JLabel("Recent blocks");
        blocksTextArea = new JTextArea(10, 25);
        blocksTextArea.setEditable(false);

        JScrollPane blocksTextAreaScrollPane = new JScrollPane(blocksTextArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        blocks.add(blocksLabel, BorderLayout.NORTH);
        blocks.add(blocksTextAreaScrollPane, BorderLayout.CENTER);


        JSplitPane lower = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, details, blocks);
        lower.setDividerSize(2);
        lower.setResizeWeight(1d);

        // place both upper and lower parts in the block chain panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, lower);
        splitPane.setDividerSize(3);
        return splitPane;
    }

    private JPanel createTransactionTab() {
        JPanel transactionTab = new JPanel();
        transactionTab.setLayout(new BoxLayout(transactionTab, BoxLayout.Y_AXIS));

        JPanel addressPanel = new JPanel();
        addressPanel.add(new JLabel("Your address: "));
        addressTextField = new JTextField(40);
        addressTextField.setEditable(false);
        addressPanel.add(addressTextField);

        JPanel balancePanel = new JPanel();
        balancePanel.add(new JLabel("Balance: "));
        balanceTextField = new JTextField(10);
        balanceTextField.setEditable(false);
        balancePanel.add(balanceTextField);

        sendButton = new JButton("Send transaction");

        messageLabel = new JLabel();

        JPanel inputs = new JPanel();
        inputs.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
//        inputs.setPreferredSize(new Dimension(100, 400));
        inputs.add(createPayPanel());
        inputs.add(createPayPanel());
        inputs.add(createPayPanel());

        transactionTab.add(addressPanel);
        transactionTab.add(balancePanel);
        transactionTab.add(inputs);
        transactionTab.add(sendButton);
        transactionTab.add(messageLabel);

        return transactionTab;
    }

    private JPanel createPayPanel() {
        JPanel payPanel = new JPanel();

        JLabel amountLabel = new JLabel("Amount: ");
        JTextField amountTextField = new JTextField(5);
        JLabel addressLabel = new JLabel("Address: ");
        JTextField addressTextField = new JTextField(40);

        payPanel.add(amountLabel);
        payPanel.add(amountTextField);
        payPanel.add(addressLabel);
        payPanel.add(addressTextField);

        recipientsAdresses.add(addressTextField);
        amounts.add(amountTextField);

        return payPanel;
    }

    public void setSendButtonActionListener(ActionListener listener) {
        sendButton.addActionListener(listener);
    }

    public void updateBlockchain(JComponent tree) {
        blockchainPanel.removeAll();
        blockchainPanel.add(tree);
        blockchainPanel.revalidate();
    }

    public void updateBalance(Double balance) {
        balanceTextField.setText(balance.toString());
    }

    public void appendReceivedTransaction(String transaction) {
        transactionsTextArea.append(transaction);
    }

    public void appendDetails(String details) {
        detailsTextArea.append(details);
    }

    public void setAddressTextField(String addressTextField) {
        this.addressTextField.setText(addressTextField);
    }

    public void setMessageLabel(String message) {
        messageLabel.setText(message);
    }

    public void appendReceivedBlock(String block) {
        blocksTextArea.append(block);
    }

    public static void main(String[] args) {
        new View();
    }

    public List<JTextField> getAmounts() {
        return amounts;
    }

    public List<JTextField> getRecipientsAdresses() {
        return recipientsAdresses;
    }
}
