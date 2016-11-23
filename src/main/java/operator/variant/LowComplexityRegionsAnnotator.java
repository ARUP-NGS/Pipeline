package operator.variant;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.BEDFile;
import buffer.IntervalsFile;
import buffer.variant.VariantRec;
/**
 * An annotator that flags all variants falling into the BED file with the "low.complex.region" annotation
 * @author chrisk (heavily influenced by bad.regions annotator)
 *
 */
public class LowComplexityRegionsAnnotator extends AbstractRegionAnnotator{
	public static final String LOW_COMPLEX_REGION = "low.complex";
	
	BEDFile lowcomplexityintervals = null;
	
	@Override
	protected IntervalsFile getIntervals() {
		return lowcomplexityintervals;
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
			}
		}		
		String lowcomplexregionsfile = getPipelineProperty("low.complex.region");
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
		if (this.lowcomplexityintervals == null) {
			throw new IllegalArgumentException("No BED file found");
		}
}
}
