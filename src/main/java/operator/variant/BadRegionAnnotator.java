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
 * An annotator that flags all variants falling into the BED file with the "bad.region" annotation
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
			}
		}		
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
		if (this.badIntervals == null) {
			throw new IllegalArgumentException("No BED file found");
		}
}

}
