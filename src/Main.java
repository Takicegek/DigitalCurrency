import network.Node;

/**
 * Created by Sorin Nutu on 2/17/2015.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Node bootstrap = new Node("localhost", 10010);
        Thread.sleep(10000);
        bootstrap.broadcastMessage("My seventh broacast message!");
    }
}
