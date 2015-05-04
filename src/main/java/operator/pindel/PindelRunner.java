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
	public static final String PINDEL_RAW_FILE = "pindel.raw.file";
	public static final String THREADS = "threads";
	
	
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

		int threadsToUse = this.getPipelineOwner().getThreadCount();
		String threadsAttr = this.getAttribute(THREADS);
		if (threadsAttr != null && threadsAttr.length()>0) {
			threadsToUse = Integer.parseInt(threadsAttr);
		}
		
		
		String command = pathToPindel + 
				" -f " + pathToReference + 
				" -i " + pathToConfigFile + 
				" -o " + outputPrefix +
				" -T " + threadsToUse +
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
<<<<<<< Updated upstream
=======
		//create FileBuffer for all instances????4.15.2015
		//need to fix template for the myeloids.
		//
		FileBuffer pindelRawResults1 = getOutputBufferForClass(TextBuffer.class);
		FileBuffer pindelRawResults2 = getOutputBufferForClass(TextBuffer.class);
		FileBuffer pindelRawResults3 = getOutputBufferForClass(TextBuffer.class);
		FileBuffer pindelRawResults4 = getOutputBufferForClass(TextBuffer.class);
		FileBuffer pindelRawResults5 = getOutputBufferForClass(TextBuffer.class);
		FileBuffer pindelRawResults6 = getOutputBufferForClass(TextBuffer.class);
		FileBuffer pindelRawResults7 = getOutputBufferForClass(TextBuffer.class);
		FileBuffer pindelRawResults8 = getOutputBufferForClass(TextBuffer.class);
		
		
		File[] files = outputDir.listFiles();	
		String destinationPath = properties.get(DEST);
		if (destinationPath == null) {
			throw new OperationFailedException("No destination path specified, use dest=\"path/to/dir/\"", this);
		}
>>>>>>> Stashed changes
		
		//Examine each file in the pindel raw output dir and see if we can associate it with an output buffer
		//This is so we can move the raw files into the review directory
		for(File file: outputDir.listFiles()){
			setPindelResultsFile(file, getAllOutputBuffersForClass(TextBuffer.class));		
		}
<<<<<<< Updated upstream
		
=======
		else {
			destination.mkdir();
		}
		
		if (destination == null)
			throw new OperationFailedException("Destination directory has not been specified", this);
		
		try{
			Logger.getLogger(Pipeline.primaryLoggerName).info("Moving Pindel Output Files");
			for(File file:files){

				if(file.getName().endsWith("_D") )
				{					
					pindelRawResults1.setFile(file);
				}
				if(file.getName().endsWith("_SI")){
					pindelRawResults2.setFile(file);
				}
				if(file.getName().endsWith("_TD")){
					pindelRawResults3.setFile(file);
				}
				if(file.getName().endsWith("_LI")){
					pindelRawResults4.setFile(file);
				}
				if(file.getName().endsWith("2_D") )
				{					
					pindelRawResults5.setFile(file);
				}
				if(file.getName().endsWith("2_SI")){
					pindelRawResults6.setFile(file);
				}
				if(file.getName().endsWith("2_TD")){
					pindelRawResults7.setFile(file);
				}
				if(file.getName().endsWith("2_LI")){
					pindelRawResults8.setFile(file);
				}
				//maybe just null?
				if(file.getName().equals("null")){
					System.out.println("xxxxxxxxxxxxxxxxxxx=\"null\"");
				}
				if(file.getName().equals(null)){
					System.out.println("xxxxxxxxxxxxxxxxxxx=null");
				}
			}
		}
		catch(Exception e){
			System.out.println("ERROR: No pindel output files");
		}
>>>>>>> Stashed changes
		

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



	/**
	 * Search the list of outputBuffers for one that has a PINDEL_RAW_FILE attribute that
	 * is equal to the name of the given file. If found, set the file associated with the outputBuffer
	 * to be the file given
	 * 
	 * @param file File to potentially associate with an output buffer
	 * @param outputBuffers List of outputBuffers to search
	 */
	private void setPindelResultsFile(File file, List<FileBuffer> outputBuffers) {
		for(FileBuffer outputBuffer : outputBuffers) {
			String attr = outputBuffer.getAttribute(PINDEL_RAW_FILE);
			if (attr != null && attr.equals(file.getName())) {
				outputBuffer.setFile(file);
			}
		}
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
