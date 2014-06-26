package operator.seqalto;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.PipedCommandOp;
import pipeline.Pipeline;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;

public class SeqAltoAlign extends PipedCommandOp {

	public static final String SAMPLE = "sample";
	
	List<StringPair> fastqFiles = new ArrayList<StringPair>();
	String pathToSeqAlto = null;
	int threads = 8;
	int trimCutoff = 10;
	String readGroup = "ARUP";
	
	@Override
	protected String getCommand() throws OperationFailedException {
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		pathToSeqAlto = this.getPipelineProperty("seqalto.path");
		String pathToIndex = this.getPipelineProperty("seqalto.index.path");
		
		String sample = "sample";
		String sampleID = properties.get(SAMPLE);
		if (sampleID != null) 
			sample = sampleID;
		//readGroup = readGroup + "\\tSM:" + sample;
		
		logger.info("Using read group : " + readGroup);
		
		if (pathToSeqAlto == null) {
			throw new OperationFailedException(" seqalto.path not specified", this);
		}
		
		if (pathToIndex == null) {
			throw new OperationFailedException(" seqalto.index.path not specified", this);
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
		
		String command = pathToSeqAlto + " align " + pathToIndex + " -1 " + files.first + " -2 " + files.second + " --trim " + trimCutoff + " -p " + threads + " --rg " + readGroup + " --pl ILLUMINA --sm sample ";
		
		return command;
	}

	class StringPair {
		String first = null;
		String second = null;
	}

}
