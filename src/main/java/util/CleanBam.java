package util;

import java.io.File;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileReader.ValidationStringency;

/**
 * Simple BAM (or .SAM) file cleaner that generates a new bam (or .sam) file after filtering
 * out reads that do not pass the following quality checks:
 *   1. No reads with unmapped mates
 *   2. No unmapped reads
 *   3. No reads with duplicate flag marked
 *   4. No reads not passing vendor quality checks
 * @author brendan
 *
 */
public class CleanBam {
	
	public static void cleanBam(File inputBAM, File outputBAM) {

		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		final SAMFileReader inputSam = new SAMFileReader(inputBAM);
		inputSam.setValidationStringency(ValidationStringency.LENIENT);
		
		final SAMFileWriter outputSam = new SAMFileWriterFactory().makeSAMOrBAMWriter(inputSam.getFileHeader(),
				true, outputBAM);

		int readCount = 0;
		int outReadCount = 0;
		for (final SAMRecord samRecord : inputSam) {
			readCount++;
			
			if ( !samRecord.getMateUnmappedFlag() && (!samRecord.getReadUnmappedFlag()) && (!samRecord.getReadFailsVendorQualityCheckFlag()) && (!samRecord.getDuplicateReadFlag())) {
				outputSam.addAlignment(samRecord);
				outReadCount++;
			}
		}

		outputSam.close();
		inputSam.close();

		System.out.println("Wrote " + outReadCount + " of " + readCount + " total reads");
	}
	
	public static void main(String[] args) {
		
		if (args.length != 2) {
			System.out.println("Remove reads with unmapped mates, zero mapping quality reads, and reads flagged as duplicates, and reads not passing vendor quality check");
			System.out.println("Usage : input.bam output.bam");
			return;
		}

		CleanBam.cleanBam(new File(args[0]), new File(args[1]));
		
	}
}
