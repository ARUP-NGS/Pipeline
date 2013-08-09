package operator.gene;

import gene.Gene;

import java.io.IOException;

import operator.OperationFailedException;

/**
 * Uses OMIM phenotype information (as provided by OMIMDB and the OMIMAnnotator) to
 * produce a gene relevance subscore, just like the NCBISummaryRanker, etc.  
 * @author brendan
 *
 */
public class OMIMPhenoRanker extends AbstractGeneRelevanceRanker {

	@Override
	public void annotateGene(Gene g) throws OperationFailedException {		
		if (rankingMap == null) {
			try {
				buildRankingMap();
			} catch (IOException e) {
				throw new OperationFailedException("Could not read ranking map", this);
			}
		}
		
		examined++;
		String phenotypes = g.getAnnotation(Gene.OMIM_PHENOTYPES);
		double score = 0.0;
		if (phenotypes != null) {
			score = computeScore(g, phenotypes);
			if (score > 0)
				scored++;
		}
		
		g.addProperty(Gene.OMIM_PHENOTYPE_SCORE, score);
		
	}

	
	public double computeScore(Gene g, String phenoStr) {
		double score = 0;
		phenoStr = phenoStr.toLowerCase();
		for(String term : rankingMap.keySet()) {
			if (phenoStr.contains(term)) {
				score += rankingMap.get(term);
				g.appendAnnotation(Gene.OMIM_PHENOTYPE_HIT, term);
				
			}
		}
		return score;
	}


	@Override
	public String getScoreKey() {
		return Gene.OMIM_PHENOTYPE_SCORE;
	}

}
