package operator.gene;

import gene.Gene;

import java.io.IOException;
import java.util.logging.Logger;

import ncbi.CachedGeneSummaryDB;
import pipeline.Pipeline;

/**
 * Adds the ncbi gene summary as an annotation to the gene
 * @author brendan
 *
 */
public class NCBISummaryAnnotator extends AbstractGeneAnnotator {

	public static final String GENE_INFO_PATH = "gene.info.path";
	public static final String NO_DOWNLOADS = "no.downloads";
	CachedGeneSummaryDB summaryDB = null;
	
	@Override
	public void annotateGene(Gene g) {
		if (summaryDB == null) {
			
			try {
				initializeDB();
			} catch (IOException e) {
				e.printStackTrace();
				Logger.getLogger(Pipeline.primaryLoggerName).severe("Could not create gene summary db : " + e.getMessage());
			}
			
		}
		
		String summary = summaryDB.getSummaryForGene( g.getName() );	
		g.addAnnotation(Gene.SUMMARY, summary);
		
	}

	private void initializeDB() throws IOException {
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
	}
	

}
