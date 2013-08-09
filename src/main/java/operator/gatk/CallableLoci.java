package operator.gatk;

import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/**
 * Interface to the CallableLociWalker of the GATK. Un-callable regions are put into a BED-like CSV file
 * @author brendan
 *
 */
public class CallableLoci extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx16g";
	public static final String MIN_DEPTH = "min.depth";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected int minDepth = -1; //-1 is the GATK default
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String getCommand() throws OperationFailedException {
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		String minDepthAttr = this.getAttribute(MIN_DEPTH);
		if (minDepthAttr != null) {
			Integer minD = Integer.parseInt(minDepthAttr);
			this.minDepth = minD;
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
		
		
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();	
		FileBuffer bedFile = getInputBufferForClass(BEDFile.class);
		String bedFilePath = "";
		if (bedFile != null) {
			bedFilePath = bedFile.getAbsolutePath();
		}
		
		FileBuffer outputFile = getOutputBufferForClass(CSVFile.class);
		if (outputFile == null) {
			throw new OperationFailedException("No output file specified", this);
		}
		
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + reference + " -I " + inputFile + " -T CallableLoci ";
		command = command + " --minDepth " + minDepth;
		command = command + " -o " + outputFile.getAbsolutePath();
		command = command + " -summary " + outputFile.getFilename() + ".summary ";
		if (bedFile != null)
			command = command + " -L:intervals,BED " + bedFilePath;
		return command;
	}

}
