package operator.bwa;

import java.io.File;

import buffer.FileBuffer;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.PipelineXMLConstants;

public class BWASamse extends IOOperator {

	public static final String READ_GROUP = "readgroup";
	public static final String PATH = "path";
	public static final String SAMTOOLS_PATH = "samtools.path";
	protected String pathToBWA = "bwa";
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		String samPath = "samtools";
		String samAttr = this.getPipelineProperty(SAMTOOLS_PATH);
		if(samAttr != null) {
			samPath = samAttr;
		}
		Object propsPath = getPipelineProperty(PipelineXMLConstants.BWA_PATH);
		if (propsPath != null)
			pathToBWA = propsPath.toString();

		FileBuffer outputFile = this.getOutputBufferForClass(FileBuffer.class);
		File outputPath = outputFile.getFile();
		String outputPrefix = outputFile.getAbsolutePath().substring(0, outputFile.getAbsolutePath().lastIndexOf(".bam"));
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
			
		String referencePath = inputBuffers.get(0).getAbsolutePath();
		String readsSAI = inputBuffers.get(1).getAbsolutePath();
		String readsPath = inputBuffers.get(2).getAbsolutePath();

		String command = pathToBWA + " samse -f " + outputPrefix + ".sam " + referencePath + " " + readsSAI + " " + readsPath;
		executeCommand(command); // BWA Alignment
		String Sam2Bam = samPath + " view -Sbh " + outputPrefix + ".sam";
		executeCommandCaptureOutput(Sam2Bam, outputPath); //Converting sam to bam
	}


}
