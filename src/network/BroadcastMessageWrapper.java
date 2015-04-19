package network;

import java.io.Serializable;

/**
 * Message wrapper for broadcast messages.
 *
 * When a node wants to broadcast a message, it will send it to two nodes: the successor and the last finger.
 * Every message is completed with an [start, end] interval and that message should be sent only to nodes inside
 * that interval.
 *
 * When a node receives a broadcast message, it processes it and then checks the interval. It will send the message
 * further to its successor and to the finger that is the closest to the middle of the interval. If the node does not
 * have any finger in that interval, it stops sending the message.
 *
 * This way, the interval halves in size and the number of messages that are transmitted in parallel doubles.
 * The time complexity for broadcast is O(log number of nodes).
 *
 * Created by Sorin Nutu on 4/5/2015.
 */
public class BroadcastMessageWrapper implements Serializable {
    private long start;
    private long end;
    private Object message;

    public BroadcastMessageWrapper(long start, long end, Object message) {
        this.start = start;
        this.end = end;
        this.message = message;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }
}
