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

    public View() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel blockchainPanel = createBlockchainPanel();

        JPanel panel2 = new JPanel();
        JLabel label2 = new JLabel("Create a transaction");
        panel2.add(label2);

        tabbedPane.addTab("Real time blockchain", blockchainPanel);
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
    private JPanel createBlockchainPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // contains the tree and a text area
        JPanel upper = new JPanel();

        // transactions text area
        transactionsTextArea = new JTextArea(10, 25);
        transactionsTextArea.setEditable(false);

        JScrollPane transactionsTextAreaScrollPane = new JScrollPane(transactionsTextArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        upper.add(transactionsTextAreaScrollPane, BorderLayout.EAST);
        upper.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // contains two text areas
        JPanel lower = new JPanel();

        // blocks text area
        blocksTextArea = new JTextArea(10, 25);
        blocksTextArea.setEditable(false);

        JScrollPane blocksTextAreaScrollPane = new JScrollPane(blocksTextArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        lower.add(blocksTextAreaScrollPane, BorderLayout.EAST);
        lower.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // place both upper and lower parts in the block chain panel
        panel.add(upper, BorderLayout.CENTER);
        panel.add(lower, BorderLayout.SOUTH);

        return panel;
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
