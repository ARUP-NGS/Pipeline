package operator.gene;

import gene.Gene;
import gene.GeneAnnotator;

import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.GeneList;

/**
 * Base class for operators that add 
 * @author brendan
 *
 */
public abstract class AbstractGeneAnnotator extends Operator implements GeneAnnotator {

	GeneList genes = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (genes == null) {
			throw new OperationFailedException("Gene list not initialized", this);
		}
		
		int count = 0;
		for(String name : genes.getGeneNames()) {
			Gene g = genes.getGeneByName(name);
			annotateGene(g);
			count++;
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Gene annotator " + getObjectLabel() + " annotated " + count + " genes");
	}
	

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof GeneList) {
					genes = (GeneList)obj;
				}
			}
		}
		
		if (genes == null)
			throw new IllegalArgumentException("No gene list provided to gene annotator " + getObjectLabel());
	}

	
}
