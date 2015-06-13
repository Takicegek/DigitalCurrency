package gui;

import javax.swing.*;
import java.awt.*;

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

    public View() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JSplitPane blockchainPane = createBlockchainTab();

        JPanel panel2 = new JPanel();
        JLabel label2 = new JLabel("Create a transaction");
        panel2.add(label2);

        tabbedPane.addTab("Real time blockchain", blockchainPane);
        tabbedPane.addTab("Create a transaction", panel2);

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
        JPanel treePanel = new JPanel();
        treePanel.add(new JLabel("Real time blockchain"));

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

        // place both upper and lower parts in the block chain panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, lower);
        splitPane.setDividerSize(3);
        return splitPane;
    }

    public void appendReceivedTransaction(String transaction) {
        transactionsTextArea.append(transaction);
    }

    public void appendReceivedBlock(String block) {
        blocksTextArea.append(block);
    }

    public static void main(String[] args) {
        new View();
    }
}
