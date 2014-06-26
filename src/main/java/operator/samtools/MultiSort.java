package operator.samtools;

import java.io.File;

import operator.MultiOperator;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;

public class MultiSort extends MultiOperator {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	public boolean requiresReference() {
		return false;
	}
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
		Object samPropsPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		String inputPath = inputBuffer.getAbsolutePath();
		int index = inputPath.lastIndexOf(".");
		String prefix = inputPath;
		if (index>0)
			prefix = inputPath.substring(0, index);
		prefix = prefix + ".sorted";

		String command = samtoolsPath + " sort " + inputBuffer.getAbsolutePath() + " " + prefix;
		
		String outputPath = prefix + ".bam";
		BAMFile outputBAM = new BAMFile(new File(outputPath), inputBuffer.getContig());
		addOutputFile(outputBAM);
		
		String command2 = samtoolsPath + " index " + outputBAM.getAbsolutePath();
		
		
		return new String[]{command, command2};
	}
}
