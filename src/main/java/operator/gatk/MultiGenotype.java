package operator.gatk;

import java.io.File;
import java.util.logging.Logger;

import operator.MultiOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.IntervalsFile;
import buffer.VCFFile;

/**
 * Parallel version of gatk's unified genotyper
 * @author brendan
 *
 */
public class MultiGenotype extends MultiOperator {

	public final String defaultMemOptions = " -Xms512m -Xmx2g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	public static final String CALL_CONF = "call_conf";
	public static final String EMIT_CONF = "emit_conf";
	protected int minIndelCount = 2; //Minimum number of consensus indels that must be present for an indel call
	protected double minIndelFrac = 0.2; //Minimum fraction of consensus indels that must be present for an indel call
	protected double emitConf = 10.0;
	protected double callConf = 30.0;
	
	protected IntervalsFile captureFile = null;
	
	public int getPreferredThreadCount() {
		//Dont use more than 12 threads...
		return Math.min(getPipelineOwner().getThreadCount(), 10);
	}
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		if (captureFile == null) {
			captureFile = (BEDFile) getInputBufferForClass(BEDFile.class);
		}
		
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
		
		String callConfStr = properties.get(CALL_CONF);
		if (callConfStr != null) {
			callConf = Double.parseDouble(callConfStr);
			System.out.println("Setting call threshold to : " + emitConf);
			logger.info( "Setting call threshold to : " + emitConf );
		}
		
		String emitConfStr = properties.get(EMIT_CONF);
		if (emitConfStr != null) {
			emitConf = Double.parseDouble(emitConfStr);
			System.out.println("Setting emit threshold to : " + emitConf);
			logger.info( "Setting emit threshold to : " + emitConf );
		}
		
		
		
		String inputPath = inputBuffer.getAbsolutePath();
		FileBuffer dbsnpFile = getInputBufferForClass(VCFFile.class);
			
		FileBuffer bedFile = getInputBufferForClass(BEDFile.class);
		String bedFilePath = "";
		if (bedFile != null) {
			bedFilePath = bedFile.getAbsolutePath();
		}
		
		int index = inputPath.lastIndexOf(".");
		String prefix = inputPath;
		if (index>0)
			prefix = inputPath.substring(0, index);
		String outputVCFPath = prefix + ".vcf";
		FileBuffer vcfBuffer = new VCFFile(new File(outputVCFPath), inputBuffer.getContig());
		addOutputFile( vcfBuffer );
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + reference.getAbsolutePath() + 
				" -I " + inputPath + 
				" -T UnifiedGenotyper";
		command = command + " -o " + outputVCFPath;
		if (dbsnpFile != null)
			command = command + " --dbsnp " + dbsnpFile.getAbsolutePath();
		command = command + " -glm BOTH";
		command = command + " -minIndelCnt " + minIndelCount;
		command = command + " -minIndelFrac " + minIndelFrac;
		command = command + " -stand_call_conf " + callConf + " ";
		command = command + " -stand_emit_conf " + emitConf + " ";
		
		if (captureFile != null) {
			command = command + " -L:intervals,BED " + captureFile.getAbsolutePath() + " ";
		}
		
		if (captureFile == null && inputBuffer.getContig() != null) {
			command = command + " -L " + inputBuffer.getContig() + " ";
		}
		
		if (inputBuffer.getContig() == null && bedFile != null)
			command = command + " -L:intervals,BED " + bedFilePath;
		
		
		return new String[]{command};
	}

	
}

