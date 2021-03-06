package operator.variant;

import util.Interval;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.BEDFile;
import buffer.IntervalsFile;
import buffer.variant.VariantRec;
import operator.OperationFailedException;

/**
 * An annotator that flags all variants overlapping the BED file with the "bad.region" annotation
 * @author brendan, added into pipeline.jar by chrisk
 *
 */
public class BadRegionAnnotator extends AbstractRegionAnnotator {

	public static final String BAD_REGION = "bad.region";
	
	BEDFile badIntervals = null;
	
	@Override
	protected IntervalsFile getIntervals() {
		return badIntervals;
	}

	@Override
	protected void doContainsAnnotation(VariantRec var) {
		var.addAnnotation(BAD_REGION, "true");
	}
	
	@Override
	public void initialize(NodeList children) {
	
		super.initialize(children);
		
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof BEDFile) {
					this.badIntervals = (BEDFile)obj; //remove this if you want to use properties file
				}
			}
		}
/**
 * If you want to set the bad regions file in the pipeline_properties.xml, use the below code
 * 
		String badregionsfile = getPipelineProperty("bad.region.bed");
		if(badregionsfile == null){
			try {
				throw new IOException("PipelineProperty 'bad.region.bed' not defined.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		IntervalsFile intervals = new BEDFile(new File(badregionsfile));
		try {
			intervals.buildIntervalsMap();
		} catch (IOException e) {
			e.printStackTrace();
		}


	    this.badIntervals = (BEDFile) intervals;
**/
		if (this.badIntervals == null) {
			throw new IllegalArgumentException("No BED file found");
		}
}

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (this.badIntervals == null) {
			throw new OperationFailedException("Intervals were not initialized", this);
		}
		
		if (this.badIntervals.intersects(var.getContig(), new Interval(var.getStart(), var.getEnd()))) {
			doContainsAnnotation(var);
		} else {
			doNotContainsAnnotation(var);
		}
		
	}
}
