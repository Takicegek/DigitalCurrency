package network;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public enum MessageType {
    FIND_SUCCESSOR_JOIN, // returns USED_ID if there is a node with the sought id
    SUCCESSOR_FOUND,

    FIND_SUCCESSOR_FIX_FINGER, // returns the node that has the id greater or equal than the sought id

    GET_PREDECESSOR,
    SEND_PREDECESSOR,

    NOTIFY_SUCCESSOR,
    CHECK_PREDECESSOR,

    GET_SUCCESSOR,
    SEND_SUCCESSOR,

    RETRY,

    GET_BLOCKCHAIN, // ask the successor for the block chain and unspent transactions
    SEND_BLOCKCHAIN,

    BROADCAST_MESSAGE;

    private static Set<MessageType> answerNotNeeded = new HashSet<MessageType>() {
        {
            add(MessageType.BROADCAST_MESSAGE);
            add(NOTIFY_SUCCESSOR);
            add(BROADCAST_MESSAGE);
        }
    };

    public static boolean waitForAnswer(MessageType type) {
        return !answerNotNeeded.contains(type);
    }
}
