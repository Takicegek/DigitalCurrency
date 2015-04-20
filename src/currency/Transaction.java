package currency;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a transaction that is initiated by a client to transfer a certain amount of money.
 * Created by Sorin Nutu on 4/19/2015.
 */
public class Transaction implements Serializable {
    private long id;
    private int numberOfInputs;
    private int numberOfOutputs;
    private List<TransactionRecord> inputs;
    private List<TransactionRecord> outputs;
    private long signature;
    private long sendersPublicKey;
}
