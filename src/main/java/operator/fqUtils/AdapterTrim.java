package operator.fqUtils;

import java.io.File;
import java.util.List;

import operator.CommandOperator;
import operator.OperationFailedException;
import buffer.FastQFile;
import buffer.FileBuffer;

public class AdapterTrim extends CommandOperator {
	
	public static final String SCRIPT_PATH = "trim.script.path";
	private String scriptPath = null;
	
	@Override
	protected String getCommand() throws OperationFailedException {
		if (scriptPath == null) {
			scriptPath = this.getPipelineProperty(SCRIPT_PATH);
			if (scriptPath == null)
				throw new OperationFailedException("No path to adapter trimming script found", this);
		}
		
		List<FileBuffer> fqs = this.getAllInputBuffersForClass(FastQFile.class);
		if (fqs.size() != 2) {
			throw new OperationFailedException("Must have exactly two fastq files for AdapterTrim, found " + fqs.size(), this);
		}
		
		List<FileBuffer> outputFQs = this.getAllOutputBuffersForClass(FastQFile.class);
		if (outputFQs.size() != 2) {
			throw new OperationFailedException("Must have exactly two output fastq files for AdapterTrim, found " + outputFQs.size(), this);
		}
		
		String outputFilenameA = fqs.get(0).getFilename();
		outputFilenameA = outputFilenameA.replace(".fastq.gz", "").replace(".fq.gz", "") + "_val_1.fq.gz";
		
		String outputFilenameB = fqs.get(1).getFilename();
		outputFilenameB = outputFilenameB.replace(".fastq.gz", "").replace(".fq.gz", "") + "_val_2.fq.gz";
		
		File outputA = new File(getProjectHome() + outputFilenameA);
		File outputB = new File(getProjectHome() + outputFilenameB);
		
		outputFQs.get(0).setFile(outputA);
		outputFQs.get(1).setFile(outputB);
		
		String command = scriptPath + " " + fqs.get(0).getAbsolutePath() + " " + fqs.get(1).getAbsolutePath() + " .";
		
		System.err.println("Expecting output: " + outputA.getAbsolutePath());
		
		return command;
	}



}
