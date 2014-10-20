package operator.bedtools;
import java.io.File;

import operator.IOOperator;
import operator.OperationFailedException;
import buffer.BAMFile;
import buffer.FileBuffer;

/*
 * Converts Bam to Fastq
 * Needed for RNA translocation pipeline.
 * In particular, use when bam2fq from samtools isn't working for any reason.
 * @author daniel 
 */

public class Bam2Fq extends IOOperator {
	public static final String BEDTOOLS_PATH = "bedtools.path";
	@Override
	public void performOperation() throws OperationFailedException {
		String inputBam = this.getInputBufferForClass(BAMFile.class).getAbsolutePath();
		File outputFastq = this.getOutputBufferForClass(FileBuffer.class).getFile();
		String defaultBedtoolsPath = "bedtools";
		String bedtoolsPath = defaultBedtoolsPath;
		String bedAttr = this.getPipelineProperty(BEDTOOLS_PATH);
		if (bedAttr != null) {
			bedtoolsPath = bedAttr;
		}
		String command_str = bedtoolsPath + " bamtofastq -i " + inputBam + " -fq " + outputFastq.getAbsolutePath();
		executeCommand(command_str);
		return;
	}

}