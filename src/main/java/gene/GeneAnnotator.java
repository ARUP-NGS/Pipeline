package gene;

import operator.OperationFailedException;


/**
 * Anything that is capable of adding an annotation (or property) to
 * a gene
 * @author brendan
 *
 */
public interface GeneAnnotator {

	/**
	 * Add the annotation to the given gene 
	 * @param g
	 * @throws OperationFailedException 
	 */
	public void annotateGene(Gene g) throws OperationFailedException;
	
	
}
