package operator.gene;

import java.util.logging.Logger;
import java.util.logging.Level;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.GeneList;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;

/**
 * Create a gene list for every gene found in the variant pool. This also 
 * @author brendan
 *
 */
public  class GeneListFromVariants extends Operator {
	
	VariantPool vars = null;
	GeneList genes = null;
        
	
	@Override
	public void performOperation() throws OperationFailedException {
                Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
    	        for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
                                try {
                                    JSONArray snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
                                    String geneName = null;
                                    for(int i=0; i<snpeff_annos.length(); i++) {
                                        JSONObject jobj = (JSONObject)snpeff_annos.get(i); 
                                        geneName = (String)jobj.get(VariantRec.GENE_NAME);
                                        if (geneName != null) break;
                                    }

				    if (geneName != null) {
					if (! genes.containsGene(geneName)) {
						genes.addGene(geneName);
					}
					var.setGene( genes.getGeneByName(geneName) );
				    }
                                } catch (JSONException ex) {
                                    ex.printStackTrace();
                                    logger.log( Level.WARNING, "GeneListFromVars Error:", ex );
                                }
			}
		}
		logger.info("Created a gene list with " + genes.size() + " entries from " + vars.size() + " variants");
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
				if (obj instanceof VariantPool) {
					vars = (VariantPool)obj;
				}
			}
		}
		
		if (vars == null) {
			throw new IllegalArgumentException("No variant list provided to gene list creator " + getObjectLabel());
		}
		if (genes == null)
			throw new IllegalArgumentException("No gene list provided to gene list creator " + getObjectLabel());
	}
	
	
}
