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
 * Computes a ranking score for a gene by examining the information
 * from the DBNSFP_gene database
 * @author brendan
 * chrisk added 2.9 gene database and throw exception if 3.1a is used
 *
 */
public class DBNSFPGeneRanker extends AbstractGeneRelevanceRanker {

	private DBNSFPGene geneDB = null;
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		
		
		if (geneDB == null) {
			String pathToDBNSFPGene = this.getPipelineProperty(DBNSFPGeneAnnotator.DBNSFPGENE_PATH);
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene reader using directory : " + pathToDBNSFPGene);
			DBNSFPGene version = geneDB.getDB();
			try {
				if (version.equals("2.0")){
				geneDB = DBNSFPGene.getDB(new File(pathToDBNSFPGene + "/dbNSFP2.0b4_gene"));
				}
				else if (version.equals("2.9")){
					geneDB = DBNSFPGene.getDB(new File(pathToDBNSFPGene + "/dbNSFP2.9_gene"));
				}
				else if (version.equals("3.1a")){
					throw new OperationFailedException("v3.1a not implemented in pipeline yet...", this);
				}
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
			score = computeScore(info);
			if (score > 0)
				scored++;
		}
		g.addProperty(Gene.DBNSFPGENE_SCORE, score);
	}

	
	public double computeScore(GeneInfo info) {
		double score = 0;
		
		for(String term : rankingMap.keySet()) {
			if (info.diseaseDesc != null && info.diseaseDesc.toLowerCase().contains(term)) {
				score += 2.0*rankingMap.get(term);
			}
			if (info.functionDesc != null && info.functionDesc.toLowerCase().contains(term)) {
				score += 2.0*rankingMap.get(term);
			}
		}
		return score;
	}


	@Override
	public String getScoreKey() {
		return Gene.DBNSFPGENE_SCORE;
	}
}
