package operator.samtools;

import java.io.File;
import java.util.logging.Logger;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.SAMFile;
import operator.MultiOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class MultiConvertBam extends MultiOperator {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
		Object samPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPath != null)
			samtoolsPath = samPath.toString();
		
		String samUserPath = properties.get(PATH);
		if (samUserPath != null) {
			samtoolsPath = samUserPath;
		}
		
		String inputPath = inputBuffer.getAbsolutePath();
		FileBuffer reference = getInputBufferForClass(ReferenceFile.class);
		String refPath = "";
		if (reference != null){
			refPath = " -t " + reference.getAbsolutePath() + " ";
		}
		else {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("No reference file provided for BAM conversion, not using reference");
		}
		
		int index = inputPath.lastIndexOf(".");
		String prefix = inputPath;
		if (index>0)
			prefix = inputPath.substring(0, index);
		String outputPath = prefix + ".bam";
		
		BAMFile outputBAM = new BAMFile(new File(outputPath), inputBuffer.getContig());
		addOutputFile(outputBAM);
		
		//Ouput handled automagically!
		String command1 = samtoolsPath + " view " + refPath + " -Sb " + inputPath + " -o " + outputPath;
		String command2 = samtoolsPath + " index " + outputPath;
		return new String[]{command1, command2};
	}

}
