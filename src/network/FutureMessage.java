package network;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Sorin Nutu on 2/27/2015.
 */
public class FutureMessage implements Future<Message> {

    protected Message message;


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
        synchronized (this) {
            System.err.println("ASTEPT ELIBERAREA!");
            wait();
            System.err.println("LOCK ELIBERAT! pentru tagul " + message.getTag());
        }
        return message;
    }

    @Override
    public Message get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
