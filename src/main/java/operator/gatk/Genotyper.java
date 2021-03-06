package operator.gatk;

import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

public class Genotyper extends IOOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx16g";
	public static final String GATK_PATH = "gatk.path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	public static final String CALLCONF = "call_conf";
	public static final String CALLEMIT = "call_emit";
	public static final String MININDELFRAC = "minIndelFrac";
	public static final String OUT_MODE = "out_mode";
	public static final String EMIT_ALL_SITES = "EMIT_ALL_SITES";
	public static final String DOWNSAMPLE_TO_COV = "downsample_to_coverage";
	public static final String PERMIT_NONZERO = "permit.nonzero";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected double standCallConf = 30.0;
	protected double standEmitConf = 10.0;
	protected String minIndelFrac = null;
	protected String outMode = null;
	protected String downsampleToCoverage = null;
	String PermitNonzero = "false";
		
	
	
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(GATK_PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		int threads = this.getPipelineOwner().getThreadCount();
		
		String threadsStr = properties.get(THREADS);
		if (threadsStr != null) {
			threads = Integer.parseInt(threadsStr);
		}
		
		String standCallConfString = properties.get(CALLCONF);
		if(standCallConfString != null){
			standCallConf = Double.parseDouble(standCallConfString);
		}
	
		Boolean AcceptNonzero = false;
		String PermitAttr = this.getAttribute(PERMIT_NONZERO);
		if (PermitAttr != null){
			if(PermitAttr.equalsIgnoreCase("true")) {
				AcceptNonzero = true;
				logger.info("Non-zero return status has been set as permitted.");
			}
			else if(PermitAttr.equalsIgnoreCase("false")) {
				AcceptNonzero = false;
				logger.info("Non-zero return status has been set as not permitted.");
			}
			else {
				logger.info("Non-zero return status permittability has not been set. Assuming the default: false.");
			}
		}
		
		String standEmitConfString = properties.get(CALLEMIT);
		if(standEmitConfString != null){
			standEmitConf = Double.parseDouble(standEmitConfString);
		}
		
		String outModeString = properties.get(OUT_MODE);
		if(outModeString != null){
			outMode = outModeString;
		}
		
		String downsampleToCoverageString = properties.get(DOWNSAMPLE_TO_COV);
		if(downsampleToCoverageString != null){
			downsampleToCoverage = downsampleToCoverageString;
		}
		
		String minIndelFracString = properties.get(MININDELFRAC);
		if(minIndelFracString != null){
			minIndelFrac = minIndelFracString;
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
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		FileBuffer dbsnpFile = getInputBufferForClass(VCFFile.class);
			
		FileBuffer bedFile = getInputBufferForClass(BEDFile.class);
		String bedFilePath = "";
		if (bedFile != null) {
			bedFilePath = bedFile.getAbsolutePath();
		}
		
		String outputVCF = outputBuffers.get(0).getAbsolutePath();
		
		boolean emitAllSites = false;
		// gzip the output file if emitting all sites
		if(EMIT_ALL_SITES.equals(outMode)){
			emitAllSites = true;
		}
		if(emitAllSites){
			outputVCF = outputVCF.substring(0, outputVCF.lastIndexOf(".vcf")) + "-all_sites.vcf.gz";
		}
				
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + reference + " -I " + inputFile + " -T UnifiedGenotyper";
		command = command + " -o " + outputVCF;
		if (dbsnpFile != null)
			command = command + " --dbsnp " + dbsnpFile.getAbsolutePath();
		command = command + " -glm BOTH";
		command = command + " -stand_call_conf " + standCallConf;
		command = command + " -stand_emit_conf " + standEmitConf;
		command = command + " -rf BadCigar ";
		command = command + " -nt " + threads;
		if(emitAllSites){
			command += " -out_mode EMIT_ALL_SITES ";
		}
		if (bedFile != null)
			command = command + " -L:intervals,BED " + bedFilePath;
		if (minIndelFrac != null)
			command = command + " -minIndelFrac " + minIndelFrac;
		if (downsampleToCoverage != null)
			command = command + " --downsample_to_coverage " + downsampleToCoverage;
		executeCommand(command, AcceptNonzero);
		return;
	}

}
