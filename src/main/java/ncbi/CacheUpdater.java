package ncbi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Run this to update all local caches of various gene info 
 * 
 * @author brendan
 *
 */
public class CacheUpdater {
	
	
	public static void updateGeneSummaryCache() throws IOException {
		
		System.out.println("Building databases...");
		
		GeneInfoDB geneInfo= new GeneInfoDB(new File(GeneInfoDB.defaultDBPath));
		
		CachedGeneSummaryDB.setExpirationLength(30);
		CachedGeneSummaryDB geneSummaries = new CachedGeneSummaryDB();
		
		Collection<String> geneIDs = geneInfo.getAllGenes();
		int alreadyLocal = 0;
		
		
		List<String> newDownloads = new ArrayList<String>();
		for(String geneID : geneIDs) {
			if (geneSummaries.hasLocalSummary(geneID)) {
				alreadyLocal++;
			} else {
				newDownloads.add(geneID);
			}
		}
		
		System.out.println(alreadyLocal + " of " + geneIDs.size() + " genes already had local summaries");
		System.out.println("Attempting to download " + newDownloads.size() + " new summaries");
		
		for(String geneID : newDownloads) {
			
			geneSummaries.getSummaryForGene(geneID);
		}
		
		
	}
	
	public static void updatePubmedCache() throws IOException {
		GeneInfoDB geneInfo= new GeneInfoDB(new File(GeneInfoDB.defaultDBPath));
		
		CachedPubmedAbstractDB abstractDB = new CachedPubmedAbstractDB();
		GenePubMedDB pubmedDB = GenePubMedDB.getDB(new File(System.getProperty("user.home") + "/oldhome/resources/gene2pubmed_human"));
		
		Collection<Integer> geneIDs = geneInfo.getAllGeneIDs();
		
		
		List<Integer> pubmedIDs = new ArrayList<Integer>();
		int totalProcessed = 0;
		
		for(Integer geneID : geneIDs) {
			
			List<Integer> pubmedArticleIDs = pubmedDB.getPubMedIDsForGene(geneID);
			for(Integer articleID : pubmedArticleIDs) {
				if (articleID != null)
				pubmedIDs.add( articleID );
			}
			
			
			if (pubmedIDs.size() > 500) {
				int start = 0;
				int end = 500;
				System.out.println("Processing batch of " + pubmedIDs.size() + " records, total so far is: " + totalProcessed);
				while(start < pubmedIDs.size()) {
					//System.out.println("Fetching records from " + start + " to " + Math.min(pubmedIDs.size(), end) + " of " + pubmedIDs.size() + " total requested");
					//Fetch batches of 500 at a time...
					abstractDB.getRecordForIDs(pubmedIDs.subList(start, Math.min(pubmedIDs.size(), end)), false);
					start = end;
					end += 500;
				}
				
				totalProcessed += pubmedIDs.size();
				pubmedIDs.clear();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		
		updateGeneSummaryCache();
		updatePubmedCache();
		
	}
}
