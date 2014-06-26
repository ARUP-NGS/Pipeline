package util.bamUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import buffer.BAMFile;

/**
 * Static class for some helpful bam utilities, including things that count reads in various ways
 * @author brendan
 *
 */
public class ReadCounter {

	/**
	 * Returns a Map of chromosome name to number of reads that map to it with mapping quality >= minMQScore
	 * Reads with MQ strictly less than the score provided are ignored.
	 * @param inputBAM
	 * @param minMQScore
	 * @return
	 */
	public static Map<String, Long> countReadsByChromosome(BAMFile inputBAM, int minMQScore) {
		
		final SAMFileReader inputSam = new SAMFileReader(inputBAM.getFile());
		inputSam.setValidationStringency(ValidationStringency.LENIENT);
		
		Map<String, Long> readCounts = new HashMap<String, Long>();
		
		for (final SAMRecord samRecord : inputSam) {
			
			String chr = samRecord.getReferenceName();
			int mq = samRecord.getMappingQuality();
			if (mq >= minMQScore) {
				Long count = readCounts.get(chr);
				if (count == null) {
					count = 0L;
				}
				readCounts.put(chr, count+1);
			}
		}
		
		inputSam.close();
		
		for(String chr : readCounts.keySet()) {
			System.out.println(chr + " : " + readCounts.get(chr));
		}
		
		return readCounts;
	}
	
	
	public static void main(String[] args) {
		
		File bam = new File("/home/brendan/DATA2/NA12878/snaptest.final.bam");
		Map<String, Long> count = countReadsByChromosome(new BAMFile(bam), 0);
		
	}
}
