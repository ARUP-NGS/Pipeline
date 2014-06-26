package operator.examples;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import buffer.variant.VariantRec;

public class ExampleAnnotator extends Annotator {

	@Override
	/**
	 * Annotators add annotations to a VariantRec. 
	 */
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		
		//Usually, we look up something about the variant in some external database...
		
		//VariantRecs maintain textual and numeric annotations independently. 
		// "Annotations" are text, and "Properties" are numeric (doubles) 
		
		var.addProperty("random.property", Math.random() );
		
		var.addAnnotation("some.text.annotation", "some.text");
	}

}
