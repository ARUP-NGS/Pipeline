package operator.gene;

import gene.Gene;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.variant.DBNSFPAnnotator;
import operator.variant.DBNSFPGene;
import operator.variant.DBNSFPGene.GeneInfo;
import pipeline.Pipeline;

/**
 * Computes ranking values based on expression data from dbNSFP
 * @author brendan
 *
 */
public class ExpressionRanker extends AbstractGeneRelevanceRanker {

	private DBNSFPGene geneDB = null;
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		if (geneDB == null) {
			String pathToDBNSFP = this.getPipelineProperty(DBNSFPAnnotator.DBNSFP_PATH);
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene reader using directory : " + pathToDBNSFP);
			try {
				geneDB = DBNSFPGene.getDB(new File(pathToDBNSFP + "/dbNSFP2.0b4_gene"));
			} catch (IOException e) {
				throw new OperationFailedException("Could not initialize dbNSFP gene file", this);
			}
		}
		
		if (rankingMap == null) {
			try {
				buildRankingMap();
			} catch (IOException e) {
				throw new OperationFailedException("Could not read ranking map", this);
			}
		}
		
		examined++;
		GeneInfo info = geneDB.getInfoForGene(g.getName());
		double score = 0.0;
		if (info != null) {
			ScoreResult result = computeScore(info);
			if (result.score > 0) {
				scored++;
				g.addAnnotation(Gene.EXPRESSION_HITS, result.hits);
				System.out.println("Found score " + result.score + " with hits: " + result.hits + " for gene : " + g.getName());
			}
			score = result.score;
		}
		g.addProperty(Gene.EXPRESSION_SCORE, score);
	}
	
	public ScoreResult computeScore(GeneInfo info) {
		double score = 0;
		String hits = "";
		for(String term : rankingMap.keySet()) {
			if (info.expression != null && info.expression.contains(term.toLowerCase())) {
				score += rankingMap.get(term);
				if (hits.length()==0)
					hits = term;
				else 
					hits = hits + ", " + term;
			}
			
		}
		ScoreResult result = new ScoreResult();
		result.score = score;
		result.hits = hits;
		return result;
	}
	
	
	class ScoreResult {
		double score = 0.0;
		String hits = null;
	}


	@Override
	public String getScoreKey() {
		return Gene.EXPRESSION_SCORE;
	}
}
