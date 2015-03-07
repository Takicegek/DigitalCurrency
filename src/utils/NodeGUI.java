package utils;

import network.Node;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Sorin Nutu on 3/6/2015.
 */
public class NodeGUI extends JFrame {
    public static NodeGUI instance;

    private JLabel textId, id;
    private JLabel nextId, next;
    private JLabel prevId, previous;


    public static NodeGUI getInstance() {
        if (instance == null) {
            instance = new NodeGUI();
        }
        return instance;
    }

    private NodeGUI() {
        id = new JLabel();
        next = new JLabel();
        previous = new JLabel();
        textId = new JLabel("Id-ul curent: ");
        nextId = new JLabel("Id-ul succesorului: ");
        prevId = new JLabel("Id-ul predecesorului: ");

        setLayout(new GridLayout(3, 2));

        getContentPane().add(textId);
        getContentPane().add(id);

        getContentPane().add(nextId);
        getContentPane().add(next);

        getContentPane().add(prevId);
        getContentPane().add(previous);

        setPreferredSize(new Dimension(300, 300));
        pack();
        setVisible(true);
    }

    public void setId(String id) {
        this.id.setText(id);
    }

    public void setNext(String next) {
        this.next.setText(next);
    }

    public void setPrevious(String previous) {
        this.previous.setText(previous);
    }
}
