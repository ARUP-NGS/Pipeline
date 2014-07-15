package operator.bamutils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import net.sf.samtools.SAMRecord;
import json.JSONException;
import operator.IOOperator;
import operator.OperationFailedException;
import operator.bamutils.BAMClassifier.ReturnRecord;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;

/*
 * Returns a BAM file containing all reads in the input BAM file which
 * intersect with the input BED file.
 * 
 * @author daniel
 * 
 */

public class BedFilter extends BAMClassifier {
	public static final String BEDPATH = "bedpath";

	public String bedFile = null;

	@Override
	public ReturnRecord processRecord(SAMRecord samRecord)
			throws FileNotFoundException, NumberFormatException, IOException,
			OperationFailedException {

		String bedAttr = this.getAttribute(BEDPATH);
		if (bedAttr != null) {
			bedFile = bedAttr;
		}
		boolean value = readPasses(samRecord);
		ReturnRecord returnValue = new ReturnRecord(samRecord, value);
		return returnValue;

	}

	private boolean readPasses(SAMRecord read) throws IOException {
		int[] interval = { read.getAlignmentStart(), read.getAlignmentEnd() };
		String chrom = read.getReferenceName();
		BufferedReader br = new BufferedReader(new FileReader(bedFile));
		String entry;
		while ((entry = br.readLine()) != null) {
			String[] line = entry.split("\t");
			if (line[0].equals(chrom)) {
				if(Integer.parseInt(line[2]) < interval[0] || Integer.parseInt(line[1]) > interval[1]) {
					br.close();
					return false;
				}
				else {
					br.close();
					return true;
				}
			}
		}
		br.close();
		return false; //Shouldn't happen, ever.
	}

}
