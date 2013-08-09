package operator.samtools;

import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Use samtools to remove duplicates from the input file
 * @author brendan
 *
 */
public class SamtoolsRemoveDuplicates extends PipedCommandOp {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() {
	
		Object samPropsPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String fileIsSam = "";
		if (inputPath.endsWith("sam"))
			fileIsSam = " -S ";
		
		String command = samtoolsPath + " rmdup " + fileIsSam + inputPath + " " + outputPath;
		return command;
	}

}
