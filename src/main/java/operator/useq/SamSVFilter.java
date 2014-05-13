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
 * Also generates counts for each possible translocation
 *
 *   
 * @author daniel
 * 
 */
public class SamSVFilter extends CommandOperator {

	
	public static final String USEQ_DIR = "useq.dir";
	public static final String JVM_ARGS="jvmargs";
	public static final String SAMSV_DIR="samsv.dir";
	protected String defaultUSeqDir = "/mnt/research2/Daniel/bin/jar/USeq-8.7.8/";
	@Override
	protected String getCommand() throws OperationFailedException {
		
		int threads = this.getPipelineOwner().getThreadCount();
		String useqPath = defaultUSeqDir;
		String inputBam = this.getInputBufferForClass(BAMFile.class).getAbsolutePath();
		String bedRegion = this.getInputBufferForClass(BEDFile.class).getAbsolutePath();
		String bedString = " -b " + bedRegion;
		if(bedRegion==null) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("No BED file provided for SamSVFilter");
		        throw new OperationFailedException("BED file is required for this operation", null);
		}
		String outputPath = this.getAttribute(SAMSV_DIR);
		Logger.getLogger(Pipeline.primaryLoggerName).info("Parsing " + inputBam + " into spanning, single, and soft-masked alignment groups with " + threads + " threads");
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
		String command = "java -Xms2G -Xmx20G " + jvmARGStr + " -jar " + useqPath + "/SamSVFilter -s " + outputPath +" -a "+ inputBam + bedString;
		return command;
	}

}
