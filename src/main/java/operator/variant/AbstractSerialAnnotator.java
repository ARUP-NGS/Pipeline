package operator.variant;

import java.util.ArrayList;
import java.util.Collections;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import buffer.variant.VariantRec;

/**
 * This is (well, should be) the base class for all annotators that need to run
 * their procedures one contig at a time due to memory constraints. 
 * 
 * @author daniel
 *
 */
public abstract class AbstractSerialAnnotator extends Annotator {

	private boolean initialized = false;
	
	/**
	 * Subclasses should override this method to actually perform the annotations.
	 * The VariantRec is the variant to annotate, and the 'line' argument is the information
	 * we get from the VCF. Usually, annotators will extract some information from the line 
	 * (like allele frequency, dbSNP ids, etc. etc) and turn that into an annotation or property 
	 * for the VariantToAnnotate
	 * @param var
	 * @param vcfLine
	 */
	protected abstract boolean addAnnotationsFromString(VariantRec variantToAnnotate, String vcfLine);
	
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
	 * Have it return an empty ArrayList if necessary.
	 * @param contig
	 */
	protected abstract ArrayList<Object> runForContig(String contig);
	
	/**
	 * This actually annotates the variant - it performs the annotation on the VariantRec in
	 * the VariantPool
	 */
	@Override
	public abstract void annotateVariant(VariantRec varToAnnotate) throws OperationFailedException;

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
		ArrayList<ArrayList<Object>> returnValues = new ArrayList<ArrayList<Object>>();
		ArrayList<ArrayList<Object>> runReturnValues = new ArrayList<ArrayList<Object>>();
		for(String contig: contigs){
			returnValues.add(prepareContig(contig));
			runReturnValues.add(runForContig(contig));
		}
		
		/*
		 * Throw whatever processing of this information you might need into this slot.
		 */
		
	}

	
	
}
