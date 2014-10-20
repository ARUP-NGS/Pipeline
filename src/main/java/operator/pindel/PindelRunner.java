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
import buffer.ReferenceFile;

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
		//executeCommand(command, true); // run pindel

		// Create PindelFolderFilter
		// Produce Pindel output?
		Logger.getLogger(Pipeline.primaryLoggerName).info("Pindel run completed, parsing results");
		
		PindelResultsContainer resultsObject = (PindelResultsContainer)getOutputBufferForClass(PindelResultsContainer.class);
		resultsObject.readResults(outputPrefix,filterThreshold);
		
		Map<String, List<PindelResult>> results = resultsObject.getPindelResults();
		
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
		featureLookup.buildExonMap(features);
		
		
		for(String svType : results.keySet()) {
			for(PindelResult sv : results.get(svType)) {
				
				double cov = computeMeanCoverageForRegion(bam, sv.getChromo(), sv.getRangeStart(), sv.getRangeEnd());
				sv.setMeanDepth(cov);
				System.out.println("Mean depth for " + sv.getChromo() + ": " + sv.getRangeStart() + "-" + sv.getRangeEnd() + "  :  " + cov + " var freq: " + (double)sv.getSupportReads()/sv.getMeanDepth());
				Object[] overlappingFeatures = featureLookup.getIntervalObjectsForRange(sv.getChromo(), sv.getRangeStart(), sv.getRangeEnd());
				for(Object feat : overlappingFeatures) {
					sv.addFeatureAnnotation(feat.toString());
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
