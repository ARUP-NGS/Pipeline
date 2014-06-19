package operator.bwa;

import java.io.File;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.PipelineXMLConstants;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.SAIFile;

/*
 * @author daniel
 * Adds control of output SAI file to the run xml.
 */

public class BWAalnD extends IOOperator {

	public static final String PATH = "path";
	public static final String THREADS = "threads";
	protected String pathToBWA = "bwa";
	protected int defaultThreads = 4;
	protected int threads = defaultThreads;
	String defaultBWAPath = "bwa";

	@Override
	public boolean requiresReference() {
		return true;
	}

	@Override
	public void performOperation() throws OperationFailedException {
		Object propsPath = getPipelineProperty(PipelineXMLConstants.BWA_PATH);
		if (propsPath != null)
			pathToBWA = propsPath.toString();

		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}

		String threadsAttr = properties.get(THREADS);
		if (threadsAttr != null) {
			threads = Integer.parseInt(threadsAttr);
		}
		FileBuffer saiAttr = this.getOutputBufferForClass(SAIFile.class);
		File saiPath = saiAttr.getFile();
		FileBuffer reference = this.getInputBufferForClass(ReferenceFile.class);
		String referencePath = reference.getAbsolutePath();
		FileBuffer reads = this.getInputBufferForClass(FastQFile.class);
		String readsPath = reads.getAbsolutePath();
		String command_str = pathToBWA + " aln -t " + threads + " -f "
				+ saiPath + " " + referencePath + " " + readsPath;
		System.out.println(command_str);
		executeCommand(command_str);

	}

}
