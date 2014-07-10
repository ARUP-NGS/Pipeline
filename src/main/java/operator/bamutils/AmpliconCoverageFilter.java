package operator.bamutils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;

import net.sf.samtools.SAMRecord;
import operator.OperationFailedException;
/*
 * 
 * @author daniel
 * 
 */

public class AmpliconCoverageFilter extends BAMClassifier {
	
	public static final String BEDPATH = "bedpath";
	public static final String FRACTION = "fraction";
	public static final float defaultFrac = (float) 0.9;
	public String bedFile = null;
	
	public ReturnRecord processRecord(SAMRecord samRecord)
			throws NumberFormatException, IOException, OperationFailedException {
		String bedAttr = this.getAttribute(BEDPATH);
		if(bedAttr != null) {
			bedFile = bedAttr;
		}
		boolean value = readPasses(samRecord);
		ReturnRecord returnValue = new ReturnRecord(samRecord, value);
		return returnValue;
	}

	public boolean readPasses(SAMRecord read) throws NumberFormatException,
			IOException, OperationFailedException {
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
		int[] interval = { read.getAlignmentStart(), read.getAlignmentEnd() };
		int[] intersection = { 1337, 1337 };
		int ampliconLen = 0;
		String chrom = read.getReferenceName();
		BufferedReader br = new BufferedReader(new FileReader(
				bedFile));
		String entry;
		while ((entry = br.readLine()) != null) {
			String[] line = entry.split("\t");
			if (line[0].equals(chrom)) {
				ampliconLen = Integer.parseInt(line[2])
						- Integer.parseInt(line[1]);
				if (interval[1] > Integer.parseInt(line[2])) {
					intersection[1] = Integer.parseInt(line[2]);
				} else {
					intersection[1] = interval[1];
				}
				if (interval[0] < Integer.parseInt(line[1])) {
					intersection[0] = Integer.parseInt(line[1]);
				} else {
					intersection[0] = interval[0];
				}
				break;
			}
		}
		br.close();
		if (intersection[0] == 1337) {
			throw new OperationFailedException(
					"Chromosome not found in BED file. Check input files!",
					this);
		}

		float ampliconFrac = (intersection[1] - intersection[0])
				/ (float) ampliconLen;
		if (ampliconFrac < fraction) {
			return false;
		}
		return true; // No need to check if it is greater than that fraction -
						// the program exits before this point if a false is
						// returned.
	}
}