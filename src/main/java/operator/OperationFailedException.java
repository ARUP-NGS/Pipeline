package operator;

/**
 * These are thrown when an operator cannot complete its job for some reason
 * @author brendan
 *
 */
public class OperationFailedException extends Exception {
	
	protected Operator source = null;
	
	public OperationFailedException(String message, Operator source) {
		super(message);
		this.source = source;
	}
	
	/**
	 * Obtain the Operator that failed
	 * @return
	 */
	public Operator getSourceOperator() {
		return source;
	}
}
