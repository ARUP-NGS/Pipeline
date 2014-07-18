package operator.bamutils;

import java.util.List;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;

/**
 * Splits a BAM file into one of two output files - one BAM for "fail" reads
 * which have less than a given fraction (default: 0.7) mapped to a reference.
 * 
 * @author daniel
 *
 */
public class FracAlignFilter extends BAMClassifier {
	public static final String FRACTION = "fraction";
	public static final float defaultFrac = (float) 0.7;

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
		int sumMap = 0;
		//Acquire cigar, sum mapped ("M") bases
		Cigar cig = read.getCigar();
		List<CigarElement> cigContents = cig.getCigarElements();
		for (CigarElement cigEl : cigContents ) {
			if (cigEl.getOperator() == CigarOperator.M) {
				sumMap += cigEl.getLength();
			}
		}
		
		if ((float) sumMap / read.getReadLength() < fraction) {
			/*System.out.println("Read failed."); 
			System.out.println("Mapped bp: " + sumMap + ". Total bp: " + read.getReadLength() + ".\n");
			*/
			return false;
		} else
			/*System.out.println("Read passed.");
			System.out.println("Mapped bp: " + sumMap + ". Total bp: " + read.getReadLength() + ".\n");
			*/
			return true;
	}

}
