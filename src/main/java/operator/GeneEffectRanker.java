package operator;

import gene.Gene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.GeneList;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * A new and much cleaner implementation of the gene-effect-relevance-ranking procedure
 * The basic idea is that we have a list of genes with annotations and a list of variants
 * with annotations, and we examine each independently ...
 * @author brendan
 *
 */
public class GeneEffectRanker extends Operator {
	
	GeneList genes = null;
	VariantPool vars = null;

	
	Map<String, Integer> searchTerms = null;
	Map<String, Integer> goTerms = null;
	List<String> interactionGenes = null;
	
	
	@Override
	public void performOperation() throws OperationFailedException {

		List<VariantRec> topHits = new ArrayList<VariantRec>();

		
		
		//Ensure each variant is associated with a gene
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				Gene g = var.getGene();
				if (g == null && var.getAnnotation(VariantRec.GENE_NAME) != null) {
					g = genes.getGeneByName( var.getAnnotation(VariantRec.GENE_NAME));
					
					if (g != null)
						var.setGene(g);
				}
			}
		}
		
		int skipped = 0;
		int geneHits = 0;
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				
				Double damageScore = var.getProperty(VariantRec.EFFECT_PREDICTION2);
				if (damageScore == null)
					damageScore = var.getProperty(VariantRec.SVM_EFFECT);
				
				if (damageScore != null && damageScore > 0.0) {
					if (var.getGene() == null) {
						System.err.println("Variant " + var + " has functional damage but no gene set, cannot compute effect productc!"); 
					}
				}
				
				
				double effectProd = computeScore(var);
				var.addProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT, effectProd);
				
				if (var.getGene() != null) {
					Double rel = var.getGene().getProperty(Gene.GENE_RELEVANCE);
					if (rel != null && rel > 0)
						geneHits++;
				}
				
				if (effectProd > 0) {
					topHits.add(var);
					Collections.sort(topHits, new ScoreComparator());
				}

			}
		}
		
		if (vars == null || vars.size()==0) {
			for(Gene g : genes.getAllGenes()) {
				computeGeneRelevance(g);
			}
		}

		System.out.println("Found gene hits for " + geneHits + " of " + genes.size() + " total genes");
		System.out.println("Skipped " + skipped + " of " + vars.size() + " total variants");
	}
	
	/**
	 * Compute overall gene 'relevance'
	 * @param g
	 */
	private double computeGeneRelevance(Gene g) {
		Double summaryScore = g.getProperty(Gene.SUMMARY_SCORE);
		Double abstractsScore = g.getProperty(Gene.PUBMED_SCORE);
		Double goScore = g.getProperty(Gene.GO_SCORE);
		Double dbNSFPScore = g.getProperty(Gene.DBNSFPGENE_SCORE);
		Double interactionScore = g.getProperty(Gene.INTERACTION_SCORE);
		Double expressionScore = g.getProperty(Gene.EXPRESSION_SCORE);
		Double omimScore = g.getProperty(Gene.OMIM_PHENOTYPE_SCORE);
		

		if (summaryScore == null)
			summaryScore = 0.0;
		if (abstractsScore == null)
			abstractsScore = 0.0;
		if (goScore == null)
			goScore = 0.0;
		if (dbNSFPScore == null)
			dbNSFPScore = 0.0;
		if (interactionScore == null)
			interactionScore = 0.0;
		if (expressionScore == null)
			expressionScore = 0.0;
		if (omimScore == null)
			omimScore = 0.0;
		
		Double relevanceScore = (goScore + summaryScore + abstractsScore/2.0 + dbNSFPScore + 10*interactionScore + expressionScore + omimScore);
		g.addProperty(Gene.GENE_RELEVANCE, relevanceScore);
		return relevanceScore;
	}
	
	/**
	 * Compute the combined ranking score for the given variant. 
	 * @param var
	 * @return
	 */
	protected double computeScore(VariantRec var) {
		Double varScore = var.getProperty(VariantRec.EFFECT_PREDICTION2);
		if (varScore == null) {
			varScore = var.getProperty(VariantRec.SVM_EFFECT);
		}
		
		if (varScore==null) //Protect against future null pointer exceptions
			varScore = 0.0;
		
		Gene g = var.getGene();
	
		if (g==null)
			return 0.0;
		
		double relevanceScore = computeGeneRelevance(g);
		
		
		Double effectProd = varScore * relevanceScore;
		return effectProd;
	}
	
	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {	
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				
				if (obj instanceof VariantPool) {
					vars = (VariantPool)obj;
				}
				if (obj instanceof GeneList) {
					genes = (GeneList)obj;
				}
			}
		}
		
		if (genes == null)
			throw new IllegalArgumentException("No gene list provided to GeneEffectRanker");

		if (vars == null)
			throw new IllegalArgumentException("No variant provided to GeneEffectRanker");

		
	}
	
	
	class ScoreComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec var0, VariantRec var1) {
			Double score0 = var0.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			Double score1 = var1.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			
			if (score0 != null && score1 != null) {
				if (score0 == score1)
					return 0;
				return score0 < score1 ? 1 : -1;
			}
			
			return 0;
		}
		
	}
	

}
