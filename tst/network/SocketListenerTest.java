package network;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sorin Nutu on 7/1/2015.
 */
public class SocketListenerTest {
    @Test
    public void testBelongsToIntervalForBroadcast() {
        assertTrue(SocketListener.belongsToIntervalForBroadcast(500, 400, 600));
        assertFalse(SocketListener.belongsToIntervalForBroadcast(300, 400, 600));
        assertTrue(SocketListener.belongsToIntervalForBroadcast(600, 400, 600));
        assertFalse(SocketListener.belongsToIntervalForBroadcast(400, 400, 600));

        assertFalse(SocketListener.belongsToIntervalForBroadcast(400, 700, 300));
        assertTrue(SocketListener.belongsToIntervalForBroadcast(800, 700, 300));
        assertTrue(SocketListener.belongsToIntervalForBroadcast(200, 700, 300));
        assertFalse(SocketListener.belongsToIntervalForBroadcast(300, 700, 700));
    }
}
