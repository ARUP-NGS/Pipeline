package operator.picard;

import java.io.IOException;

import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FileBuffer;
import json.JSONException;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.PipelineXMLConstants;

/*
 * Converts a SAM/BAM file to a fastq as an alternative to Samtools.
 * @author daniel
 */

public class BamToFastq extends IOOperator {
	
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	public static final String JVM_ARGS="jvmargs";
	protected String picardDir = defaultPicardDir;
			
	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		FileBuffer inputBAM = this.getInputBufferForClass(BAMFile.class);
		FileBuffer outputFastq = this.getOutputBufferForClass(FastQFile.class);
		
		Object path = getPipelineProperty(PipelineXMLConstants.PICARD_PATH);
		if (path != null)
			picardDir = path.toString();
		
		if (picardDir.endsWith("/")) {
			picardDir = picardDir.substring(0, picardDir.length()-1);
		}
		
		String command = "java -jar -Xmx16G " + picardDir + "/SamToFastq.jar I=" + inputBAM.getAbsolutePath() + " F=" + outputFastq.getAbsolutePath();
		executeCommand(command);
		return;
	}
	
}