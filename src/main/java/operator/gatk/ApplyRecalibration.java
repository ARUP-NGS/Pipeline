package operator.gatk;

import buffer.CSVFile;
import buffer.ReferenceFile;
import buffer.VCFFile;
import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Apply a recalibration (probably created with the VariantRecalibrator
 * @author brendan
 *
 */
public class ApplyRecalibration extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected int threads = 1;
	
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
		
		String threadsStr = properties.get(THREADS);
		if (threadsStr != null) {
			threads = Integer.parseInt(threadsStr);
		}
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null)
			jvmARGStr = "";
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(VCFFile.class).getAbsolutePath();
		String inputRecal = getInputBufferForClass(CSVFile.class).getAbsolutePath();
			
		
		String outputVCF = outputBuffers.get(0).getAbsolutePath();
				
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + reference + " -input " + inputFile + " -T ApplyRecalibration";		
		command = command + " --ts_filter_level 99.0 ";
		command = command + " -tranchesFile output.tranches ";
		command = command + " -recalFile " + inputRecal + " ";
		command = command + " -o " + outputVCF;
		
		   
		return command;
	}
}
