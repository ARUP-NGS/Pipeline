package operator.pindel;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import json.JSONException;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.BAMFile;

public class PindelRunner extends IOOperator {

	public static final String ISIZE = "insert.size";
	public static final String FILTERTHRESHOLD = "filter.threshold";
	public static final String PINDEL_PATH="pindel.path";
	
	@Override
	public boolean requiresReference() {
		return true;
	}

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		
		
		String outputPrefix = this.getProjectHome() + "pindelOutput/out";
		//String pathToBamFile = inputBuffers.get(0).getAbsolutePath();
		BAMFile bam = (BAMFile)getInputBufferForClass(BAMFile.class);
		String pathToBamFile = bam.getAbsolutePath();
		
		String pathToBedFile = inputBuffers.get(1).getAbsolutePath(); 
		String pathToConfigFile = this.getProjectHome() + "pindelConfig.txt"; 
		String pathToReference = inputBuffers.get(2).getAbsolutePath(); 
		String sampleName="currentSample";
		
		String pathToPindel = this.getAttribute(PINDEL_PATH);
		if (pathToPindel == null) {
			pathToPindel = this.getPipelineProperty(PINDEL_PATH);
		}
		
		int insertSize = 400; // probably should be an attribute
		String insertSizeString = properties.get(ISIZE);
		if (insertSizeString != null) {
			insertSize = Integer.parseInt(insertSizeString);
		}

		int filterThreshold = 15; // probably should be an attribute
		String filterString = properties.get(FILTERTHRESHOLD);
		if (filterString != null) {
			filterThreshold = Integer.parseInt(filterString);
		}

		try {
			createConfigFile(sampleName, pathToBamFile, insertSize,
					pathToConfigFile);
		} catch (Exception e) {
			e.printStackTrace();
			throw new OperationFailedException(e.getLocalizedMessage(), this);
		}

		File outputDir = new File(this.getProjectHome() + "/pindelOutput");
		outputDir.mkdirs(); //Actually create the directory on the filesystem

		String command = pathToPindel + 
				" -f " + pathToReference + 
				" -i " + pathToConfigFile + 
				" -o " + outputPrefix +
				//" -T " + this.getPipelineOwner().getThreadCount() +
				" -j " + pathToBedFile +
				" -L " + this.getProjectHome() + "/pindel.log ";
		Logger.getLogger(Pipeline.primaryLoggerName).info("Pindel operator is executing command " + command);
		executeCommand(command); // run pindel

		// Create PindelFolderFilter
		// Produce Pindel output?
		Logger.getLogger(Pipeline.primaryLoggerName).info("Pindel run completed, parsing results");
		PindelFolderFilter folderFilter = new PindelFolderFilter(outputPrefix,
				filterThreshold, pathToReference, pathToPindel);
		List<PindelResult> results = folderFilter.getPindelResults();
		// Produce VarViewer output?
	}

	private void createConfigFile(String sampleName, String pathToBamFile,
			int insertSize, String pathToConfigFile) throws Exception {
		
			PrintWriter writer = new PrintWriter(pathToConfigFile, "UTF-8");
			writer.println(pathToBamFile + "\t"
					+ insertSize + "\t" + sampleName);
			writer.close();
		

	}

}
