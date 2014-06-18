package operator.samtools;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import operator.CommandOperator;
import operator.IOOperator;
import operator.OperationFailedException;
import operator.PipedCommandOp;

import org.apache.log4j.Logger;

import pipeline.Pipeline;
import buffer.BAMFile;
import buffer.FileBuffer;

/*
 * Converts Bam to Fastq
 * Needed for RNA translocation pipeline. 
 * 
 */

public class Bam2Fq extends IOOperator {
	public static final String SAMTOOLS_PATH = "samtools.path";
	String inputBam = this.getInputBufferForClass(BAMFile.class).getAbsolutePath();
	String outputFastq = inputBam.substring(0,inputBam.lastIndexOf('.')) + ".fastq";
	String defaultSamPath = "samtools";

	@Override
	public void performOperation() throws OperationFailedException {
		String samPath = defaultSamPath;
		String samAttr = this.getPipelineProperty(SAMTOOLS_PATH);
		if (samAttr != null) {
			samPath = samAttr;
		}
		String command_str = samPath + " bam2fq " + inputBam ;
		executeCommand(command_str);
		return;
	}

	

}