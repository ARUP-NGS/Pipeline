package operator.bamutils;

import java.util.logging.Logger;

import operator.OperationFailedException;
import pipeline.Pipeline;
import net.sf.samtools.SAMRecord;

/**
 * Splits a BAM file's records, failing only those whose
 * contigs' names have ContigNameFilter in them
 * and whose strandedness is not the input strandedness. 
 * Default strandedness: +
 * Default Name Filter: '""' (filters all contigs.)
 * 
 * @author daniel
 *
 */
public class StrandFilter extends BAMClassifier {
	public static final String STRAND = "strand";
	public static final String defaultStrand = "+";
	public static final String CONTIG_SUBSTR = "contig.substr";
	public static final String defaultContigSubstr = "";
	public ReturnRecord processRecord(SAMRecord samRecord) throws OperationFailedException {
		boolean value = readPasses(samRecord); // Initializing return value for
												// this function.
		ReturnRecord returnValue = new ReturnRecord(samRecord, value);
		return returnValue;
	}

	/**
	 * Fail read if it is on a contig meant to be filtered and it has the wrong strandedness
	 * @throws OperationFailedException 
	 * 
	 *
	 */
	public boolean readPasses(SAMRecord read) throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		String strand = defaultStrand;
		if (getAttribute(STRAND) != null) {
			strand = getAttribute(STRAND);
		}
		String contigSubstr = defaultContigSubstr;
		if (getAttribute(CONTIG_SUBSTR) != null){
			contigSubstr = getAttribute(CONTIG_SUBSTR);
		//	logger.info("contigSubstr set to " + contigSubstr + ". To filter reads mapped to all contigs, use an empty string.");
		}
		if(!strand.equals("+") && !strand.equals("-")) {
			throw new OperationFailedException("Strandedness must be of the for \"+\" or \"-\".", this);
		}
		//logger.info("Passing strand has been set to " + strand + ".");
		String chrom = read.getReferenceName();
		if(!chrom.toLowerCase().contains(contigSubstr.toLowerCase()))
			return true;
		boolean reverseStrand = read.getReadNegativeStrandFlag();
		if(reverseStrand){
			if(strand.equals("-"))
				return true;
			else
				return false;
		}
		if(strand.equals("+"))
			return true;
		else
			return false;
	}

}
