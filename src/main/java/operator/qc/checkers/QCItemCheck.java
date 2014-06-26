package operator.qc.checkers;

/**
 * These objects can perform a quality control 'check' on some object (typically a DOCMetrics
 * or BAMMetrics object) to determine if some of values fall outside of normal bounds
 *  
 * @author brendan
 *
 * @param <T>
 */
public interface QCItemCheck<T> {
	
	public QCCheckResult checkItem(T item);
	
	public enum ResultType {OK, WARNING, SEVERE, UNKNOWN}
	
	/**
	 * The result of a qc check - each result has a severity level 
	 * and, optionally, a message
	 * @author brendan
	 *
	 */
	public class QCCheckResult {
		ResultType result = null;
		String message = null;
		
		public ResultType getResult() {
			return result;
		}
		
		public String getMessage() {
			return message;
		}
	}

}
