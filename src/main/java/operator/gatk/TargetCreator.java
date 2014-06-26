package operator.gatk;

import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/**
 * Creates indel realignment targets for use with the IndelRealigner
 * @author brendan
 *
 */
public class TargetCreator extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String JVM_ARGS="jvmargs";
	public static final String PATH = "path";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String getCommand() {
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		
		FileBuffer bedFile = getInputBufferForClass(BEDFile.class);
		String bedFilePath = "";
		if (bedFile != null) {
			bedFilePath = bedFile.getAbsolutePath();
		}
		
		String indelIntervalsFile = outputBuffers.get(0).getAbsolutePath();
		
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) getPipelineProperty(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference + 
				" -I " + inputFile + 
				" -T RealignerTargetCreator -o " + indelIntervalsFile;
		if (bedFile != null)
			command = command + " -L:intervals,BED " + bedFilePath;
		return command;
	}

}
