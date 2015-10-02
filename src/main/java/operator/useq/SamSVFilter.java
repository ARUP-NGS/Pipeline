package operator.useq;

import java.util.logging.Logger;

import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.BEDFile;
import buffer.BAMFile;

/**
 * Implements USeq's SamFilter program.
 * Parses a BAM file into spanning, single, and soft-masked alignment groups.
 *
 *   
 * @author daniel/elainegee
 * 
 */
public class SamSVFilter extends CommandOperator {
	
	public static final String USEQ_DIR = "useq.dir";
	public static final String JVM_ARGS="jvmargs";
	public static final String SAMSV_DIR="samsv.dir";
	public static final String MEMORY_RANGE="memory.range";
	public static final String SAMSV_OPT="samsv.options";
	protected String defaultUSeqDir = "/mnt/research2/Daniel/bin/jar/USeq_8.7.8/";
	protected String memoryRange = " -Xms2G -Xmx8G ";
	@Override
	protected String getCommand() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		int threads = this.getPipelineOwner().getThreadCount();
		String memoryAttr = properties.get(MEMORY_RANGE);
		if(memoryAttr != null){
			memoryRange = memoryAttr;
			logger.info("Default memory range overridden. New value: " + memoryRange + ".");
		}
		String useqPath = defaultUSeqDir;
		String useqAttr = getPipelineProperty(USEQ_DIR);
		if(useqAttr != null) {
			useqPath = useqAttr;
			logger.info("Default USeq location overridden. New value: " + useqPath + ".");
		}
		String inputBam = this.getInputBufferForClass(BAMFile.class).getAbsolutePath();
		String bedRegion = this.getInputBufferForClass(BEDFile.class).getAbsolutePath();
		String bedString = " -b " + bedRegion;
		if(bedRegion==null) {
			logger.info("No BED file provided for SamSVFilter");
		        throw new OperationFailedException("BED file is required for SamSVFilter", this);
		}
		String outputPath = this.getAttribute(SAMSV_DIR);
		if(outputPath==null)
			throw new OperationFailedException("No SV directory provided. Cannot proceed without it.",this);
		String SVoptions = this.getAttribute(SAMSV_OPT);
		if(SVoptions==null)
			SVoptions = "";
		//User can override path specified in properties
		String userPath = properties.get(USEQ_DIR);
		if (userPath != null) {
			 useqPath = userPath;
		}
		
		if (useqPath.endsWith("/")) {
			useqPath = useqPath.substring(0, useqPath.length()-1);
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
		//Trim the outputPath so that the sample name and/or random string can be added
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length()-1);
		}
		logger.info("Now attempting to parse " + inputBam + " into spanning, single, and soft-masked alignment groups with " + threads + " threads");
		String command = "java " + memoryRange + " " + jvmARGStr + " -jar " + useqPath + "/SamSVFilter -s " + outputPath +" -a "+ inputBam + bedString + SVoptions;
		
		return command;
	}

}
