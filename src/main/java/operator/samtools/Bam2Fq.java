package operator.samtools;

import java.io.File;

import operator.IOOperator;
import operator.OperationFailedException;
import buffer.BAMFile;
import buffer.FileBuffer;

/*
 * Converts Bam to Fastq
 * Needed for RNA translocation pipeline. 
 * 
 */

public class Bam2Fq extends IOOperator {
	public static final String SAMTOOLS_PATH = "samtools.path";
	@Override
	public void performOperation() throws OperationFailedException {
		String inputBam = this.getInputBufferForClass(BAMFile.class).getAbsolutePath();
		File outputFastq = this.getOutputBufferForClass(FileBuffer.class).getFile();
		String defaultSamPath = "samtools";
		String samPath = defaultSamPath;
		String samAttr = this.getPipelineProperty(SAMTOOLS_PATH);
		if (samAttr != null) {
			samPath = samAttr;
		}
		String command_str = samPath + " bam2fq " + inputBam ;
		executeCommandCaptureOutput(command_str, outputFastq);
		return;
	}

	

}