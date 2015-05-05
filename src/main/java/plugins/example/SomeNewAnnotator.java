package plugins.example;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import buffer.variant.VariantRec;

public class SomeNewAnnotator extends Annotator {

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		
		var.addAnnotation("some.annotation", "fisticuffs");
		
	}

}
