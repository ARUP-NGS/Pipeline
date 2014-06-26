package operator.gene;

import gene.Gene;

import java.io.IOException;
import java.util.logging.Logger;

import ncbi.CachedGeneSummaryDB;
import operator.OperationFailedException;
import pipeline.Pipeline;

/**
 * An annotator that computes a score for variants based on how many hits a particular term has
 * in the gene summary associated with the gene that this variant is in. No score is computed
 * for variants that are not in a gene. Furthermore, since gene summaries are only available
 * for some subset of genes, many genes will not be scored with the algorithm. 
 * @author brendan
 *
 */
public class GeneSummaryRanker extends AbstractGeneRelevanceRanker  {

	public static final String GENE_INFO_PATH = "gene.info.path";
	public static final String NO_DOWNLOADS = "no.downloads";
	
	protected CachedGeneSummaryDB summaryDB = null;
	
	public void performOperation() throws OperationFailedException {
		super.performOperation();
		 
		//Force a final write to the cache when we're done. 
		if (summaryDB != null) {
			try {
				summaryDB.writeMapToFile();
			} catch (IOException e) {
				//Not a huge deal really, we just wont have cached gene summaries for next time. 
				e.printStackTrace();
			}
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info(this.getObjectLabel() + " found " + scored + " hits in " + examined + " total genes");
	}
	
	/**
	 * If true, we write some progress indicators to system.out
	 * @return
	 */
	protected boolean displayProgress() {
		return true;
	}
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		if (summaryDB == null) {
			initializeDB();
		}
		
		examined++;
    	String summary = summaryDB.getSummaryForGene( g.getName() );
    	if (summary == null)
    		return;
    	
    	double score = scoreSummary(summary.toLowerCase());
    	if (score > 0)
    		scored++;
    	g.addProperty(Gene.SUMMARY_SCORE, score);
	}
	
	/**
	 * Computes a score for the given gene summary by seeing how many key terms, defined in a text
	 * file, are present in the summary
	 * @param summary
	 * @return
	 */
	public double scoreSummary(String summary) {
		double score = 0;
		for(String term : rankingMap.keySet()) {
			if (summary.contains(term)) {
				score += rankingMap.get(term);
			}
		}
		return score;
	}

	
	
	
	private void initializeDB() {
		try {
			buildRankingMap();
			
			String pathToGeneInfo = this.getAttribute(GENE_INFO_PATH);
			if (pathToGeneInfo != null) {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Gene summary is using custom location for cache: " + pathToGeneInfo);
				summaryDB = new CachedGeneSummaryDB(pathToGeneInfo);
			}
			else {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Gene summary is using default location for local cache");
				summaryDB = new CachedGeneSummaryDB();
			}
			
			String dlAttr = this.getAttribute(NO_DOWNLOADS);
			if (dlAttr != null) {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Gene summary is setting prohibit downloads to : " + dlAttr);
				Boolean prohibitDLs = Boolean.parseBoolean(dlAttr);
				summaryDB.setProhibitNewDownloads(prohibitDLs);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String getScoreKey() {
		return Gene.SUMMARY_SCORE;
	}
	

	
	

}
