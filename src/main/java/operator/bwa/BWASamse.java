package operator.bwa;

import buffer.FileBuffer;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.PipelineXMLConstants;

/*
 * Wrapper for bwa's "old" aln/samse algorithm.
 * @author daniel
 */

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
		String outputPrefix = outputFile.getAbsolutePath().substring(0, outputFile.getAbsolutePath().lastIndexOf(".bam"));
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
			
		String referencePath = inputBuffers.get(0).getAbsolutePath();
		String readsSAI = inputBuffers.get(1).getAbsolutePath();
		String readsPath = inputBuffers.get(2).getAbsolutePath();

		String command = pathToBWA + " samse -f " + outputPrefix + ".sam " + referencePath + " " + readsSAI + " " + readsPath;
		try {
			executeCommand(command); // BWA Alignment
		}
		finally {
			System.out.println("Attempt to align complete.");
			executeCommand("touch " + outputPrefix + ".sam"); //Touch file to avoid null pointer exceptions being thrown
		}
		String Sam2Bam = samPath + " view -Sbh -o " + outputPrefix + ".bam " + outputPrefix + ".sam";
		try {
			executeCommand(Sam2Bam); //Converting sam to bam
		} finally {
			executeCommand("touch " + outputPrefix + ".bam");
			System.out.println("Attempt to convert ");
		}
		//Remove intermediate SAM file
		String RemoveSam = "rm " + outputPrefix + ".sam";
		System.out.println("Attempting to remove intermediate SAM file.");
		executeCommand(RemoveSam);
	}


}
