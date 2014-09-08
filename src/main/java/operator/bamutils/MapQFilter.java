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
public class MapQFilter extends BAMClassifier {
	public static final String MAPQMIN = "mapq.min";
	public static final int defaultMapq = 10;
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
		int mapqMin = defaultMapq;
		if (getAttribute(MAPQMIN) != null) {
			mapqMin = Integer.parseInt(getAttribute(MAPQMIN));
		}

		// If the mapqMin is not between 0 and 1, set it to the default value.
		if (mapqMin < 0 || mapqMin >= 255) {
			String logString = "Minimum mapping quality provided is not in the scope of [0,254]. \n"
							+ Float.toString(mapqMin)
							+ "Minimum mapping quality is now set to default "
							+ Float.toString(defaultMapq);
			logger.info(logString);
			mapqMin = defaultMapq;
		}
		//byte[] refSequence = SequenceUtil.makeReferenceFromAlignment(read, true);
		//int mismatchCount = 0;
		/*for(int i=0;i<read.getReadLength();i++){
			if(refSequence[i]!=read.getReadBases()[i]) {
				mismatchCount+=1;
			}
		}
		*/
		int readMQ = read.getMappingQuality();
		if (readMQ < mapqMin || read.getReadUnmappedFlag()) {
			return false;
		} else {
			return true;
		}
	}

}
