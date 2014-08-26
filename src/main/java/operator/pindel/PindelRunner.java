package operator.pindel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import json.JSONException;
import operator.IOOperator;
import operator.OperationFailedException;

public class PindelRunner extends IOOperator {

	public static final String ISIZE = "insert.size";
	public static final String FILTERTHRESHOLD = "filter.threshold";

	@Override
	public boolean requiresReference() {
		return true;
	}

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		
		String outputPrefix = this.getProjectHome() + "pindelOutput/out";
		String pathToBamFile = inputBuffers.get(0).getAbsolutePath(); 
		String pathToBedFile = inputBuffers.get(1).getAbsolutePath(); 
		String pathToConfigFile = this.getProjectHome() + "pindelConfig.txt"; 
		String pathToPindel = ""; // Pipeline property - never changes
		String pathToReference = inputBuffers.get(2).getAbsolutePath(); 
		String sampleName = inputBuffers.get(3).getAbsolutePath(); 
		
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

		createConfigGile(sampleName, pathToBamFile, insertSize,
				pathToConfigFile);

		new File(this.getProjectHome() + "/pindelOutput");

		String command = pathToPindel + " -f " + pathToReference + " -i "
				+ pathToConfigFile + " -o " + outputPrefix + " -j "
				+ pathToBedFile;

		executeCommand(command); // run pindel

		// Create PindelFolderFilter
		// Produce Pindel output?
		PindelFolderFilter folderFilter = new PindelFolderFilter(outputPrefix,
				filterThreshold, pathToReference, pathToPindel);

		// Produce VarViewer output?
	}

	private void createConfigGile(String sampleName, String pathToBamFile,
			int insertSize, String pathToConfigFile) {
		try {
			PrintWriter writer = new PrintWriter(pathToConfigFile, "UTF-8");
			writer.println(sampleName + "\t" + pathToBamFile + "\t"
					+ insertSize);
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
