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
 * An annotator that flags all variants falling into the BED file with the "low.complex.region" annotation
 * @author chrisk (heavily influenced by bad.regions annotator)
 *
 */
public class LowComplexityRegionsAnnotator extends AbstractRegionAnnotator{
	public static final String LOW_COMPLEX_REGION = "low.complex";
	
	BEDFile lowComplexityIntervals = null;
	
	@Override
	protected IntervalsFile getIntervals() {
		return lowComplexityIntervals;
	}

	@Override
	protected void doContainsAnnotation(VariantRec var) {
		var.addAnnotation(LOW_COMPLEX_REGION, "true");
	}
	
	@Override
	public void initialize(NodeList children) {
	
		super.initialize(children);
		
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof BEDFile) {
					this.lowComplexityIntervals = (BEDFile)obj; //remove this if you want to use properties file
				}
			}
		}		
/**
 * If you want to set the low regions file in the pipeline_properties.xml, use the below code
 * 		String lowcomplexregionsfile = getPipelineProperty("low.complex.region");
 
		if(lowcomplexregionsfile == null){
			try {
				throw new IOException("PipelineProperty 'low.complex.region' not defined.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		IntervalsFile intervals = new BEDFile(new File(lowcomplexregionsfile));
		try {
			intervals.buildIntervalsMap();
		} catch (IOException e) {
			e.printStackTrace();
		}


	    this.lowcomplexityintervals = (BEDFile) intervals;
**/
		if (this.lowComplexityIntervals == null) {
			throw new IllegalArgumentException("No BED file found");
		}
}

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (this.lowComplexityIntervals == null) {
			throw new OperationFailedException("Intervals were not initialized", this);
		}
		
		if (this.lowComplexityIntervals.intersects(var.getContig(), new Interval(var.getStart(), var.getEnd()))) {
			doContainsAnnotation(var);
		} else {
			doNotContainsAnnotation(var);
		}
		
	}
}
