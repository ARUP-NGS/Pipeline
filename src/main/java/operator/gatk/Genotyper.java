package operator.gatk;

import operator.CommandOperator;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

public class Genotyper extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx16g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	public static final String CALLCONF = "call_conf";
	public static final String CALLEMIT = "call_emit";
	public static final String MININDELFRAC = "minIndelFrac";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected double standCallConf = 30.0;
	protected double standEmitConf = 10.0;
	protected String minIndelFrac = null;
	
	
	
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
		
		int threads = this.getPipelineOwner().getThreadCount();
		
		String threadsStr = properties.get(THREADS);
		if (threadsStr != null) {
			threads = Integer.parseInt(threadsStr);
		}
		
		String standCallConfString = properties.get(CALLCONF);
		if(standCallConfString != null){
			standCallConf = Double.parseDouble(standCallConfString);
		}
		
		String standEmitConfString = properties.get(CALLEMIT);
		if(standEmitConfString != null){
			standEmitConf = Double.parseDouble(standEmitConfString);
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
				
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + reference + " -I " + inputFile + " -T UnifiedGenotyper";
		command = command + " -o " + outputVCF;
		if (dbsnpFile != null)
			command = command + " --dbsnp " + dbsnpFile.getAbsolutePath();
		command = command + " -glm BOTH";
		command = command + " -stand_call_conf " + standCallConf;
		command = command + " -stand_emit_conf " + standEmitConf;
		command = command + " -nt " + threads;
		if (bedFile != null)
			command = command + " -L:intervals,BED " + bedFilePath;
		if (minIndelFrac != null)
			command = command + " -minIndelFrac " + minIndelFrac;
		return command;
	}

}
