package operator.novoalign;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;

public class MultiSortMergeIndex extends IOOperator {

	String pathToNovosort = null;
	int threads = 8;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		pathToNovosort = this.getPipelineProperty("novoalign.path");
		String pathToIndex = this.getPipelineProperty("novoalign.index.path");
		
		if (pathToNovosort == null) {
			throw new OperationFailedException(" novoalign.path not specified", this);
		}
		
		pathToNovosort = pathToNovosort.replace("novoalign", "novosort");
		if (!pathToNovosort.endsWith("novosort")) {
			pathToNovosort = pathToNovosort + "novosort";
		}
		
		List<FileBuffer> inputFiles = new ArrayList<FileBuffer>();
		for(FileBuffer inputBuffer : inputBuffers) {
			if (inputBuffer instanceof BAMFile) {
				inputFiles.add(inputBuffer);
			}
			if (inputBuffer instanceof MultiFileBuffer) {
				MultiFileBuffer multiBuf = (MultiFileBuffer)inputBuffer;
				for(int i=0; i<multiBuf.getFileCount(); i++) {
					FileBuffer buf = multiBuf.getFile(i);
					if (buf instanceof BAMFile) {
						inputFiles.add(buf);
					}
				}
			}
		}
		
		String outputFilePath = outputBuffers.get(0).getAbsolutePath();
		logger.info("Merging, sorting, and indexing " + inputFiles.size() + " files into output file: " + outputFilePath);
		
		String command = pathToNovosort;
		for(FileBuffer buf : inputFiles) {
			command = command + " " + buf.getAbsolutePath();
		}
		command = command +  " -c " + threads;
		command = command + " -i ";
		command = command + " -o " + outputFilePath;
		
		System.out.println("Executing command : " + command);
		executeCommand(command);
	}

}
