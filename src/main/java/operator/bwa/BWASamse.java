package operator.bwa;

import java.io.File;

import buffer.FileBuffer;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.PipelineXMLConstants;

public class BWASamse extends IOOperator {

	public static final String READ_GROUP = "readgroup";
	public static final String PATH = "path";
	protected String pathToBWA = "bwa";
	
	protected String defaultRG = "@RG\\tID:unknown\\tSM:unknown\\tPL:ILLUMINA";
	protected String readGroupStr = defaultRG;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.BWA_PATH);
		if (propsPath != null)
			pathToBWA = propsPath.toString();

		File outputFile = this.getOutputBufferForClass(FileBuffer.class).getFile();
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
			
		String rgStr = properties.get(READ_GROUP);
		if (rgStr != null) {
			readGroupStr = rgStr;
		}
		String referencePath = inputBuffers.get(0).getAbsolutePath();
		String readsSAI = inputBuffers.get(1).getAbsolutePath();
		String readsPath = inputBuffers.get(2).getAbsolutePath();

		String command = pathToBWA + " sampe -r " + readGroupStr + " " + referencePath + " " + readsSAI + " " + readsPath;
		executeCommandCaptureOutput(command, outputFile);
		return;		
	}


}
