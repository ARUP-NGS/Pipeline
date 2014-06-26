package operator.gatk;

import operator.CommandOperator;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.CSVFile;
import buffer.ReferenceFile;

public class IndelRealign extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String JVM_ARGS="jvmargs";
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
		String intervalsFile = getInputBufferForClass(CSVFile.class).getAbsolutePath();
		
		String realignedBam = outputBuffers.get(0).getAbsolutePath();
		
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) getPipelineProperty(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
		if (!jvmARGStr.contains("java.io.tmpdir"))
			jvmARGStr =jvmARGStr + " -Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir");
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath 
				+ " -R " + reference 
				+ " -I " + inputFile
				+ " -rf BadCigar "
				+ " -T IndelRealigner -targetIntervals " 
				+ intervalsFile + " -o " + realignedBam;
		return command;
	}

}
