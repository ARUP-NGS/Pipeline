package operator.fqUtils;

import java.io.IOException;
import java.util.logging.Logger;

import json.JSONException;
import operator.CommandOperator;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.FastQFile;
/*
 * Trims fastq records to a directed length
 * @author daniel
 */
public class FastxTrimmer extends IOOperator {
	public static final String TRIM_NUM = "trim.num";
    public static final String FASTX_TRIMMER = "fastx.trimmer";
	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

		String inputFastq = getInputBufferForClass(FastQFile.class)
				.getAbsolutePath();

		String outputFastq = getOutputBufferForClass(FastQFile.class)
				.getAbsolutePath();

		String trim = getAttribute(TRIM_NUM);
		if (trim == null) {
			logger.info("Last nucleotide to keep has not been set. Please set it - 30 is typical.");
			throw new OperationFailedException("Number of nucleotides to keep is required for FastxTrimmer operator", null);
		}
		String fastxTrim = getPipelineProperty(FASTX_TRIMMER);
		
		if(fastxTrim==null) {
			logger.info("No location for fastx_trimmer provided.");
			throw new OperationFailedException("No path to fastx_trimmer provided. Abort mission!", this);
		}
		String command_str;
		if(!inputFastq.substring(inputFastq.length()-2, inputFastq.length()).equals("gz")){
			command_str = fastxTrim + " -Q33 -l " + trim + " -i " 
					+ inputFastq + " -o " + outputFastq;	
			executeCommand(command_str);
		}
		else {
			logger.info("Fastq file is gzipped. Unzipping to facilitate fastx_trimmer.");
			executeCommand("gunzip " + inputFastq);
			inputFastq = inputFastq.substring(0, inputFastq.length()-3);
			command_str = fastxTrim + " -Q33 -l " + trim + " -i " 
					+ inputFastq + " -o " + outputFastq;	
			executeCommand(command_str);
			executeCommand("gzip " + inputFastq);
		}
	}

}
