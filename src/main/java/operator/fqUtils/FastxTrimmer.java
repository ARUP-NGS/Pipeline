package operator.fqUtils;

import java.util.logging.Logger;

import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.FastQFile;
/*
 * Trims fastq records to a directed length
 * @author daniel
 */
public class FastxTrimmer extends CommandOperator {
	public static final String TRIM_NUM = "trim.num";
    public static final String FASTX_TK = "fastx.tk";
	@Override
	protected final String getCommand() throws OperationFailedException {
		String inputFastq = getInputBufferForClass(FastQFile.class)
				.getAbsolutePath();
		String outputFastq = getOutputBufferForClass(FastQFile.class)
				.getAbsolutePath();

		String trim = getAttribute(TRIM_NUM);
		if (trim == null) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Last nucleotide to keep has not been set. Please set it - 30 is typical.");
			throw new OperationFailedException("Number of nucleotides to keep is required for FastxTrimmer operator", null);
		}
		String fastxTrim = getPipelineProperty(FASTX_TK);
		
		String command_str = fastxTrim + " -l " + trim + " -i " 
				+ inputFastq + " -o " + outputFastq;
		return command_str;
	}

}
