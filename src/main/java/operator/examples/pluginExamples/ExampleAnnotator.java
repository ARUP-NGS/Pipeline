package operator.examples.pluginExamples;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import buffer.variant.VariantRec;

/**
 * Just a very simple example Annotator that demonstrates some plugin functionality. 
 * @author brendan
 *
 */
public class ExampleAnnotator extends Annotator {

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		
		var.addAnnotation("example.new.annotation", "Bananas are my favorite");
	}

}
