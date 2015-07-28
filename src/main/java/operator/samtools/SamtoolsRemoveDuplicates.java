package operator.samtools;

import java.util.logging.Logger;

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
	public static final String TREAT_PAIRS_AS_SINGLE = "treat.pairs.as.single";

	protected boolean treatPairsAsSingle = true;
	
	@Override
	protected String getCommand() {
	
		Object samPropsPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
	
		
		String treatPairsAsSingleAttr = this.getAttribute(TREAT_PAIRS_AS_SINGLE);
		if (treatPairsAsSingleAttr != null) {
			Boolean treatAsSingle = Boolean.parseBoolean(treatPairsAsSingleAttr);
			treatPairsAsSingle = treatAsSingle;
			Logger.getLogger(Pipeline.primaryLoggerName).info("Treating pairs as single : " + treatPairsAsSingle);
		}
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String pairs = "";
		if (treatPairsAsSingle) {
			pairs = " -S ";
		}
		
		String command = samtoolsPath + " rmdup " + pairs + inputPath + " " + outputPath;
		return command;
	}

}
