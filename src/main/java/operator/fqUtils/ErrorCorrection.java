package operator.fqUtils;



/**
 * Implements lighter, an error correction program for Illumina reads
 * @author daniel
 * knum sets the length of kmers for the program, while
 * alpha sets a somewhat arbitrary parameter for the process.
 * You can change them in the run xml, but it is discouraged unless
 * you know what you are doing.
 * For a file with filename "Filename.fastq", the output of this program
 * is a file with filename "Filename.cor.fq". It cannot be redirected to stdout
 */
import operator.CommandOperator;
import operator.OperationFailedException;
import buffer.FastQFile;

public class ErrorCorrection extends CommandOperator {
	public static final String LIGHTER = "lighter";
	public static final String THREADS = "threads";
	public static final String KNUM = "knum";
	public static final String GENOME_SIZE = "genome.size";
	public static final String ALPHA_PARAMETER = "alpha.parameter";

	@Override
	protected final String getCommand() throws OperationFailedException {
		String inputFastq = getInputBufferForClass(FastQFile.class)
				.getAbsolutePath();

		String genomeSize = "3150000000";
		String genomeGet = getAttribute(GENOME_SIZE);
		if (genomeGet != null) {
			genomeSize = genomeGet;
		}

		String alpha = "0.1";
		String alphaGet = getAttribute(ALPHA_PARAMETER);
		if (alphaGet != null) {
			alpha = alphaGet;
		}

		String knum = "12"; // Set default k. Changes if set in run xml.
		String kGet = getAttribute(KNUM);
		if (kGet != null) {
			knum = kGet;
		}

		String threads = "10"; // Set default # of threads
		String threadsGet = getPipelineProperty(THREADS);
		if (threadsGet != null) {
			threads = threadsGet;
		}

		String lighter = "lighter"; //Default assumes lighter is on path
		if (lighter != null) {
			lighter = properties.get(LIGHTER);
		}
		String parameters = " -t " + threads + " -k " + knum + " " + genomeSize + " " + alpha;
		String commandStr = lighter + " -r " + inputFastq + parameters + " -all";
		return commandStr;
	}

}