package operator.pindel;

import gene.BasicIntervalContainer;
import gene.ExonLookupService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import json.JSONException;
import json.JSONObject;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import util.coverage.CoverageCalculator;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;
import buffer.TextBuffer;

/**
 * An operator to run PINDEL, parse the output, and annotate it using the 'ExonLookupService', which
 * determines which genes & exons are overlapped by any detected variants. 
 * @author brendan
 *
 */
public class PindelRunner extends IOOperator {

	public static final String ISIZE = "insert.size";
	public static final String FILTERTHRESHOLD = "filter.threshold";
	public static final String PINDEL_PATH="pindel.path";
	public static final String MERGE_THRESHOLD = "merge.threshold";
	public static final String PREFERRED_NMS = "nm.Definitions";
	public static final String DEST = "dest";
	protected File destination = null;
	
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
		
	
		
		String pathToBedFile = (getInputBufferForClass(BEDFile.class)).getAbsolutePath(); 
		String pathToConfigFile = this.getProjectHome() + "pindelConfig.txt"; 
		String pathToReference = (getInputBufferForClass(ReferenceFile.class)).getAbsolutePath(); 
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
		
		int mergeThreshold = 25;
		String mergeAttr = properties.get(MERGE_THRESHOLD);
		if (mergeAttr != null) {
			mergeThreshold = Integer.parseInt(mergeAttr);
		}
		
		String preferredNMsPath = this.getAttribute(PREFERRED_NMS);
		Map<String, String> preferredNMs = loadPreferredNMs(preferredNMsPath);
		
		
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
				" -T " + Math.min(8, this.getPipelineOwner().getThreadCount()) +
				" -j " + pathToBedFile +
				" -L " + this.getProjectHome() + "/pindel.log ";
		Logger.getLogger(Pipeline.primaryLoggerName).info("Pindel operator is executing command " + command);
		executeCommand(command, true); // run pindel

		// Create PindelFolderFilter
		// Produce Pindel output?
		Logger.getLogger(Pipeline.primaryLoggerName).info("Pindel run completed, parsing results");
		Logger.getLogger(Pipeline.primaryLoggerName).info("Pindel Threshold: " + filterThreshold + " merge distance: " + mergeThreshold);
		
		PindelResultsContainer resultsObject = (PindelResultsContainer)getOutputBufferForClass(PindelResultsContainer.class);
		resultsObject.readResults(outputPrefix,filterThreshold, mergeThreshold);
		
		Map<String, List<PindelResult>> results = resultsObject.getPindelResults();
		
//#CHRISK
		FileBuffer pindelRawResults = getOutputBufferForClass(TextBuffer.class);
		File[] files = outputDir.listFiles();	
		String destinationPath = properties.get(DEST);
		if (destinationPath == null) {
			throw new OperationFailedException("No destination path specified, use dest=\"path/to/dir/\"", this);
		}
		
		String projHome = getProjectHome();
		
		if (! destinationPath.startsWith("/") && projHome != null) {
			destinationPath = projHome + destinationPath;
		}
		
		destination = new File(destinationPath);
		if (destination.exists()) {
			if (! destination.isDirectory()) {
				throw new OperationFailedException("Destination path " + destination.getAbsolutePath() + " is not a directory", this);
			}
		}
		else {
			destination.mkdir();
		}
		
		if (destination == null)
			throw new OperationFailedException("Destination directory has not been specified", this);
		String fileSep = System.getProperty("file.separator");
		
