package operator.bcrabl;

public class QualityCheckResult {

	final boolean passed;
	
	final String message;
	
	public QualityCheckResult(boolean passed, String message) {
		this.passed  = passed;
		this.message = message;
	}
	
}
