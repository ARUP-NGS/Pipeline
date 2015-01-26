package operator.variant;

import java.io.IOException;
import java.util.logging.Logger;

import json.JSONException;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.vcfParser.VCFMetricsAnnotators;
import util.vcfParser.VCFParser;
import util.vcfParser.VCFParser.GTType;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * This operator identifies variants that are NOT found in a given variant pool, but "should be",
 * given their frequency in the population (based on, for instance, 1000 Genomes or UK10K data). 
 * Such "variants" may be present in the reference, but the "alternate" is at 99% in the population,
 * and their absence (if detected) in this sample might be interesting. Here, we look for common
 * variants not found in the sample and add new variants to the given VariantPool to identify them. 
 * @author brendan
 *
 */
public class MissingCommonVarFinder extends Operator {

	//Determines minimum frequency of variant in 'common variants' to consider including
	public static final String FREQUENCY_THRESHOLD = "freq.threshold";
	
	private VariantPool variants = null;
	private BAMFile bamFile = null;
	private BEDFile capture = null;
	private VCFFile commonVars = null;

	//Variants above this frequency threshold in the 'common vars' vcf
	//but not found in the variantPool will be added
	private Double frequencyThreshold = 0.95;
	
	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		
		capture.buildIntervalsMap();
		
		String freqAttr = this.getAttribute(FREQUENCY_THRESHOLD);
		if (freqAttr != null) {
			frequencyThreshold = Double.parseDouble(freqAttr);
		}
		Logger.getLogger(Pipeline.primaryLoggerName).info("Missing common vars using frequency threshold of " + frequencyThreshold);

		
		//We iterate over the variants in the input VCF
		VCFParser parser = new VCFParser(commonVars);
		parser.setFailIfNoSource(false);
		parser.addVCFMetricsAnnotator(new VCFMetricsAnnotators.AFAnnotator());
		
		
		int count = 0;
		int added = 0;
		while(parser.advanceLine()) {
			VariantRec var = parser.toVariantRec();
			double freq = var.getProperty(VariantRec.AF);
			count++;
	
			//System.out.println("Depth at site: "+ var.toString() + "\t:\t" + bamFile.depthAtSite(var.getContig(), var.getStart()));
			
			//If this variant is in our capture region AND we didn't find it, then it's a hit!
			if (freq > frequencyThreshold
					&&  capture.contains(var.getContig(), var.getStart(), false)
					&&  (variants.findRecordNoWarn(var.getContig(), var.getStart())==null)) {
				
				//Is there coverage at the site? 
				int depth = bamFile.depthAtSite(var.getContig(), var.getStart());
				if (depth > 2) {
					VariantRec newVar = new VariantRec(var.getContig(), 
											var.getStart(),
											var.getEnd(),
											var.getRef(),
											var.getRef(), //NOT a typo: The 'alt' allele here is the SAME as the reference
											10.0,
											"1/1",
											GTType.HOM);

					System.out.println("Adding variant " + newVar.toString());
					added++;
					variants.addRecord(newVar);
				}
			}
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Missing common vars identified " + added + " new variants");
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof BAMFile) {
					bamFile = (BAMFile)obj;
				}
				if (obj instanceof VariantPool) {
					variants = (VariantPool)obj;
				}
				if (obj instanceof VCFFile) {
					commonVars = (VCFFile)obj;
				}
				if (obj instanceof BEDFile) {
					capture = (BEDFile)obj;
				}
			}
		}
		
		if (bamFile == null) {
			throw new IllegalArgumentException("A BAM file must be provided t the MissingCommonVars annotator");
		}
		if (variants == null) {
			throw new IllegalArgumentException("A variant pool must be provided t the MissingCommonVars annotator");
		}
		if (commonVars == null) {
			throw new IllegalArgumentException("A VCF file must be provided t the MissingCommonVars annotator");
		}
		if (capture == null) {
			throw new IllegalArgumentException("A BED file must be provided t the MissingCommonVars annotator");
		}
	}
}
