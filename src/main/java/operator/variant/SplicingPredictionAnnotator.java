package operator.variant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.CSVFile;
import buffer.variant.VariantRec;


/**
 * Adds the scoreSpliceSites annotation to a variant in the variant pool:
 * 1) Run MarcSingleton's scoreSpliceSites perl program on annotated CSV file once - predicts presence of splice site
 * 2) Read in data one line at a time (per variant)
 * 3) Add annotation value (NM#:REFscore,ALTscore;NM#2:REFscore2,ALTscore2,etc.) to annotations separately
 * @author elainegee
 *
 */

public class SplicingPredictionAnnotator extends Annotator {
	
	public static final String SPLICINGPREDICTION_PATH = "splicingprediction.path"; //path to spliceingPrediction/ folder (not script)
	
	
	CSVFile csvFile = null;
	String SpliceScriptPath = null;
	
	/**
	 * This function is run first by the Annotator class. Will run Marc Singleton's  scoreSpliceSites on an annotation (CSV) file
	 * @throws OperationFailedException
	 */
	@Override
	protected void prepare() throws OperationFailedException {
		if (SpliceScriptPath == null) {
			throw new OperationFailedException("splicingPrediction path not specified", this);
		}
		
		//Run scoreSpliceSites on CSV file, i.e. annotation must have been previously done (outputs to standard out)
		String command = BashBuilder(csvFile.getAbsolutePath());
		executeCommand(command);
		
		//Now read in re-annotated CSV necessary for annotating variants
		String spliceOut = getProjectHome() + "splice.final.csv";  
		File finalCSV = new File(spliceOut);
		
		//Associate all variants in file with those in variant pool
		try {
			annotateVariantsFromFile(finalCSV);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * This function is run last by the Annotator class. Will output Excel file for troubleshooting
	 * @throws OperationFailedException
	 */
	@Override
	protected void cleanup() throws OperationFailedException {
		//Will need to cleanup intermediate file "splice.final.csv"
		
		//For debugging, use ExcelWriter to double check variants are correctly assigned
		//ExcelWriter ew = new ExcelWriter();
	}
	
	/**
	 * Constructor for creating bash script to handle scoreSpliceSites, which sends results to StandardOut
	 * @param CSVPath
	 */
	protected String BashBuilder(String CSVPath) {		
		String fileName = getProjectHome() + "SpliceScript.sh";
		String perlcmd="perl " + SpliceScriptPath +"/scoreSpliceSites -v " + CSVPath + " -s " + SpliceScriptPath +"/spliceRegions.bed > splice.final.csv";
				
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(fileName));
			writer.write(perlcmd + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Return bash caommand to execute shell script to run scoreSpliceSites
		String bashcmd = "/bin/bash " + fileName;
		return bashcmd;
	}
	
	
	/**
	 * This function takes the output file that the script generates, reads one line at a time and parses 
	 * the chromosome, position, ref, alt (for identifying variant) and associated splicing info (all results from
	 * scoreSpliceSites, NM#(s) with largest ALTScore-REFScore diff, largest ALTScore-REFScore diff), and associates 
	 * the values with the variant
	 * @param tableFile
	 * @param var
	 * @throws OperationFailedException
	 */
	private void annotateVariantsFromFile(File tableFile) throws IOException {
		/**
		 * Loop through splicingPrediction output file one line at a time and update annotations in variant pool
		 */
		// Read in splicingPrediction score in output CSV file
		Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up splicingPrediction score in : " + tableFile);
		System.out.println(tableFile);
		BufferedReader reader = new BufferedReader(new FileReader(tableFile));
		String line = reader.readLine(); 
		line = reader.readLine(); //Skip first line (headers)

		//read in probability for specific variant (output splicing scores in last column, may contain data for multiple transcripts)
		while(line != null) {
			try {
				//parse splicing output line
				String[] tokens = line.split("\t"); //Split line by tabs
				//Get chr, pos, ref, and alt from tokens, kinda like this (you may have to change the indices to match the output file):
				String chr = tokens[0];
				Integer pos = Integer.parseInt(tokens[1]);
				String ref = tokens[3];
				String alt = tokens[4];
				String spliceString = tokens[40]; // all splice output

				//Annotate variant if there is a splicing variant call
				if (spliceString.length() > 1) {
					//Find top scoring ALTscore-REFscore difference & get NM#(s) & score 
					String[] output = comparescore(spliceString);
					String spliceTopNM = output[0]; //top scoring NM number(s)
					Double spliceTopNMDiff = Double.parseDouble(output[1]); //top score
							

					//find variant				
					VariantRec var = variants.findRecord(chr, pos, ref, alt);
							
					if (var != null) {
						//assign annotation (string) & property (value)
						var.addAnnotation(VariantRec.SPLICING_ALL, spliceString); //string containing all splice output
						var.addAnnotation(VariantRec.SPLICING_TOPNM, spliceTopNM); //string containing highest scoring NM#(s)
						var.addProperty(VariantRec.SPLICING_TOPNMDIFF, spliceTopNMDiff); //number containing largest diff=ALTScore-REFScore
					}
					else {
						Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not find variant to associate with annotation at position " + chr + ":" + pos);
					}
				}
				
			}
			catch (NumberFormatException nfe) {
				
			}
			line = reader.readLine();
		}
	
		reader.close();				
	}
	
	/**
	 * This function takes the output string from scoreSpliceSites stored in MaxEntScan column and identifies
	 * the NM#(s) and maximum difference (REF score-ALT score) and corresponding NM#'s to identify variants
	 * with potential effects on introducing (+ diff) or removing (- diff) a splice site.
	 * @param line
	 * @throws OperationFailedException
	 */
	private static String[] comparescore(String line) {
		// Calculates ALT/REF diff and compares all the values for identified transcripts 
		String[] spliceNMTokens = line.split(";"); //split line by semicolons for each transcript
		
		// Find the NM transcript with the top scoring splicing diff score=ALTScore-REFScore
		String topNM="";
		Double topScoreDiff = Double.NaN;

		for (int i=0; i< spliceNMTokens.length; i++) {
			String candidate = spliceNMTokens[i];
			String[] NMTokens = candidate.split(":|,"); // split line by colons & commas

			String name = NMTokens[0]; //NM number
			Double refScore = Double.parseDouble(NMTokens[1]); //REF score
			Double altScore = Double.parseDouble(NMTokens[2]); //ALT score
			Double diff = altScore - refScore; //diff, i.e. ALTScore-REFScore

			//Find candidate with largest difference (postive or negative)
			if (i == 0) {
				//Initialize values with first candidate
				topNM = name; //NM Number
			topScoreDiff = diff; //diff=ALTScore-REFScore
			} 
			else if ((i > 0) && (java.lang.Double.compare(Math.abs(diff), Math.abs(topScoreDiff)) == 0)) {
				topNM = topNM + ";" + name;
			}	
			else if ((i > 0) && (java.lang.Double.compare(Math.abs(diff), Math.abs(topScoreDiff)) > 0)) {
				topNM = name; //NM Number
				topScoreDiff = diff; //diff=ALTScore-REFScore
			}			
		}
			
		String[] results={topNM, String.valueOf(topScoreDiff)};
		return results;		
	}
	
	/**
	 * Unlike usual annotators which do things one variant at time, we instead process everything in a single
	 * big batch. Since we use the 'Annotator.java' class as a base class we must implement the annotateVariant(...)
	 * method, but we don't actually use it. 
	 */
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		//OK, we actually don't do anything in here.... all annotations take place in 'prepare'
	}
	
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		//Annotated CSV file is a required arg
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());			
				if (obj instanceof CSVFile) {
					csvFile = (CSVFile)obj;
				}
				}
		}
		
		//Check to see if the required splicingPrediction path has been specified
		SpliceScriptPath = this.getAttribute(SPLICINGPREDICTION_PATH);
		if (SpliceScriptPath == null) {
			SpliceScriptPath = this.getPipelineProperty(SPLICINGPREDICTION_PATH);
			if (SpliceScriptPath == null) {
				throw new IllegalArgumentException("No path to splicingPrediction folder specified");
			}
		}
		
		//Test to see if scoreSpliceSites script is really there
		File scoreSpliceFile = new File(SpliceScriptPath + "/scoreSpliceSites");
		if (! scoreSpliceFile.exists()) {
			throw new IllegalArgumentException("No perl script file found on splicingPrediction path: " + SpliceScriptPath + "/scoreSpliceSites");
		}
		
		//Test to see if spliceRegions.bed file is really there
		File spliceBEDFile = new File(SpliceScriptPath + "/spliceRegions.bed");
		if (! spliceBEDFile.exists()) {
			throw new IllegalArgumentException("No BED file found on splicingPrediction path: " + SpliceScriptPath + "/spliceRegions.bed");
		}
	}		
	
	
	//Run program automatically
	//public static void main(String[] args) throws IOException {
	//	SplicingPredictionAnnotator sp = new SplicingPredictionAnnotator();
			
	//	sp.annotateVariant(new File("splice.final.vcf"));
		

}
