package operator.annovar;

import java.text.DecimalFormat;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Base class for things that can take a variant pool and add an annotation of some sort to
 * the variants
 * @author brendan
 *
 */
public abstract class Annotator extends Operator {

	protected VariantPool variants = null;
	

	/**
	 * Compute or obtain an annotation for the given variant and add it to the list of
	 * annotations or properties stored for the variant
	 * @param var
	 * @throws OperationFailedException 
	 */
	public abstract void annotateVariant(VariantRec var) throws OperationFailedException;
	
	/**
	 * If true, we write some progress indicators to system.out
	 * @return
	 */
	protected boolean displayProgress() {
		return false;
	}
	
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("No variant pool specified", this);
		
		DecimalFormat formatter = new DecimalFormat("#0.00");
		int tot = variants.size();
		
	
		prepare();
		
		int varsAnnotated = 0;
		
		for(String contig : variants.getContigs()) {
			for(VariantRec rec : variants.getVariantsForContig(contig)) {
				annotateVariant(rec);
				
				varsAnnotated++;
				double prog = 100 * (double)varsAnnotated  / (double) tot;
				if (displayProgress() && varsAnnotated % 2000 == 0) {
					System.out.println("Annotated " + varsAnnotated + " of " + tot + " variants  (" + formatter.format(prog) + "% )");	
				}
			}
		}
			
		cleanup();
	}
	
	/**
	 * This method is called prior to annotation of the variants. It's a no-op by default,
	 * but various subclasses may override if they need to do some work before 
	 * the annotation process begins (looking at you, VarBinAnnotator)
	 */
	protected void prepare() throws OperationFailedException {
		//Blank on purpose, subclasses may override 
	}
	
	protected void cleanup() throws OperationFailedException {
		//Blank on purpose, subclasses may override
	}
	
	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VariantPool) {
					variants = (VariantPool)obj;
				}

			}
		}
	}
}
