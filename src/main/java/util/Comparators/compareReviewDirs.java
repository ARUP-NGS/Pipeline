package util.Comparators;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import json.JSONException;
import json.JSONObject;
import buffer.FileBuffer;
import buffer.JSONBuffer;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import operator.IOOperator;
import operator.OperationFailedException;
import operator.variant.CompareVCF;
import util.CompressGZIP;
import util.Comparators.CompareAnnotationCSVs;
import pipeline.Pipeline;


/*
 * Compares review directories, including VCFs, CSVs, and QC Metrics.
 * @author daniel
 */

public class compareReviewDirs extends IOOperator{

@Override
public void performOperation() throws OperationFailedException, JSONException,
		IOException {
	
	Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
	// Get folder locations
	String revDirLoc1 = this.getAttribute("ReviewDir1");
	if(revDirLoc1.endsWith("/"))
		revDirLoc1=revDirLoc1.substring(0, revDirLoc1.length()-1);
	String revDirLoc2 = this.getAttribute("ReviewDir2");
	if(revDirLoc2.endsWith("/"))
		revDirLoc2=revDirLoc2.substring(0, revDirLoc2.length()-1);
	
	// Create "File" objects for them
	File revDir1 = new File(revDirLoc1);
	File revDir2 = new File(revDirLoc2);
	
	// If provided folders either don't exist or aren't folders, run away!
	if (!revDir1.exists() || !revDir1.isDirectory()) { 
		throw new OperationFailedException("ERROR: Review Directory #1 does not exist as a folder:  " + revDirLoc1, this);
	}
	if (!revDir2.exists() || !revDir2.isDirectory()) { 
		throw new OperationFailedException("ERROR: Review Directory #2 does not exist as a folder:  " + revDirLoc2, this);
	}
	
	//Create "File"s for the vcf files.VCF1reader
	String Prefix1 = revDirLoc1.split("/")[revDirLoc1.split("/").length-1];
	String Prefix2 = revDirLoc2.split("/")[revDirLoc2.split("/").length-1];
	System.out.println("Prefix1 is " + Prefix1);
	System.out.println("Prefix2 is " + Prefix2);
	String vcfLoc1 = revDirLoc1 + "/var/" + Prefix1.split("\\.")[0]+"_all_variants.vcf";
	logger.info("vcfLoc1 is: " + vcfLoc1);
	String vcfLoc2 = revDirLoc2 + "/var/" + Prefix2.split("\\.")[0]+"_all_variants.vcf";
	logger.info("vcfLoc1 is: " + vcfLoc1);
	VariantPool varPool1 = new VariantPool(new VCFFile( new File(vcfLoc1)));
	VariantPool varPool2 = new VariantPool(new VCFFile( new File(vcfLoc2)));
	varPool1.sortAllContigs();
	varPool2.sortAllContigs();
	
	LinkedHashMap<String, Integer> compareStats = CompareVCF.compareVars(varPool1, varPool2, logger);
	String csvLoc1 = revDirLoc1 + "/var/" + Prefix1.split("\\.")[0]+"_annotated.csv";
	String csvLoc2 = revDirLoc2 + "/var/" + Prefix2.split("\\.")[0]+"_annotated.csv";
	CompareAnnotationCSVs CSVComp = new CompareAnnotationCSVs();
	LinkedHashMap<String, Object> csvResults = CSVComp.CSVCompare(csvLoc1, csvLoc2);
	
	LinkedHashMap<String, Object> AllResults = new LinkedHashMap<String, Object>();
	AllResults.put("CSVResults", csvResults);
	AllResults.put("VCFResults", compareStats);
	JSONObject ResultsJson = new JSONObject(csvResults);
	String ResultsStr = ResultsJson.toString();
	
	byte[] bytes = CompressGZIP.compressGZIP(ResultsStr);

	// Write compresssed JSON to file
	File dest = this.getOutputBufferForClass(JSONBuffer.class).getFile();
	BufferedOutputStream writer = new BufferedOutputStream(
			new FileOutputStream(dest));
	writer.write(bytes);
	writer.close();
	
	//TODO: Create output for this operation.
	
}

}