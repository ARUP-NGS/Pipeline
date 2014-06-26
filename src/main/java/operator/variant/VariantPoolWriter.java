package operator.variant;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.CSVFile;
import buffer.GeneList;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * VariantPoolWriters write a list of variants to a file. Various subclasses
 * use different formatting schemes, and may expect various annotations to be
 * present among the variants.
 * @author brendan
 *
 */
public abstract class VariantPoolWriter extends Operator {
	
	protected VariantPool variants = null;
	protected List<VariantPool> additionalVariants = new ArrayList<VariantPool>(); //Optional storage for additional variant lists
	protected CSVFile outputFile = null;
	protected GeneList genes = null; //Optional parameter
	private Comparator<VariantRec> recSorter = null;
	
	/**
	 * Write a suitable header for the output file
	 * @param outputStream
	 */
	public abstract void writeHeader(PrintStream outputStream);
	
	/**
	 * If true, will throw an error if no GeneList provided
	 * @return
	 */
	public boolean requiresGeneList() {
		return false;
	}
	
	/**
	 * Set the gene list associated with this writer
	 * @return
	 */
	public void setGenes(GeneList genes) {
		this.genes = genes;
	}
	
	/**
	 * Write the given variant record to the given output stream
	 * @param rec
	 * @param outputStream
	 */
	public abstract void writeVariant(VariantRec rec, PrintStream outputStream);
	
	/**
	 * Provide a sorting mechanism for records
	 * @param sorter
	 */
	public void setComparator(Comparator<VariantRec> sorter) {
		this.recSorter = sorter;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("VariantPoolWriter attempting to write variants to file");
		if (variants == null) {
			logger.severe("No variant pool found, could not write variants");
			throw new OperationFailedException("Variant pool not specified", this);
		}
		
		try {
			PrintStream outStream = System.out;
			if (outputFile != null) {
				logger.info("VariantWriter is writing to file : " + outputFile.getAbsolutePath());
				outStream = new PrintStream(new FileOutputStream( outputFile.getFile()));
				
			}
			
			writeHeader(outStream);
			
			int tot = 0;
			
			if (recSorter != null) {
				List<VariantRec> varList = variants.toList();
				Collections.sort(varList, recSorter);
				for(VariantRec var : varList) {
					writeVariant(var, outStream);
					tot++;
				}
			}
			else {
				Collection<String> contigCollection = variants.getContigs();

				//Sort contigs so they're always in the same order
				String[] contigs = contigCollection.toArray(new String[]{});
				Arrays.sort(contigs);
			
				for(int i=0; i<contigs.length; i++) {
					String contig = contigs[i];
					List<VariantRec> vars = variants.getVariantsForContig(contig);
					for(VariantRec rec : vars) {
						writeVariant(rec, outStream);
						tot++;
					}
				}
			}
			
			logger.info("VariantWriter wrote " + tot + " variants in " + variants.getContigCount() + " contigs to output.");
			outStream.close();
		} catch (FileNotFoundException e) {
			throw new OperationFailedException("Could not write to file : " + outputFile.getFile().getAbsolutePath(), this);
		}
		
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VariantPool) {
					if (variants == null)
						variants = (VariantPool)obj;
					else {
						additionalVariants.add( (VariantPool)obj);
					}
				}
				if (obj instanceof CSVFile) {
					outputFile = (CSVFile)obj;
				}

				if (obj instanceof GeneList) {
					genes = (GeneList)obj;
				}
			}
		}
		
//		if (outputFile == null) {
//			throw new IllegalArgumentException("Output CSV file not specified");
//		}

		if (variants == null) {
			throw new IllegalArgumentException("Variant pool not specified");
		}
		
		if (requiresGeneList() && genes == null) {
			throw new IllegalArgumentException("GeneList required but not provided");
		}
	}


}
