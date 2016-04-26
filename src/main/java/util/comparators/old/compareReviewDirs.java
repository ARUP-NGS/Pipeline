package util.comparators.old;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import json.JSONException;
import json.JSONObject;
import buffer.JSONBuffer;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import operator.IOOperator;
import operator.OperationFailedException;
import operator.variant.CompareVCF;
import util.CompressGZIP;
import util.ReviewDirTool;
import util.comparators.old.CompareAnnotationCSVs;
import util.comparators.old.CompareQCMetrics;
import pipeline.Pipeline;

/*
 * Compares review directories, including VCFs, CSVs, and QC Metrics.
 * @author daniel
 */

public class compareReviewDirs extends IOOperator {
	
	public static final String csvKey = "annotated.vars";
	public static final String qcKey = "qc.json";
	public static final String vcfKey = "vcf.file";
	
	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {

		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		// Get folder locations
		String revDirLoc1 = this.getAttribute("ReviewDir1");
		if (revDirLoc1.endsWith("/"))
			revDirLoc1 = revDirLoc1.substring(0, revDirLoc1.length() - 1);
		String revDirLoc2 = this.getAttribute("ReviewDir2");
		if (revDirLoc2.endsWith("/"))
			revDirLoc2 = revDirLoc2.substring(0, revDirLoc2.length() - 1);

		// Create "File" objects for them
		File revDir1 = new File(revDirLoc1);
		File revDir2 = new File(revDirLoc2);

		// If provided folders either don't exist or aren't folders, run away!
		if (!revDir1.exists() || !revDir1.isDirectory()) {
			throw new OperationFailedException(
					"ERROR: Review Directory #1 does not exist as a folder:  "
							+ revDirLoc1, this);
		}
		if (!revDir2.exists() || !revDir2.isDirectory()) {
			throw new OperationFailedException(
					"ERROR: Review Directory #2 does not exist as a folder:  "
							+ revDirLoc2, this);
		}

		// Create "File"s for the vcf files.VCF1reader
		// Modification: Grab file locations from Sample Manifest.
		File manifestFile1 = new File(revDirLoc1 + "/sampleManifest.txt");
		Map<String, String> smHash1 = ReviewDirTool.readManifest(manifestFile1);
		File manifestFile2 = new File(revDirLoc2 + "/sampleManifest.txt");
		Map<String, String> smHash2 = ReviewDirTool.readManifest(manifestFile2);
		
		// Compare VCF Files
		LinkedHashMap<String, Object> compareStats = null;
		if(smHash1.get(vcfKey) != null && !smHash1.get(vcfKey).isEmpty() && smHash2.get(vcfKey)!= null && !smHash2.get(vcfKey).isEmpty()) {
			String vcfLoc1 = revDirLoc1 + "/" + smHash1.get(vcfKey);
			String vcfLoc2 = revDirLoc2 + "/" + smHash2.get(vcfKey);
			logger.info("vcfLoc1 is: " + vcfLoc1);
			logger.info("vcfLoc2 is: " + vcfLoc2);
			VariantPool varPool1 = new VariantPool(new VCFFile(new File(vcfLoc1)));
			VariantPool varPool2 = new VariantPool(new VCFFile(new File(vcfLoc2)));
			varPool1.sortAllContigs();
			varPool2.sortAllContigs();
			compareStats = CompareVCF.compareVars(varPool1, varPool2, logger);
		}
		else
			throw new OperationFailedException("One or both sample manifests did not contain a final vcf. Abort mission!", this);
		
		System.out.println("VCF Comparison Done");
		
		// Compare Annotation Files
		LinkedHashMap<String, Object> csvResults = null;
		if(smHash1.get(csvKey) != null && !smHash1.get(csvKey).isEmpty() && smHash2.get(csvKey)!= null && !smHash2.get(csvKey).isEmpty()) {
			String csvLoc1 = revDirLoc1 + "/" + smHash1.get(csvKey);
			String csvLoc2 = revDirLoc2 + "/" + smHash2.get(csvKey);
			csvResults = CompareAnnotationCSVs.CSVCompare(csvLoc1, csvLoc2);
		}
		else
			throw new OperationFailedException("One or both sample manifests did not contain a final csv. Abort mission!", this);

		System.out.println("Now pausing so that I can figure out what's making all of that output! CSV Comparison Done");
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		LinkedHashMap<String, Object> qcResults = null;
		if(smHash1.get(qcKey) != null && !smHash1.get(qcKey).isEmpty() && smHash2.get(qcKey)!= null && !smHash2.get(qcKey).isEmpty()) {
			String qcLoc1 = revDirLoc1 + "/" + smHash1.get(qcKey);
			String qcLoc2 = revDirLoc2 + "/" + smHash2.get(qcKey);
			qcResults = CompareQCMetrics.JSONCompare(qcLoc1, qcLoc2);
		}
		else
			throw new OperationFailedException("One or both sample manifests did not contain a qc json. Abort mission!", this);
		System.out.println("Now pausing so that I can figure out what's making all of that output! QC Metrics Comparison Done");
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		LinkedHashMap<String, Object> AllResults = new LinkedHashMap<String, Object>();
		if(csvResults != null)
			AllResults.put("CSVResults", csvResults);
		else
			logger.info("csvResults is null. Not adding to json");
		if(compareStats != null)
			AllResults.put("VCFResults", compareStats);
		else
			logger.info("compareStats is null. Not adding to json");
		if(qcResults != null)
			AllResults.put("QCResults", qcResults);
		else
			logger.info("qcResults is null. Not adding to json");
		JSONObject ResultsJson = new JSONObject(AllResults);
		String ResultsStr = ResultsJson.toString();

		byte[] bytes = CompressGZIP.compressGZIP(ResultsStr);

		// Write compresssed JSON to file
		File dest = this.getOutputBufferForClass(JSONBuffer.class).getFile();
		BufferedOutputStream writer = new BufferedOutputStream(
				new FileOutputStream(dest));
		writer.write(bytes);
		writer.close();

		// TODO: Create output for this operation.

	}
}