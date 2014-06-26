package operator.samtools;

import java.io.File;

import operator.MultiOperator;

import org.apache.log4j.Logger;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;

public class MultiRemoveDuplicates extends MultiOperator {

	public static final String TREAT_PAIRS_AS_SINGLE = "treat.pairs.as.single";
	public static final String PATH = "path";
	public static final String FORCESINGLEEND = "force.single.end";

	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	protected boolean treatPairsAsSingle = true;

	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
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
		
		String inputPath = inputBuffer.getAbsolutePath();
		int index = inputPath.lastIndexOf(".");
		String prefix = inputPath;
		if (index>0)
			prefix = inputPath.substring(0, index);
		String outputPath = prefix + ".dedup.bam";
		
		BAMFile outputBAM = new BAMFile(new File(outputPath), inputBuffer.getContig());
		addOutputFile(outputBAM);
		

		String treatAsSingleStr = "";
		if (treatPairsAsSingle) {
			treatAsSingleStr = " -S ";
		}

		
		String command = samtoolsPath + " rmdup " + treatAsSingleStr + inputPath + " " + outputPath;
		return new String[]{command};
	}

}
