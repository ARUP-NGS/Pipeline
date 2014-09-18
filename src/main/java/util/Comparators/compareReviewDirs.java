package util.Comparators;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import json.JSONException;
import buffer.FileBuffer;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import operator.IOOperator;
import operator.OperationFailedException;
import operator.variant.CompareVCF;
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
	List<FileBuffer> RevDirs = this.getAllInputBuffersForClass(FileBuffer.class);
	String revDirLoc1 = RevDirs.get(0).getAbsolutePath();
	if(!revDirLoc1.endsWith("/"))
		revDirLoc1=revDirLoc1+"/";
	String revDirLoc2 = RevDirs.get(1).getAbsolutePath();
	if(!revDirLoc2.endsWith("/"))
		revDirLoc2=revDirLoc1+"/";
	
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
	String vcfLoc1 = revDirLoc1 + "var/" + revDirLoc1.split(".")[0]+".all.vcf";
	String vcfLoc2 = revDirLoc2 + "var/" + revDirLoc2.split(".")[0]+".all.vcf";
	VariantPool varPool1 = new VariantPool(new VCFFile( new File(vcfLoc1)));
	VariantPool varPool2 = new VariantPool(new VCFFile( new File(vcfLoc2)));
	varPool1.sortAllContigs();
	varPool2.sortAllContigs();
	CompareVCF compare = new CompareVCF();
	int[] compareStats = CompareVCF.compareVars(varPool1, varPool2, logger);
	//TODO: Create output for this operation.
	
	//
	String csvLoc1 = revDirLoc1 + "var/" + revDirLoc1.split(".")[0]+"_annotated.csv";
	String csvLoc2 = revDirLoc2 + "var/" + revDirLoc2.split(".")[0]+"_annotated.csv";
}

}