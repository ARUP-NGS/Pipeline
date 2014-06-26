package operator.qc.checkers;

import java.text.DecimalFormat;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Perform a few basic checks on a set of variants
 * @author brendan
 *
 */
public class VariantPoolChecker extends AbstractChecker<VariantPool> {

	DecimalFormat formatter = new DecimalFormat("0.000");
	int basesExamined;
	
	public VariantPoolChecker(int basesExamined) {
		this.basesExamined = basesExamined;
	}
	
	@Override
	public QCCheckResult checkItem(VariantPool pool) {
		QCCheckResult result = new QCCheckResult();
		result.message = "";
		result.result = QCItemCheck.ResultType.OK;

		//If we expect that one in a thousand bases differs
		double expectedVars = basesExamined / 1250.0;
		
		double dif = Math.abs( (double)(pool.size() - expectedVars)/expectedVars);

		
		if (dif > 0.25) {
			result.result = QCItemCheck.ResultType.SEVERE;
			result.message = "Very high number of variants called : " + pool.size() + "\n";	
		}
		else {
			if (dif > 0.20) {
				result.result = QCItemCheck.ResultType.WARNING;
				result.message = "High number of variants called : " + pool.size() + "\n";	
			}
		}
		

		
		double tt = pool.computeTTRatio();
		
		//Different cutoffs for small pools / panels
		if (pool.size() < 1000) {
			if (tt < 1.5) {
				result.result = QCItemCheck.ResultType.SEVERE;
				result.message = result.message + "Very low TT ratio : " + formatter.format(tt) + "\n";
			}
			else {
				if (tt < 2.0) {
					result.result = QCItemCheck.ResultType.WARNING;
					result.message = result.message + "Low TT ratio : " + formatter.format(tt) + "\n";
				}
			}
			
		}
		else {
			
			if (tt < 2.0) {
				result.result = QCItemCheck.ResultType.SEVERE;
				result.message = result.message + "Very low TT ratio : " + formatter.format(tt) + "\n";
			}
			else {
				if (tt < 2.25) {
					result.result = QCItemCheck.ResultType.WARNING;
					result.message = result.message + "Low TT ratio : " + formatter.format(tt) + "\n";
				}
			}
		}
		
		//heterozygosity
		double het = (double)pool.countHeteros() / (double)pool.size();
		if (pool.size() < 1000) {
			if (het > 0.85) {
				result.result = QCItemCheck.ResultType.SEVERE;
				result.message = result.message + "Very high het % : " + formatter.format(het) + "\n";
			}
			else {
				if (het > 0.75) {
					result.result = QCItemCheck.ResultType.WARNING;
					result.message = result.message + "High het % : " + formatter.format(het) + "\n";
				}
			}
			
		}
		else {
			
			if (het > 0.80) {
				result.result = QCItemCheck.ResultType.SEVERE;
				result.message = result.message + "Very high het % : " + het + "\n";
			}
			else {
				if (het > 0.70) {
					result.result = QCItemCheck.ResultType.WARNING;
					result.message = result.message + "High het % : " + het + "\n";
				}
			}
			
		}
		
		
		
		//Fraction novel
		int knowns = 0;
		for(String contig : pool.getContigs() ) {
			for(VariantRec var : pool.getVariantsForContig(contig)) {
				Double freq = var.getProperty(VariantRec.POP_FREQUENCY);
				String rsID = var.getPropertyOrAnnotation(VariantRec.RSNUM);
				if ( (freq != null && freq > 1e-5) || (rsID!=null && rsID.length()>3)) {
					knowns++;
				}
			}
		}
		
		double novelFrac = 100.0*(((double)pool.size() - knowns) / (double)pool.size());
		String novelStr = "" + novelFrac;
		if (novelStr.length()>5)
			novelStr = novelStr.substring(0, 5);
		
		if (pool.size() < 1000) {
			if (novelFrac > 0.10) {
				result.result = QCItemCheck.ResultType.SEVERE;
				result.message = result.message + "Very high novel  % : " + novelStr + "%\n";				
			}
			else {
				if (novelFrac > 0.05) {
					result.result = QCItemCheck.ResultType.WARNING;
					result.message = result.message + "High novel  % : " + novelStr + "%\n";				
				}
			}
			
		}
		else {
			
			if (novelFrac > 0.8) {
				result.result = QCItemCheck.ResultType.SEVERE;
				result.message = result.message + "Very high novel  % : " + novelStr + "%\n";				
			}
			else {
				if (novelFrac > 0.04) {
					result.result = QCItemCheck.ResultType.WARNING;
					result.message = result.message + "High novel  % : " + novelStr + "%\n";				
				}
			}
			
		}
		
		
		return result;
	}

}
