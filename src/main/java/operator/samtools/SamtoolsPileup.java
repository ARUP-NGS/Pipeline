package operator.samtools;

import buffer.BAMFile;
import buffer.BCFFile;
import buffer.FileBuffer;
import buffer.PileupFile;
import buffer.ReferenceFile;
import buffer.SAMFile;
import operator.OperationFailedException;
import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class SamtoolsPileup extends PipedCommandOp {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() throws OperationFailedException {
	
		Object samPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPath != null)
			samtoolsPath = samPath.toString();
		
		String samUserPath = properties.get(PATH);
		if (samUserPath != null) {
			samtoolsPath = samUserPath;
		}
		
		
		
		String inputPath = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		FileBuffer reference = getInputBufferForClass(ReferenceFile.class);
		String refPath = "";
		if (reference != null){
			refPath =  reference.getAbsolutePath();
		}
		else {
			throw new OperationFailedException("No reference specified", this);
		}
		
		String outputTypeStr = "";
		FileBuffer outputBuffer = outputBuffers.get(0);
		if (outputBuffer instanceof BCFFile) {
			outputTypeStr = " -g ";
		}
		
		//Ouput handled automagically!
		String command = samtoolsPath + " mpileup -C50 -E " + outputTypeStr + " -f " + refPath + " " + inputPath;
		return command;
	}
	
}
