package operator.samtools;

import buffer.BCFFile;
import buffer.PileupFile;
import operator.CommandOperator;
import operator.OperationFailedException;
import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class ConvertPileupVCF extends PipedCommandOp {

	public static final String PATH = "path";
	protected String defaultBCFToolsPath = "bcftools";
	protected String bcftoolsPath = defaultBCFToolsPath;
	
	@Override
	protected String getCommand() throws OperationFailedException {
	
		Object samPath = getPipelineProperty(PipelineXMLConstants.BCFTOOLS_PATH);
		if (samPath != null)
			bcftoolsPath = samPath.toString();
		
		String samUserPath = properties.get(PATH);
		if (samUserPath != null) {
			bcftoolsPath = samUserPath;
		}
				
		
		String inputPath = getInputBufferForClass(BCFFile.class).getAbsolutePath();
				
		//Ouput handled automagically!
		String command = bcftoolsPath + " view -N -cvg " + inputPath;
		return command;
	}

}
