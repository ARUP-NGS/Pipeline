package operator.samtools;

import java.util.logging.Logger;

import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.GlobFileBuffer;
import buffer.MultiFileBuffer;

public class MergeFiles extends CommandOperator {
	
	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() {
		Object samPropsPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		int count = 0;
		StringBuffer inputPaths = new StringBuffer();
		for(int i=0; i<inputBuffers.size(); i++) {
			FileBuffer buff = inputBuffers.get(i);
			if (buff instanceof BAMFile) {
				inputPaths.append(" " + buff.getAbsolutePath());
				count++;
			}
			
			if (buff instanceof MultiFileBuffer || buff instanceof GlobFileBuffer) {
				MultiFileBuffer mBuf = (MultiFileBuffer)buff;
				for(int j=0; j<mBuf.getFileCount(); j++) {
					inputPaths.append(" " + mBuf.getFile(j).getAbsolutePath());
					count++;
				}
			}
		}
		
		
		System.out.println("Samtools merge is merging files: " + inputPaths);
		if (count == 0) {
			throw new IllegalArgumentException("No files found to merge, aborting");
		}
		
		String command;
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		if (count == 1) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Only one input to MergeFiles, not merging...");
			//Should we just move the file? Copy it? 
			command = "cp " + inputPaths + " " + outputPath;
		}
		else {
			command = samtoolsPath + " merge -f " + outputPath + " " + inputPaths ;			
		}
		
		
		return command;
	}

}
