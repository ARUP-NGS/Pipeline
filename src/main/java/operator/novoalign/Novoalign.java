package operator.novoalign;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.PipedCommandOp;
import pipeline.Pipeline;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;

public class Novoalign extends PipedCommandOp {
	
	public static final String SAMPLE = "sample";
	
	List<StringPair> fastqFiles = new ArrayList<StringPair>();
	String pathToNovoalign = null;
	int threads = 8;
	int trimCutoff = 10;
	String readGroup = "@RG\\tID:ARUP\\tPL:ILLUMINA";
	
	@Override
	protected String getCommand() throws OperationFailedException {
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		pathToNovoalign = this.getPipelineProperty("novoalign.path");
		String pathToIndex = this.getPipelineProperty("novoalign.index.path");
		
		String sample = "sample";
		String sampleID = properties.get(SAMPLE);
		if (sampleID != null) 
			sample = sampleID;
		readGroup = readGroup + "\\tSM:" + sample;
		
		logger.info("Using read group : " + readGroup);
		
		if (pathToNovoalign == null) {
			throw new OperationFailedException(" novoalign.path not specified", this);
		}
		
		if (pathToIndex == null) {
			throw new OperationFailedException(" novoalign.index.path not specified", this);
		}
		
		if (inputBuffers.size() != 2) {
			throw new OperationFailedException("Incorrect number of input arguments, must be exactly 2", this);
		}
		
//		String[] commands = new String[fastqFiles.size()];
//		int index = 0;
//		for(StringPair files : fastqFiles) {
//			commands[index] = pathToSeqAlto + " align " + pathToIndex + " -1 " + files.first + " -2 " + files.second + " --trim " + trimCutoff + " -p " + threads
//		}
		
		List<FileBuffer> inputFiles = this.getAllInputBuffersForClass(MultiFileBuffer.class);
		
		StringPair files = new StringPair();
		files.first = ((MultiFileBuffer)inputFiles.get(0)).getFile(0).getAbsolutePath() ;
		files.second = ((MultiFileBuffer)inputFiles.get(1)).getFile(0).getAbsolutePath();
		
		String command = pathToNovoalign + " -d " + pathToIndex + " -f " + files.first + " " + files.second + " -c " + threads + " -oSAM " + readGroup + " ";
		
		return command;
	}

	class StringPair {
		String first = null;
		String second = null;
	}
}
