package operator.variant;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import buffer.variant.VariantRec;

/**
 * This annotator computes the ESP 6500 exomes frequency, and adds it as the VariantRec.EXOMES_FREQ
 * annotation. The raw data is looked up in a VCF file that can be obtained from : http://evs.gs.washington.edu/EVS/
 * It must then be tabix-indexed prior to use
 * @author brendan
 *
 */
public class ESP6500Annotator extends Annotator {

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		
	}

	
}
