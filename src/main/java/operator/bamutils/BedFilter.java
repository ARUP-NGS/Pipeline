package operator.bamutils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;

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
 * contain the full interval contained within the input BED file.
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
		if(bedFile == null) {
			throw new OperationFailedException("bedFile wasn't initialized! Something went wrong.", this);
		}
		boolean value = readPasses(samRecord);
		ReturnRecord returnValue = new ReturnRecord(samRecord, value);
		return returnValue;

	}

	private boolean readPasses(SAMRecord read) throws IOException, OperationFailedException {
		int[] interval = { read.getAlignmentStart(), read.getAlignmentEnd() };
		String chrom = read.getReferenceName();
		if (chrom.contains("CTRL")) {
			return true;
		}
		BufferedReader br = null;
		//System.out.println("Bedfile location: " + bedFile);
		try{
			FileReader fr = new FileReader(bedFile);
			br = new BufferedReader(fr);
		}
		catch (NullPointerException e) {
			e.printStackTrace();
			System.out.println(e.toString());
		}
		String entry;

		while ((entry = br.readLine()) != null) {
			String[] line = entry.split("\t");
			if (line[0].equals(chrom)) {
				//System.out.println(line[0]+" " + line[1] + " " + line[2] + " is the bed entry for this chromosome.");
				//System.out.println(interval[0] + " " + interval[1] + " is the interval for the read.");
				if(Integer.parseInt(line[2]) < interval[1] && Integer.parseInt(line[1]) > interval[0]) {
					//System.out.println("Read covers region.");
					br.close();
					return true;
				}
				else {
					//System.out.println("Read fails to cover region.");
					br.close();
					return false;
				}
			}
		}
		br.close();
		return false; //Shouldn't happen, ever.
	}

}
