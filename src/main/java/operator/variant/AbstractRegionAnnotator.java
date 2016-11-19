package operator.variant;


import java.io.IOException;

import buffer.IntervalsFile;
import buffer.variant.VariantRec;
import operator.OperationFailedException;
import operator.annovar.Annotator;

/**
 * A base class for annotators that annotate variant based on whether or not a BED file contains each variant. 
 * @author brendanofallon
 *
 */
public abstract class AbstractRegionAnnotator extends Annotator {

	private IntervalsFile intervals = null;
	
	/**
	 * Return the IntervalsFile that defines the intervals used for annotation
	 * @return
	 */
	protected abstract IntervalsFile getIntervals();
	
	/**
	 * This is called whenever the set of intervals defined by getIntervals() happens to contain the given variant. This
	 * means we actually want to annotate the variant. 
	 * @param var
	 */
	protected abstract void doContainsAnnotation(VariantRec var);
	
	/**
	 * Called for each variant that DOESN'T fall into the defined regions. Might be useful. 
	 * @param var
	 */
	protected void doNotContainsAnnotation(VariantRec var) {
		//no-op by default
	}
	
	/**
	 * Set the intervals use for annotation
	 */
	@Override 
	public void prepare() {
		this.intervals = getIntervals();
		try {
			intervals.buildIntervalsMap();
		} catch (IOException e) {
			throw new IllegalArgumentException("Error initializing intervals: " + e.getLocalizedMessage());
		}
	}
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (this.intervals == null) {
			throw new OperationFailedException("Intervals were not initialized", this);
		}
		
		if (this.intervals.contains(var.getContig(), var.getStart())) {
			doContainsAnnotation(var);
		} else {
			doNotContainsAnnotation(var);
		}
		
	}

}
