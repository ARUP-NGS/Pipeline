package operator.gatk;

import java.util.logging.Logger;

import operator.CommandOperator;

import pipeline.Pipeline;

import buffer.BAMFile;
import buffer.CSVFile;
import buffer.ReferenceFile;

/**
 * Performs table recalibration with the GATK
 * @author brendan
 *
 */
public class TableRecalibrator extends CommandOperator {

	public static final String GATK_PATH = "gatk.path";
	public static final String JVM_ARGS="jvmargs";
	public final String defaultMemOptions = " -Xms1g -Xmx8g";
	public static final String PATH = "path";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String getCommand() {
		
		Object propsPath = getPipelineProperty(GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
	
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		String recalDataFile = getInputBufferForClass(CSVFile.class).getAbsolutePath();
		
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) getPipelineProperty(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
		
		String recalBamFile = outputBuffers.get(0).getAbsolutePath();
				
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference + 
				" -I " + inputFile + 
				" -T TableRecalibration " + 
				" -o " + recalBamFile + 
				" -recalFile " + recalDataFile;
		return command;
	}

}
