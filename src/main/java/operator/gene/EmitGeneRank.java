package operator.gene;

import gene.Gene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ncbi.GeneInfoDB;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.GeneList;

public class EmitGeneRank extends Operator {

	GeneList genes = null;
	GeneInfoDB infoDB = null;
	
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		List<Gene> rankers = new ArrayList<Gene>();
		
		for(Gene g : genes.getAllGenes()) {
			double score = 0;
			if (g.getProperty(Gene.SUMMARY_SCORE) != null) {
				score += g.getProperty(Gene.SUMMARY_SCORE);
			}
			
			if (g.getProperty(Gene.PUBMED_SCORE) != null) {
				score += g.getProperty(Gene.PUBMED_SCORE);
			}
			
			if (g.getProperty(Gene.DBNSFPGENE_SCORE) != null) {
				score += g.getProperty(Gene.DBNSFPGENE_SCORE);
			}
			
			if (g.getProperty(Gene.GO_SCORE) != null) {
				score += g.getProperty(Gene.GO_SCORE);
			}
			
			if (g.getProperty(Gene.INTERACTION_SCORE) != null) {
				score += g.getProperty(Gene.INTERACTION_SCORE);
			}
			
			g.addProperty(Gene.GENE_RELEVANCE, score);
			rankers.add(g);
			
		}
		
		Collections.sort(rankers, new RelSorter());
		
//		for(Gene gene : rankers) {
//			emitGene(gene);
//		}
		
	}
	
	/**
	 * Hah! We don't actually annotate genes, we just write gene info to system out
	 */
	public void emitGene(Gene g) throws OperationFailedException {
		if (infoDB == null) {
			infoDB = GeneInfoDB.getDB();
		}
		
		if (g.getProperty(Gene.GENE_RELEVANCE) > 0) {
			System.out.println(g.getName() + " : " + g.getProperty(Gene.GENE_RELEVANCE));
		}		

	}

	
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
			throw new IllegalArgumentException("No gene list provided to gene ranker " + getObjectLabel());
	}



	class RelSorter implements Comparator<Gene> {

		@Override
		public int compare(Gene g0, Gene g1) {
			Double s0 = g0.getProperty(Gene.GENE_RELEVANCE);
			Double s1 = g1.getProperty(Gene.GENE_RELEVANCE);
			
			if (s0== null && s1 == null)
				return 0;
			
			if (s0 == null && s1 != null)
				return -1;
			
			if (s0 != null && s1 == null)
				return 1;
			
			if (s0.equals(s1)) {
				return 0;
			}
			
			return s0 < s1 ? 1 : -1;
			
		}
		
	}

}
