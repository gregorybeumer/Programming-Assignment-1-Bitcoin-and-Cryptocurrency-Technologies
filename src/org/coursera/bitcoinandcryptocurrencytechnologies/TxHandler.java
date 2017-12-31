package org.coursera.bitcoinandcryptocurrencytechnologies;
import java.util.ArrayList;
import java.util.List;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
	private UTXOPool utxoPool;
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
    	this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
    	boolean isValidTx = true;
    	for (int i = 0; i < tx.numInputs(); i++) {
			Transaction.Input in = tx.getInput(i);
			if (in != null) {
	    		UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
	    		// (1) all outputs claimed by tx are in the current UTXO pool
	    		if (!this.utxoPool.contains(u)) {
	    			isValidTx = false;
	    			break;
	    		}
	    		// (2) the signatures on each input of tx are valid
				Transaction.Output op = this.utxoPool.getTxOutput(u);
				if (!Crypto.verifySignature(op.address, tx.getRawDataToSign(i), in.signature)) {
	    			isValidTx = false;
	    			break;
	    		}
			}
    	}

    	if (isValidTx) {
	    	// (3) no UTXO is claimed multiple times by tx
    		List<UTXO> utxoList = new ArrayList<UTXO>();
    		for (Transaction.Input in: tx.getInputs()) {
        		UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
        		utxoList.add(u);
    		}
			outerLoop:
			for (int i = 0; i < utxoList.size(); i++) {
				UTXO curr = utxoList.get(i);
				for (int j = i+1; j < utxoList.size(); j++) {
					if (curr.equals(utxoList.get(j))) {
						isValidTx = false;
						break outerLoop;
					}
				}
			}
    	}

    	if (isValidTx) {
    		// (4) all of tx's output values are non-negative
    		for (Transaction.Output op: tx.getOutputs()) {
    			if (op.value < 0) {
    				isValidTx = false;
    				break;
    			}
    		}
    	}

    	if (isValidTx) {
    		// (5) the sum of tx's input values is greater than or equal to the sum of its output values
    		double sumTxInputValues = 0;
    		double sumTxOutputValues = 0;
    		for (Transaction.Input in: tx.getInputs()) {
    			UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
    			Transaction.Output op = this.utxoPool.getTxOutput(u);
    			sumTxInputValues += op.value;
    		}
    		for (Transaction.Output op: tx.getOutputs()) {
				sumTxOutputValues += op.value;
    		}
    		if (sumTxInputValues < sumTxOutputValues) {
    			isValidTx = false;
    		}
    	}

    	return isValidTx;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    	List<Transaction> acceptedTxs = new ArrayList<Transaction>();
    	for (Transaction tx: possibleTxs) {
    		if (isValidTx(tx)) {
    			acceptedTxs.add(tx);
    			for (Transaction.Input in: tx.getInputs()) {
    				UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
    				this.utxoPool.removeUTXO(u);
    			}
    			for (int i = 0; i < tx.numOutputs(); i++) {
    				Transaction.Output op = tx.getOutput(i);
    				if (op != null) {
	    				UTXO uNew = new UTXO(tx.getHash(), i);
	    				this.utxoPool.addUTXO(uNew, op);
    				}
    			}
    		}
    	}
    	return acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
    }

}
