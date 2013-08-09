package operator.samtools;

import java.util.logging.Logger;

import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.SAMFile;
import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Uses samtools to convert a SAM file to a BAM file
 * @author brendan
 *
 */
public class ConvertSamBam extends PipedCommandOp {


	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() {
	
		Object samPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPath != null)
			samtoolsPath = samPath.toString();
		
		String samUserPath = properties.get(PATH);
		if (samUserPath != null) {
			samtoolsPath = samUserPath;
		}
		
		
		
		String inputPath = getInputBufferForClass(SAMFile.class).getAbsolutePath();
		FileBuffer reference = getInputBufferForClass(ReferenceFile.class);
		String refPath = "";
		if (reference != null){
			refPath = " -t " + reference.getAbsolutePath() + " ";
		}
		else {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("No reference file provided for BAM conversion, not using reference");
		}
		
		//Ouput handled automagically!
		String command = samtoolsPath + " view " + refPath + " -Sb " + inputPath;
		return command;
	}

}
