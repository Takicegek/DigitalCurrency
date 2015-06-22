package gui.treelayout;

import currency.Block;
import org.abego.treelayout.NodeExtentProvider;

/**
 * Created by Sorin Nutu on 6/14/2015.
 */
public class BlockExtentProvider implements NodeExtentProvider<Block> {
    @Override
    public double getWidth(Block block) {
        return 90;
    }

    @Override
    public double getHeight(Block block) {
        return 20;
    }
}
