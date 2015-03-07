package network;

import java.io.Serializable;

/**
 * Represents a message that will be passed between network nodes.
 * Created by Sorin Nutu on 2/17/2015.
 */
public class Message implements Serializable {
    private MessageType type;
    private Object object;
    private int tag;

    public Message(MessageType type, Object object) {
        this.type = type;
        this.object = object;
    }

    public Message(MessageType type, Object object, int tag) {
        this.type = type;
        this.object = object;
        this.tag = tag;
    }

    public Message() {

    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", object=" + object +
                ", tag=" + tag +
                '}';
    }
}
