package operator.samtools;

import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Builds an index of the sequence in question using samtools' faidx command
 * @author brendan
 *
 */
public class SamFaidx extends CommandOperator {
	
	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() {
	
		Object samPropPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropPath != null)
			samtoolsPath = samPropPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		
		String command = samtoolsPath + " faidx " + inputPath;
		return command;
	}

}