		try{
			Logger.getLogger(Pipeline.primaryLoggerName).info("Moving Pindel Output Files");
			for(File file:files){

				if(file.getName().endsWith("_D") )
				{					
					//file.setFile(destinationFile);
					String filename1 = file.getName();
					String newPath1 = destination.getAbsolutePath() + fileSep + filename1;
					File destinationFile1 = new File(newPath1);
					file.renameTo(destinationFile1);
					pindelRawResults.setFile(destinationFile1);
				}
				if(file.getName().endsWith("_SI")){
					String filename2 = file.getName();
					String newPath2 = destination.getAbsolutePath() + fileSep + filename2;
					File destinationFile2 = new File(newPath2);
					file.renameTo(destinationFile2);
					pindelRawResults.setFile(destinationFile2);
				}
				if(file.getName().endsWith("_TD")){
					String filename3 = file.getName();
					String newPath3 = destination.getAbsolutePath() + fileSep + filename3;
					File destinationFile3 = new File(newPath3);
					file.renameTo(destinationFile3);
					pindelRawResults.setFile(destinationFile3);
				}
				if(file.getName().endsWith("_LI")){
					String filename4 = file.getName();
					String newPath4 = destination.getAbsolutePath() + fileSep + filename4;
					File destinationFile4 = new File(newPath4);
					file.renameTo(destinationFile4);
					pindelRawResults.setFile(destinationFile4);
				}
				if(file.getName().endsWith("2_D") )
				{					
					//file.setFile(destinationFile);
					String filename5 = file.getName();
					String newPath5 = destination.getAbsolutePath() + fileSep + filename5;
					File destinationFile5 = new File(newPath5);
					file.renameTo(destinationFile5);
					pindelRawResults.setFile(destinationFile5);
				}
				if(file.getName().endsWith("2_SI")){
					String filename6 = file.getName();
					String newPath6 = destination.getAbsolutePath() + fileSep + filename6;
					File destinationFile6 = new File(newPath6);
					file.renameTo(destinationFile6);
					pindelRawResults.setFile(destinationFile6);
				}
				if(file.getName().endsWith("2_TD")){
					String filename7 = file.getName();
					String newPath7 = destination.getAbsolutePath() + fileSep + filename7;
					File destinationFile7 = new File(newPath7);
					file.renameTo(destinationFile7);
					pindelRawResults.setFile(destinationFile7);
				}
				if(file.getName().endsWith("2_LI")){
					String filename8 = file.getName();
					String newPath8 = destination.getAbsolutePath() + fileSep + filename8;
					File destinationFile8 = new File(newPath8);
					file.renameTo(destinationFile8);
					pindelRawResults.setFile(destinationFile8);
				}
				if(file.getName().endsWith("3_D") )
				{					
					//file.setFile(destinationFile);
					String filename9 = file.getName();
					String newPath9 = destination.getAbsolutePath() + fileSep + filename9;
					File destinationFile9 = new File(newPath9);
					file.renameTo(destinationFile9);
					pindelRawResults.setFile(destinationFile9);
				}
				if(file.getName().endsWith("3_SI")){
					String filename10 = file.getName();
					String newPath10 = destination.getAbsolutePath() + fileSep + filename10;
					File destinationFile10 = new File(newPath10);
					file.renameTo(destinationFile10);
					pindelRawResults.setFile(destinationFile10);
				}
				if(file.getName().endsWith("3_TD")){
					String filename11 = file.getName();
					String newPath11 = destination.getAbsolutePath() + fileSep + filename11;
					File destinationFile11 = new File(newPath11);
					file.renameTo(destinationFile11);
					pindelRawResults.setFile(destinationFile11);
				}
				if(file.getName().endsWith("3_LI")){
					String filename12 = file.getName();
					String newPath12 = destination.getAbsolutePath() + fileSep + filename12;
					File destinationFile12 = new File(newPath12);
					file.renameTo(destinationFile12);
					pindelRawResults.setFile(destinationFile12);
				}
				
			}
		}
		catch(Exception e){
			System.out.println("ERROR: No pindel output files");
		}
		

//#\CHRISK		
		
		//Now add annotations to all those results... 
		ExonLookupService featureLookup = new ExonLookupService();
		String featureFile = getPipelineProperty("feature.file");
		if(featureFile == null){
			throw new IOException("PipelineProperty 'feature.file' not defined.");
		}
		File features = new File(featureFile);
		if (!features.exists()) {
			throw new IOException("Feature file " + features.getAbsolutePath() + " does not exist!");
		}
		featureLookup.setPreferredNMs(preferredNMs);
		featureLookup.buildExonMap(features);
		
		
		for(String svType : results.keySet()) {
			for(PindelResult sv : results.get(svType)) {
				
				double cov = computeMeanCoverageForRegion(bam, sv.getChromo(), sv.getRangeStart(), sv.getRangeEnd());
				sv.setMeanDepth(cov);
				Object[] overlappingFeatures = featureLookup.getIntervalObjectsForRange(sv.getChromo(), sv.getRangeStart(), sv.getRangeEnd());
				for(Object feats : overlappingFeatures) {
					//Add exons only
					for(Object feat : feats.toString().split(",")) {
						if (feat.toString().contains("Exon")) {
							sv.addFeatureAnnotation(feat.toString().replace("Coding", "").replace(";", ""));
						}
					}
				}
			}
		}
		
		
		//Write it to a file
		JSONObject resultsJSON = resultsObject.resultsToJSON();
		File resultJSONFile = resultsObject.getFile();
		resultJSONFile.createNewFile();
		BufferedWriter writer = new BufferedWriter(new FileWriter(resultsObject.getFile()));
		writer.write(resultsJSON.toString() +"\n");
		writer.close();
		
	}


	
		private double computeMeanCoverageForRegion(BAMFile bam, String chr, int start, int end) throws IOException {
			BasicIntervalContainer intervals = new BasicIntervalContainer();
			intervals.addInterval(chr,  start,  end, null);
			CoverageCalculator covCalc = new CoverageCalculator(bam.getFile(), intervals);
			covCalc.setThreadCount(1);
			int[] depthHistogram;
			try {
				depthHistogram = covCalc.computeOverallCoverage();
				double meanDepth = CoverageCalculator.getMean(depthHistogram);
				if (Double.isNaN(meanDepth)) {
					meanDepth = 0.0; //WIll break json parsing later if left as NaN
				}
				return meanDepth;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return -1;
		}
		
	private void createConfigFile(String sampleName, String pathToBamFile,
			int insertSize, String pathToConfigFile) throws Exception {
		
			PrintWriter writer = new PrintWriter(pathToConfigFile, "UTF-8");
			writer.println(pathToBamFile + "\t"
					+ insertSize + "\t" + sampleName);
			writer.close();

	}

}
