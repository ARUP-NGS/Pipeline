package operator.samtools;

import operator.MultiOperator;
import pipeline.PipelineXMLConstants;
import buffer.FileBuffer;

/**
 * Uses samtools to index all files
 * @author brendan
 *
 */
public class MultiIndex extends MultiOperator {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
		Object samPropsPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		String command = samtoolsPath + " index " + inputBuffer.getAbsolutePath();
		return new String[]{command};
	}

}
