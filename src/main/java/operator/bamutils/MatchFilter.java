package operator.bamutils;

import net.sf.samtools.SAMRecord;

/**
 * Splits a BAM file into one of two output files - one BAM for "fail" reads
 * which have less than a given fraction (default: 0.667) of concordant bp with
 * the reference, one with "pass" reads.
 * 
 * @author daniel
 *
 */
public class MatchFilter extends BAMClassifier {
	public static final String FRACTION = "fraction";
	public static final float defaultFrac = (float) 0.667;

	public ReturnRecord processRecord(SAMRecord samRecord) {
		boolean value = readPasses(samRecord); // Initializing return value for
												// this function.
		ReturnRecord returnValue = new ReturnRecord(samRecord, value);
		return returnValue;
	}

	/**
	 * Fail read if less than a given fraction is mapped.
	 * 
	 *
	 */

	public boolean readPasses(SAMRecord read) {
		float fraction = defaultFrac;
		if (getAttribute(FRACTION) != null) {
			fraction = Float.parseFloat(getAttribute(FRACTION));
		}

		// If the fraction is not between 0 and 1, set it to the default value.
		if (fraction < 0 || fraction > 1) {
			System.out
					.println("Fraction provided is not in the scope of [0,1]. \n"
							+ Float.toString(fraction)
							+ "Fraction is now set to default "
							+ Float.toString(defaultFrac));
			fraction = defaultFrac;
		}
		//byte[] refSequence = SequenceUtil.makeReferenceFromAlignment(read, true);
		//int mismatchCount = 0;
		/*for(int i=0;i<read.getReadLength();i++){
			if(refSequence[i]!=read.getReadBases()[i]) {
				mismatchCount+=1;
			}
		}
		*/
		Integer mismatchCount = (Integer) read.getAttribute("NM");
		if ((double) mismatchCount / (double)read.getReadLength() > (1 - fraction)) {
			return false;
		} else {
			return true;
		}
	}

}
