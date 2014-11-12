package operator.variant;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import operator.writer.VarViewerWriter;
import pipeline.Pipeline;
import util.Interval;
import buffer.CSVFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * This is (well, should be) the base class for all annotators that need to run
 * their procedures one contig at a time due to memory constraints. 
 * 
 * @author daniel
 *
 */
public abstract class AbstractSerialAnnotator extends Annotator {
	
	ArrayList<ArrayList<Object>> returnValues = new ArrayList<ArrayList<Object>>();
	ArrayList<ArrayList<Object>> runReturnValues = new ArrayList<ArrayList<Object>>();
	ArrayList<File> outputFiles = new ArrayList<File>();
	Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
	
	private boolean initialized = false;
	

	
	/**
	 * This is called by prepare(), which is being overridden from the standard Annotator 
	 * to loop through and annotate one contig at a time. Place whatever material
	 * you would typically use in prepare in here, but one contig at a time.
	 * It returns an ArrayList of Objects for the purpose of being able to return useful
	 * information from each use of this method.
	 * Have it return an empty ArrayList if necessary.
	 * @param contig
	 */
	protected abstract ArrayList<Object> prepareContig(String contig);
	
	
	/**
	 * This is called by prepare(), which is being overridden from the standard Annotator 
	 * to loop through and annotate one contig at a time.
	 * The purpose of this class is to permit any parsing, shell calls, etc., which might
	 * be necessary before the final annotation begins, but isn't necessarily contained in 
	 * prepareContig.
	 * Left blank on purpose. Subclasses may override.
	 * @param contig
	 */
	protected ArrayList<Object> runForContig(String contig) {
		return null;
	}

	
	/*Functions annotateVariant and addAnnotationsFromString are both
	options for annotation, depending on the style of annotator.
	
	*/
	
	/**
	 * This actually annotates the variant - it performs the annotation on the VariantRec in
	 * the VariantPool
	 */
	@Override
	public abstract void annotateVariant(VariantRec varToAnnotate) throws OperationFailedException;

	
	/**
	 * Subclasses should override this method to actually perform the annotations.
	 * The VariantRec is the variant to annotate, and the 'line' argument is the information
	 * we get from the VCF. Usually, annotators will extract some information from the line 
	 * (like allele frequency, dbSNP ids, etc. etc) and turn that into an annotation or property 
	 * for the VariantToAnnotate
	 * 
	 * Blank on purpose - subclasses can override.
	 * @param var
	 * @param vcfLine
	 */
	protected boolean addAnnotationsFromString(VariantRec variantToAnnotate, String vcfLine) {
		return false;
	}
	
	
	
	/**
	 * This prepares everything needed before the actual annotation. This can
	 * (and often should) be overridden by the inheriting class. For example,
	 * the running of an annotator, 
	 * 
	 */
	@Override
	public void prepare() {
		ArrayList<String> contigs = new ArrayList<String>(variants.getContigs());
		Collections.sort(contigs);
		for(String contig: contigs){
			returnValues.add(prepareContig(contig));
			runReturnValues.add(runForContig(contig));
		}

		
		/*
		 * Throw whatever processing of this information you might need into this slot.
		 */
		
	}

	/*
	 * 
	 */
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("No variant pool specified", this);
		logger.info("About to split VariantPool by contig.");
		LinkedHashMap<String, VariantPool> VariantPoolSet = variants.splitPoolByContigs(variants);
		
		DecimalFormat formatter = new DecimalFormat("#0.00");
		ArrayList<String> contigFilenames = new ArrayList<String>();
		for(String key : VariantPoolSet.keySet()) {
			VariantPool workingPool = VariantPoolSet.get(key);
			
			CSVFile tempCSV = new CSVFile(new File(this.getProjectHome() + "/" + this.objectLabel + "." + key + ".csv.var"));
				
			int tot = workingPool.size();
			
			prepare();
			contigFilenames.add(tempCSV.getAbsolutePath());
			int varsAnnotated = 0;
	
			for(String contig : workingPool.getContigs()) {
				for(VariantRec rec : workingPool.getVariantsForContig(contig)) {
					//TODO veryify that recEnd is not incorrect for two alt alleles
					Integer recLength = rec.getRef().length() - rec.getAlt().length(); //if >0 Indicates a deletion
					if (recLength < 0) { //Indicates that it is an insertion
						recLength = 0; 
					} else if (recLength == 0) { //Indicates that it is an snv or mnv
						if (rec.getRef().length() == 1) { // Indicates that it is an snv
							recLength = 1;
						} else { // Indicates that it is an mnv
							recLength = rec.getRef().length();
						}
					}
					Integer recEnd = rec.getStart() - 1 + recLength;
					Interval recInterval = new Interval(rec.getStart() - 1, recEnd);
					if (bedFile == null || bedFile.intersects(rec.getContig(), recInterval)) {
						annotateVariant(rec);
					
						varsAnnotated++;
						double prog = 100 * (double)varsAnnotated  / (double) tot;
						if (displayProgress() && varsAnnotated % 2000 == 0) {
							System.out.println("Annotated " + varsAnnotated + " of " + tot + " variants  (" + formatter.format(prog) + "% )" + " for contig: " + contig);	
						}
					}
				}
			}
			
			VarViewerWriter tempWrite = new VarViewerWriter();
			tempWrite.variants = workingPool;
			tempWrite.outputFile = tempCSV;
			logger.info("About to write variants for contig " + key + " to file " + tempCSV.getAbsolutePath());
			tempWrite.performOperation();
			
			
		
			cleanup();
		}
	
	}
}