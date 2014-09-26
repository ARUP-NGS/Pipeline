package operator.bamutils;

import java.util.logging.Logger;

import pipeline.Pipeline;
import net.sf.samtools.SAMRecord;

/**
 * Splits a BAM file into one of two output files - one BAM for "fail" reads
 * which have less than a given mapqMin (default: 10) or are unmapped to
 * the reference, one with "pass" reads which have a sufficient mapQ.
 * 
 * @author daniel
 *
 */
public class MapFilter extends BAMClassifier {
	public Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

	public ReturnRecord processRecord(SAMRecord samRecord) {
		boolean value = readPasses(samRecord); // Initializing return value for
												// this function.
		ReturnRecord returnValue = new ReturnRecord(samRecord, value);
		return returnValue;
	}

	/**
	 * Fail read if it is unmapped or has less than a minimum mapping quality.
	 * 
	 *
	 */

	public boolean readPasses(SAMRecord read) {
		if(read.getReferenceName().equals("*"))
			return false;
		 else 
			return true;
	}

}
