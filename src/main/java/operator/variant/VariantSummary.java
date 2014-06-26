package operator.variant;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.CSVFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.TextBuffer;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import operator.CommandOperator;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * Write a brief summary of a variant pool (or vcf or csv variant file) to
 * an output file
 * @author brendan
 *
 */
public class VariantSummary extends IOOperator {

	protected VariantPool variants = null;
	
	@Override
	public void performOperation() throws OperationFailedException {	
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		if (variants == null)
			throw new OperationFailedException("No input variants found", this);
		
		FileBuffer outputBuffer = super.getOutputBufferForClass(TextBuffer.class);
		PrintStream outWriter = System.out;
		try {
			outWriter = new PrintStream(new FileOutputStream(outputBuffer.getFile()));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		outWriter.println("Summary of variants");
		outWriter.println("Total variants : " + variants.size());
		outWriter.println("Total SNPs : " + variants.countSNPs());
		outWriter.println("Total indels: " + (variants.countInsertions() + variants.countDeletions()));
		outWriter.println("\t insertions: " + variants.countInsertions());
		outWriter.println("\t deletions: " + variants.countDeletions());
		outWriter.println(" Ts / Tv ratio: " + variants.computeTTRatio());
		
		int dbSNP = 0;
		int inGene = 0;
		int exonic =0 ;
		int inTKG = 0;
		
		for(String contig : variants.getContigs()) {
			for(VariantRec var : variants.getVariantsForContig(contig)) {
				
				String rsNum = var.getAnnotation(VariantRec.RSNUM); 
				if (rsNum != null && rsNum.length()>3) {
					dbSNP++;
				}
				
				Double popFreq = var.getProperty(VariantRec.POP_FREQUENCY);
				if (popFreq != null && popFreq > 0) {
					inTKG++;
				}
				
				String gene = var.getAnnotation(VariantRec.GENE_NAME);
				if (gene != null && gene.length()>2 && gene.length() < 10) {
					inGene++;
				}
				
				String exon = var.getAnnotation(VariantRec.VARIANT_TYPE);
				if (exon != null && exon.contains("exonic")) {
					exonic++;
					
				}
			}
		}

		DecimalFormat formatter = new DecimalFormat("#0.00");
		outWriter.println(" Variants in dbSNP: " + dbSNP + " ( " + formatter.format(100.0*dbSNP / variants.size()) + " )");
		outWriter.println(" Variants in 1000G: " + inTKG + " ( " + formatter.format(100.0*inTKG / variants.size()) + " )");
		outWriter.println(" Variants in genes: " + inGene + " ( " + formatter.format(100.0*inGene / variants.size()) + " )");
		outWriter.println(" Variants in exons: " + exonic + " ( " + formatter.format(100.0*exonic / variants.size()) + " )");
		
		outWriter.close();
	}

	

	public void initialize(NodeList children) {	
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		if (inputList != null) {
			NodeList inputChildren = inputList.getChildNodes();
			for(int i=0; i<inputChildren.getLength(); i++) {
				Node iChild = inputChildren.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof VariantPool) {
						variants = (VariantPool)obj;
						break;
					}
					if (obj instanceof VCFFile) {
						try {
							variants = new VariantPool((VCFFile)obj);
						} catch (IOException e) {
							e.printStackTrace();
							throw new IllegalArgumentException("Could not read vcf file : " + obj.toString());
						}
						break;
					}
					
					if (obj instanceof CSVFile) {
						try {
							variants = new VariantPool((CSVFile)obj);
						} catch (IOException e) {
							e.printStackTrace();
							throw new IllegalArgumentException("Could not read csv file : " + obj.toString());
						}
					}
				}
			}
		}
		
		if (outputList != null) {
			NodeList outputChilden = outputList.getChildNodes();
			for(int i=0; i<outputChilden.getLength(); i++) {
				Node iChild = outputChilden.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof FileBuffer) {
						addOutputBuffer( (FileBuffer)obj );
					}
					else {
						throw new IllegalArgumentException("Found non-FileBuffer object in output list for Operator " + getObjectLabel());
					}
				}
			}
		}
		
		if ( requiresReference() ) {
			ReferenceFile ref = (ReferenceFile) getInputBufferForClass(ReferenceFile.class);
			if (ref == null) {
				throw new IllegalArgumentException("Operator " + getObjectLabel() + " requires reference file, but none were found");
			}
		}
	}

}
