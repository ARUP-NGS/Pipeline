package operator.bamutils;

import java.io.IOException;
import java.util.logging.Logger;

import buffer.BAMFile;
import buffer.BEDFile;
import buffer.DOCMetrics;
import buffer.FileBuffer;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import util.coverage.CoverageCalcTest;

/**
 * This class computes a 'DOCMetrics' (Depth Of Coverage Metrics) object
 * from a BAM file. It's meant to be a drop-in replacement for the GATK's old / slow
 * DepthOfCoverage tool 
 * @author brendanofallon
 *
 */
public class FastDepthOfCoverage extends IOOperator {

	
	public boolean requiresReference() {
		return false;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		BEDFile intervals = (BEDFile) this.getInputBufferForClass(BEDFile.class);
		BAMFile bam = (BAMFile) this.getInputBufferForClass(BAMFile.class);
		DOCMetrics metrics = (DOCMetrics) getOutputBufferForClass(DOCMetrics.class);
		
		
		try {
			CoverageCalcTest covCalc = new CoverageCalcTest(bam.getFile(), intervals);
			covCalc.setThreadCount( getPipelineOwner().getThreadCount() );
			
			int[] depthHistogram = covCalc.computeOverallCoverage();
			
			
			//The depth histogram is an un-normalized raw counts of the number of bases with a certain read depth
			//for instance, the 10th position in the list of the number of bases found with read depths = 10 
			double mean = CoverageCalcTest.getMean(depthHistogram);
			double[] covs = CoverageCalcTest.convertCountsToProportions(depthHistogram);
			
			int[] cutoffs = new int[]{1, 10, 15, 20, 25, 50, 100};
			double[] covAboveCutoffs = new double[cutoffs.length];
			for(int i=0; i<covAboveCutoffs.length; i++) {
				covAboveCutoffs[i] = covs[cutoffs[i]];
			}
			
			metrics.setMeanCoverage(mean);
			metrics.setCoverageProportions(covs);
			metrics.setCutoffs(cutoffs);
			metrics.setFractionAboveCutoff(covAboveCutoffs);
			
		} catch (IOException e) {
			e.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning(getObjectLabel() + " encountered IO Error : " + e.getLocalizedMessage());
			throw new OperationFailedException("IO Error calculating coverages : " + e.getLocalizedMessage(), this);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning(getObjectLabel() + " was interrupted: " + e.getLocalizedMessage());
			throw new OperationFailedException("Interrupted calculating coverages : " + e.getLocalizedMessage(), this);
		}
		
		
	}

}
