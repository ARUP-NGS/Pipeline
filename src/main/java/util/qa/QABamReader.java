package util.qa;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import buffer.BAMFile;
import buffer.BEDFile;

public class QABamReader {

	public static BamResults processBAMFile(BEDFile bed, BAMFile inputBAM) {
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		if (inputBAM.getFile() == null) {
			throw new IllegalArgumentException("File associated with inputBAM " + inputBAM.getAbsolutePath() + " is null");
		}
		final SAMFileReader inputSam = new SAMFileReader(inputBAM.getFile());
		inputSam.setValidationStringency(ValidationStringency.LENIENT);
			
		long recordsRead = 0;
		long readsInBed = 0;
		BamResults results = new BamResults();
		for (final SAMRecord samRecord : inputSam) {
			
			if (samRecord.getMappingQuality()==0) {
				results.mq0Reads++;
			}
			
			results.readLengthSum += samRecord.getReadLength();
			
			int midPoint = (samRecord.getAlignmentStart() + samRecord.getAlignmentEnd())/2;
			if (bed != null && (bed.contains(samRecord.getReferenceName().replace("chr", ""), samRecord.getAlignmentStart(), false) || bed.contains(samRecord.getReferenceName().replace("chr", ""), samRecord.getAlignmentEnd(), false) || bed.contains(samRecord.getReferenceName().replace("chr", ""),  midPoint, false))) {
				readsInBed++;
			}
			
			recordsRead++;
		}
		
		results.totalReadCount = recordsRead;
		results.totalReadsInBed = readsInBed;
		inputSam.close();
		return results;
	}
	
	public static void main(String[] args) throws IOException {
		DecimalFormat formatter = new DecimalFormat("0.00##");
		List<BamResults> results = new ArrayList<BamResults>();
		BEDFile bed = null;
		int start = 0;
		if (args[0].endsWith(".bed")) {
			bed = new BEDFile(new File(args[0]));
			bed.buildIntervalsMap(true);
			start = 1;
		}
		

		for(int i=start; i<args.length; i++) {
			File file = new File(args[i]);
			if (! file.exists()) {
				System.err.println("File " + file.getAbsolutePath() + " does not exist, skipping it");
				continue;
			}
			
			BamResults result = processBAMFile(bed, new BAMFile(file));
			results.add(result);
			System.out.println(args[i] + "\t" + formatter.format((double)result.totalReadsInBed/(double)result.totalReadCount) + "\t" + formatter.format(1.0 - (double)result.mq0Reads/(double)result.totalReadCount) + "\t" + formatter.format((double)result.readLengthSum/(double)result.totalReadCount));
		}
		
	}
	
	
	public static class BamResults {
		long mq0Reads = 0;
		long readLengthSum = 0;
		long totalReadCount = 0;
		long totalReadsInBed = 0;
	}
}
