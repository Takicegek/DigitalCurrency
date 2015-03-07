package utils;

import network.Message;

/**
 * Created by Sorin Nutu on 3/1/2015.
 */
public class MessageWrapper {
    private Message message;
    private boolean waitForAnswer;

    public MessageWrapper(Message message, boolean waitForAnswer) {
        this.message = message;
        this.waitForAnswer = waitForAnswer;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public boolean isWaitForAnswer() {
        return waitForAnswer;
    }

    public void setWaitForAnswer(boolean waitForAnswer) {
        this.waitForAnswer = waitForAnswer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageWrapper that = (MessageWrapper) o;

        if (waitForAnswer != that.waitForAnswer) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (waitForAnswer ? 1 : 0);
        return result;
    }
}
