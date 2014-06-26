package operator.variant;

import java.util.logging.Logger;

import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.variant.VariantRec;

/**
 * Simple IO operator that puts all novel variants in their own pool
 * @author brendan
 *
 */
public class NovelFilter extends VariantMultiFilter {

	double popFreqCutoff = 0.001;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		if (inVariants == null) {
			throw new OperationFailedException("No input variants found", this);
		}
		if (outVariants == null) {
			throw new OperationFailedException("No output variants found", this);
		}
		
		
		//Read attributes
		Double cutoffAttr = readDoubleAttribute(POP_FREQ);
		if (cutoffAttr != null) {
			popFreqCutoff = cutoffAttr;
		}
		
		for(String contig : inVariants.getContigs()) {
			for(VariantRec var : inVariants.getVariantsForContig(contig)) {
				Double freq = var.getProperty(VariantRec.POP_FREQUENCY);
				if (freq == null || freq < popFreqCutoff) {
					outVariants.addRecordNoSort(var);
				}
			}
		}
		
		outVariants.sortAllContigs();
		logger.info("Novel filter, retained " + outVariants.size() + " of " + inVariants.size() + " variants");

	}

}
