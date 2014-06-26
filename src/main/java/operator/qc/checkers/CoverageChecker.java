package operator.qc.checkers;

import buffer.DOCMetrics;

/**
 * Assesses a DOCMetrics object to determine if coverage metrics fall outside the norm
 * @author brendan
 *
 */
public class CoverageChecker extends AbstractChecker<DOCMetrics>{

	
	
	@Override
	public QCCheckResult checkItem(DOCMetrics doc) {
		QCCheckResult result = new QCCheckResult();
		result.message = "";
		result.result = QCItemCheck.ResultType.OK;
		
		
		if (doc.getMeanCoverage() < 15) {
			result.result = QCItemCheck.ResultType.SEVERE;
			result.message =  "Mean depth of coverage is very low (" + doc.getMeanCoverage() + ")\n";
		}
		else {
			if (doc.getMeanCoverage() < 30) {
				result.result = QCItemCheck.ResultType.WARNING;
				result.message =  "Mean depth of coverage is low (" + doc.getMeanCoverage() + ")\n";
			}	
		}
		
		
		if (doc.getCoverageProportions() != null) {
			if (doc.getCoverageProportions()[10] < 0.75) {
				result.result = QCItemCheck.ResultType.SEVERE;
				result.message = "Very few bases covered to depth of 10 or greater (" + doc.getCoverageProportions()[10] +")\n";
			}
			else {
				if (doc.getCoverageProportions()[10] < 0.90) {
					result.result = QCItemCheck.ResultType.WARNING;
					result.message = "Too few bases covered to depth of 10 or greater (" + doc.getCoverageProportions()[10] +")\n";
				}
			}
			
		}
		
		
		return result;
	}

	

}
