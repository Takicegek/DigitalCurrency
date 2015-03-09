package network;

import java.util.concurrent.*;

/**
 * Created by Sorin Nutu on 2/27/2015.
 */
public class FutureMessage implements Future<Message> {

    protected Message message;
    private Semaphore semaphore;

    public FutureMessage() {
        semaphore = new Semaphore(0);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public Message get() throws InterruptedException, ExecutionException {
        semaphore.acquire();
        return message;
    }

    @Override
    public Message get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public void setMessage(Message message) {
        this.message = message;
        semaphore.release();
    }
}
