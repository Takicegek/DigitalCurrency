package network;

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

    RETRY;

    public static boolean waitForAnswer(MessageType type) {
        if (type == MessageType.NOTIFY_SUCCESSOR) {
            return false;
        }
        return true;
    }
}
