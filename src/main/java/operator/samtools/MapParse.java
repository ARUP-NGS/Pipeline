package operator.samtools;

import java.util.List;

import operator.IOOperator;
import operator.OperationFailedException;

import org.apache.log4j.Logger;

import pipeline.Pipeline;
import buffer.BAMFile;
import buffer.FileBuffer;

/*
 * MapParse parses a BAM file into two output BAM files - mapped and unmapped
 * This is used in the RNA gene fusion/translocation pipeline.
 */

public class MapParse extends IOOperator {

	public static final String SAMTOOLS_PATH = "samtools.path";

	String defaultSamPath = "samtools";
	String inputBAM = getInputBufferForClass(BAMFile.class).getAbsolutePath();
	List<FileBuffer> outputBuffers = this
			.getAllOutputBuffersForClass(BAMFile.class);

	@Override
	public void performOperation() throws OperationFailedException {
		if (outputBuffers.size() != 2) {
			throw new IllegalArgumentException(
					"Exactly 2 output buffers required - the first for mapped reads, the second for unmapped reads.");
		}
		Logger.getLogger(Pipeline.primaryLoggerName).info(
				"Pipeline is parsing BAM file " + inputBAM + " into Mapped ("
						+ outputBuffers.get(0) + ") and Unmapped ("
						+ outputBuffers.get(1) + ").");
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

	}
}
