package operator.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.CSVFile;
import buffer.variant.VariantRec;

import operator.annovar.Annotator;
import operator.variant.ExcelWriter;


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
		executeCommand(command)
				
		//Now read in re-annotated CSV and annotate variants
		File finalCSV = new File("splice.final.csv");
		try {
			annotateVariant(finalCSV);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Cannot annotate variant: " + e.getMessage(), this);
		}

	}
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
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
				Integer bin = Integer.parseInt(binStr);
				VariantRec var = variants.findRecord(contig, pos);
				if (var != null) {
					//assign annotation (string) & property (value)
					var.addProperty(VariantRec.VARBIN_BIN, new Double(bin)); //number
					var.addAnnotation(VariantRec.VARBIN_BIN, new Double(bin)); //string
				}
				else {
					Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not find variant to associate with varbin annotation at position " + contig + ":" + pos);
				}
			//	System.out.println("Adding bin #" + bin + " to variant : " + var.toSimpleString());
			}
			catch (NumberFormatException nfe) {
				
			}
			line = reader.readLine();
		}
	
		reader.close();				
	}
	
	
	protected void executeCommand(String command) throws OperationFailedException {
		//Handle running script within pipeline
		Runtime r = Runtime.getRuntime();
		Process p;
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info(getObjectLabel() + " executing command : " + command);
		try {
			p = r.exec(command);

			try {
				if (p.waitFor() != 0) {
					logger.info("Task with command " + command + " for object " + getObjectLabel() + " exited with nonzero status");
					throw new OperationFailedException("Task terminated with nonzero exit value : " + System.err.toString() + " command was: " + command, this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Task was interrupted : " + System.err.toString() + "\n" + e.getLocalizedMessage(), this);
			}

		}
		catch (IOException e1) {
			throw new OperationFailedException("Task encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
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
	
	protected void cleanup() throws OperationFailedException {
		//Delete output file from Marc's script
	}
	
	//Run program automatically
	public static void main(String[] args) throws IOException {
		SplicingPredictionAnnotator sp = new SplicingPredictionAnnotator();
			
		sp.annotateVariant(new File("splice.final.vcf"));
		
		//For debugging, use ExcelWriter to double check variants are correctly assigned
		ExcelWriter ew = new ExcelWriter();
		
	}
		
}
