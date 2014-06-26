package operator.samtools;

import buffer.BAMFile;
import buffer.FileBuffer;
import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Use samtools to index the given input file
 * @author brendan
 *
 */
public class Index extends CommandOperator {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() throws OperationFailedException {
		//Obtain samtools path from static Pipeline properties
		Object samPropsPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		//Obsolete: look for path string in this operators properties and use it if specified
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		//Obtain input bam file from input files list (ignore other input files)
		FileBuffer inputBuffer = getInputBufferForClass(BAMFile.class);
		
		//A bit of error checking..
		if (! inputBuffer.getFile().exists()) {
			throw new OperationFailedException("Input buffer " + inputBuffer.getAbsolutePath() + " does not exist!" , this);
		}
		
		//Build command string, this one is pretty easy
		String command = samtoolsPath + " index " + inputBuffer.getAbsolutePath();
		return command;
	}

}
