package operator.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
 * 1) Run MarcSingleton's scoreSpliceSites perl program
 * 2) Read in data one line at a time (per variant)
 * 3) Add annotation values for REF & ALT to annotations
 * @author elainegee
 *
 */

public class SplicingPredictionAnnotator extends Annotator {
	
	public static final String SPLICINGPREDICTION_PATH = "SplicingPrediction.path"; //path to spliceingPrediction/ folder (not script)
	
	
	CSVFile csvFile = null;
	String SpliceScriptPath = null;
	
	@Override
	protected void prepare() throws OperationFailedException {
		if (SpliceScriptPath == null) {
			throw new OperationFailedException("splicngPrediction path not specified", this);
		}
		
		//Run Marc Singleton's scoreSpliceSites on CSV file, i.e. annotation must have been previously done
		String command="perl " + SpliceScriptPath +"/scoreSpliceSites -v" + csvFile.getAbsolutePath() + " -s" + SpliceScriptPath +"/spliceRegions.bed";
		executeCommand(command);
				
		//Now read in re-annotated CSV and annotate variants
		File finalCSV = new File("splice.final.csv");
		
		//Associate all variants in file with those in variant pool
		annotateVariantsFromFile(finalCSV);

	}
	
	/**
	 * This function takes the output file that the script generates, reads one line at a time and parses 
	 * the chromosome, position, and splicing info from it, and associates the values with the  
	 * @param tableFile
	 * @param var
	 * @throws OperationFailedException
	 */
	private void annotateVariantsFromFile(File tableFile) throws OperationFailedException {
		/**
		 * Loop through splicingPrediction output file one line at a time and update annotations in variant pool
		 */
		// Read in splicingPrediction score in output CSV file
		Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up splicingPrediction score in : " + tableFile);
		BufferedReader reader = new BufferedReader(new FileReader(tableFile));
		String line = reader.readLine();
		line = reader.readLine(); //Skip first line
		
		while(line != null) {
			if (line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			//read in probability for specific variant (output from Marc's script is in 1 column, may contain data for multiple transcripts)

			//find variant
			try {
				String[] tokens = line.split("\t"); //Split line by tabs
				//Get chr, pos, ref, and alt from tokens, kinda like this (you may have to change the indices to match the output file):
				String chr = tokens[0];
				Integer pos = Integer.parseInt(tokens[1]);
				String ref = tokens[2];
				String alt = tokens[3];
				
				Double refScore = Double.parseDouble(tokens[??]); //Not sure what index will be
				Double altScore = Double.parseDouble(tokens[??]); //Not sure what index will be
				
				VariantRec var = variants.findRecord(chr, pos, ref, alt);
				
				
				
				if (var != null) {
					//assign annotation (string) & property (value)
					var.addProperty(VariantRec.SPLICING_SCORE_REF, refScore); //number
					var.addProperty(VariantRec.SPLICING_SCORE_ALT, altScore); //number
				}
				else {
					Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not find variant to associate with annotation at position " + chr + ":" + pos);
				}
			//	System.out.println("Adding bin #" + bin + " to variant : " + var.toSimpleString());
			}
			catch (NumberFormatException nfe) {
				
			}
			line = reader.readLine();
		}
	
		reader.close();				
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
	public static void main(String[] args) throws IOException {
		SplicingPredictionAnnotator sp = new SplicingPredictionAnnotator();
			
		sp.annotateVariant(new File("splice.final.vcf"));
		
		//For debugging, use ExcelWriter to double check variants are correctly assigned
		ExcelWriter ew = new ExcelWriter();
		
	}
		
}
