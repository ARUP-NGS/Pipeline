package operator.picard;

import java.io.File;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import operator.CommandOperator;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Uses Picard to generate some alignment quality metrics
 * @author brendan
 *
 */
public class CollectMetrics extends CommandOperator {
	
	public static final String PATH = "path";
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	protected String picardDir = defaultPicardDir;
	
	public boolean requiresReference() {
		return true;
	}
	
	protected String[] getCommands() throws OperationFailedException {
		return new String[]{ getCommand() };
	}
	
	@Override
	protected String getCommand() throws OperationFailedException {
		Object path = getPipelineProperty(PipelineXMLConstants.PICARD_PATH);
		if (path != null)
			picardDir = path.toString();
		
		//User can override path specified in properties
		String userPath = properties.get(PATH);
		if (userPath != null) {
			 picardDir = userPath;
		}
		
		if (picardDir.endsWith("/")) {
			picardDir = picardDir.substring(0, picardDir.length()-1);
		}
		
		FileBuffer inputBAM = getInputBufferForClass(BAMFile.class);
		if (inputBAM == null) {
			throw new OperationFailedException("No input BAM files found for CollectMetrics ", this);
		}
		
		FileBuffer referenceFile = getInputBufferForClass(ReferenceFile.class);
		
		String inputPath = inputBAM.getAbsolutePath();
		
		File metricsDir = new File(getProjectHome() + "/metrics");
		if (! metricsDir.exists()) {
			metricsDir.mkdir();
		}
		
		String outputPrefix = inputBAM.getFilename().replace(".bam", ".metrics");
		
		String command = "java -Xms4g -Xmx16g -jar " + picardDir + "/CollectMultipleMetrics.jar " + 
				" INPUT=" + inputPath + 
				" VALIDATION_STRINGENCY=LENIENT " +
				" REFERENCE_SEQUENCE=" + referenceFile.getAbsolutePath() +
				" OUTPUT=" + metricsDir.getAbsolutePath() + "/" + outputPrefix +
				" PROGRAM=CollectAlignmentSummaryMetrics " +
				" PROGRAM=CollectInsertSizeMetrics";

		return command;
	}


}
