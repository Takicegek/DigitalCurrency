package network;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public enum MessageType {
    FIND_SUCCESSOR_JOIN, // returns USED_ID if there is a node with the sought id
    USED_ID,
    SUCCESSOR_FOUND,
    FIND_SUCCESSOR_FIX_FINGER, // returns the node that has the id greater or equal than the sought id

    PING,

    GET_PREDECESSOR,
    NOTIFY_SUCCESSOR
}
