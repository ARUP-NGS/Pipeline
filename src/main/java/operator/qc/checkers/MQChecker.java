package operator.qc.checkers;

import math.Histogram;
import buffer.BAMMetrics;

/**
 * Checks mapping quality information from a BAMMetrics object to look
 * for QC issues
 * @author brendan
 *
 */
public class MQChecker extends AbstractChecker<BAMMetrics> {
	
	@Override
	public operator.qc.checkers.QCItemCheck.QCCheckResult checkItem(
			BAMMetrics bam) {
		
		QCCheckResult result = new QCCheckResult();
		result.message = "";
		result.result = QCItemCheck.ResultType.OK;

		Histogram mqHisto = bam.mqHistogram;
		if (mqHisto == null) {
			result.message = "No mapping quality information found";
			result.result =QCItemCheck.ResultType.UNKNOWN;
			return result;
		}
		
		double greaterThan50 = 100.0-100.0*mqHisto.getCumulativeDensity( mqHisto.getBin(50.0));
		//double greaterThan30 = 100.0-100.0*mqHisto.getCumulativeDensity( mqHisto.getBin(30.0));
		//double greaterThan10 = 100.0-100.0*mqHisto.getCumulativeDensity( mqHisto.getBin(10.0));
		
		if (greaterThan50 < 80.0) {
			result.result = QCItemCheck.ResultType.WARNING;
			result.message = "Too few reads with mapping quality >50 : " + ("" + greaterThan50).substring(0, 5) + "%\n";
		}
		if (greaterThan50 < 60.0) {
			result.result = QCItemCheck.ResultType.SEVERE;
			result.message = "Far too few reads with mapping quality >50 : " + ("" + greaterThan50).substring(0, 5) + "%\n";
		}
		
				
		return result;
	}

	private String toPercentage(double val) {
		String str = "" + 100.0*val;
		if (str.length()>6)
			str = str.substring(0, 6);
		return str;
	}

}
