package gui.treelayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import javax.swing.*;

import currency.Block;
import org.abego.treelayout.TreeForTreeLayout;
import org.abego.treelayout.TreeLayout;

/**
 * A JComponent displaying a tree of TextInBoxes, given by a {@link TreeLayout}.
 *
 * @author Udo Borkowski (ub@abego.org)
 */
public class TreePane extends JComponent {
    private final TreeLayout<Block> treeLayout;

    private TreeForTreeLayout<Block> getTree() {
        return treeLayout.getTree();
    }

    private Iterable<Block> getChildren(Block parent) {
        return getTree().getChildren(parent);
    }

    private Rectangle2D.Double getBoundsOfNode(Block node) {
        return treeLayout.getNodeBounds().get(node);
    }

    /**
     * Specifies the tree to be displayed by passing in a {@link TreeLayout} for
     * that tree.
     *
     * @param treeLayout
     */
    public TreePane(final TreeLayout<Block> treeLayout) {
        this.treeLayout = treeLayout;

        Dimension size = treeLayout.getBounds().getBounds().getSize();
        setPreferredSize(size);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();

                for (Map.Entry<Block, Rectangle2D.Double> box : treeLayout.getNodeBounds().entrySet()) {
                    Rectangle2D.Double rectangle = box.getValue();
                    if (x >= rectangle.getX() && x <= rectangle.getX() + rectangle.getWidth() &&
                            y >= rectangle.getY() && y <= rectangle.getY() + rectangle.getHeight()) {
                        String message = "Block " + box.getKey().hashCode() + ", mined by node " + box.getKey().getMinerId() + "\n";
                        message += "It contains " + box.getKey().getTransactions().size() + " transactions.";
                        JOptionPane.showMessageDialog(TreePane.this, message);
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------
    // painting

    private final static int ARC_SIZE = 10;
    private final static Color BOX_COLOR = Color.orange;
    private final static Color BORDER_COLOR = Color.darkGray;
    private final static Color TEXT_COLOR = Color.black;

    private void paintEdges(Graphics g, Block parent) {
        if (!getTree().isLeaf(parent)) {
            Rectangle2D.Double b1 = getBoundsOfNode(parent);
            double x1 = b1.getCenterX();
            double y1 = b1.getCenterY();
            for (Block child : getChildren(parent)) {
                Rectangle2D.Double b2 = getBoundsOfNode(child);
                g.drawLine((int) x1, (int) y1, (int) b2.getCenterX(),
                        (int) b2.getCenterY());

                paintEdges(g, child);
            }
        }
    }

    private void paintBox(Graphics g, Block textInBox) {
        // draw the box in the background
        g.setColor(BOX_COLOR);
        Rectangle2D.Double box = getBoundsOfNode(textInBox);
        g.fillRoundRect((int) box.x, (int) box.y, (int) box.width - 1,
                (int) box.height - 1, ARC_SIZE, ARC_SIZE);
        g.setColor(BORDER_COLOR);
        g.drawRoundRect((int) box.x, (int) box.y, (int) box.width - 1,
                (int) box.height - 1, ARC_SIZE, ARC_SIZE);

        // draw the text on top of the box (possibly multiple lines)
        g.setColor(TEXT_COLOR);
        String[] lines = ("" + textInBox.hashCode()).split("\n");
        FontMetrics m = getFontMetrics(getFont());
        int x = (int) box.x + ARC_SIZE / 2;
        int y = (int) box.y + m.getAscent() + m.getLeading() + 1;
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], x, y);
            y += m.getHeight();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        paintEdges(g, getTree().getRoot());

        // paint the boxes
        for (Block textInBox : treeLayout.getNodeBounds().keySet()) {
            paintBox(g, textInBox);
        }
    }
}