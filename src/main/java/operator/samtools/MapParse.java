package operator.samtools;

import java.util.List;

import operator.IOOperator;
import operator.OperationFailedException;

import org.apache.log4j.Logger;

import buffer.BAMFile;
import buffer.FileBuffer;

/*
 * MapParse parses a BAM file into two output BAM files - mapped and unmapped
 * This is used in the RNA gene fusion/translocation pipeline.
 * @author daniel
 */

public class MapParse extends IOOperator {

	public static final String SAMTOOLS_PATH = "samtools.path";

	String defaultSamPath = "samtools";
	
	@Override
	public void performOperation() throws OperationFailedException {
		String inputBAM = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		List<FileBuffer> outputBuffers = this
				.getAllOutputBuffersForClass(FileBuffer.class);
		System.out.println(outputBuffers.size() + " is the number of output buffers provided.");
		System.out.println(inputBAM + " is the original input BAM location.");
		if (outputBuffers.size() != 2) {
			throw new IllegalArgumentException(
					"Exactly 2 output buffers required - the first for mapped reads, the second for unmapped reads.");
		}

		String samPath = defaultSamPath;
		String samAttr = this.getPipelineProperty(SAMTOOLS_PATH);
		if (samAttr != null) {
			samPath = samAttr;
		}
		String mappedBam = outputBuffers.get(0).getAbsolutePath();
		String command1 = samPath + " view -bh -F 0x0004 -o " + mappedBam + " "
				+ inputBAM;
		executeCommand(command1);
		String unmappedBam = outputBuffers.get(1).getAbsolutePath();
		String command2 = samPath + " view -bh -f 0x0004 -o " + unmappedBam
				+ " " + inputBAM;
		executeCommand(command2);
		System.out.println("Testing ...");

	}
}
