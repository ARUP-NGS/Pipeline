package operator.gene;

import gene.Gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import ncbi.CachedPubmedAbstractDB;
import ncbi.GeneInfoDB;
import ncbi.GenePubMedDB;
import ncbi.PubMedRecord;
import operator.OperationFailedException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.TextBuffer;

/**
 * Annotates variants with the PUBMED_SCORE property, which is computed by looking at pubmed abstracts
 * stored in a CachedPubmedAbstractDB 
 * 
 * @author brendan
 *
 */
public class PubmedRanker extends AbstractGeneAnnotator {

	//String constants for some XML properties we may use
	public static final String DISABLE_CACHE_WRITES = "disable.cache.writes";
	public static final String GENE_INFO_PATH = "gene.info.path";
	public static final String PUBMED_PATH = "pubmed.path";
	public static final String NO_DOWNLOADS = "no.downloads";

	CachedPubmedAbstractDB abstractDB = null; //DB for abstracts that we query
	GenePubMedDB geneToPubmed = null; //Look up pub med id's associated with a certain gene
	GeneInfoDB geneInfo = null; //Look up gene IDs for gene symbols
	TextBuffer termsFile = null; //Stores key terms we use to score pub med records (abstracts)
	Map<String, Integer> rankingMap = null;
	private boolean disableCacheWrites = false; //Disable writing of new variants to cache, useful if we have multiple instances running
	private int examined = 0;
	private int scored = 0;
	
	
	/**
	 * If true, we write some progress indicators to system.out
	 * @return
	 */
	protected boolean displayProgress() {
		return true;
	}
	
	public void performOperation() throws OperationFailedException {
		
		String disableStr = properties.get(DISABLE_CACHE_WRITES);
		if (disableStr != null) {
			Boolean disable = Boolean.parseBoolean(disableStr);
			disableCacheWrites = disable;
		}
			
		try {
			if (abstractDB == null) {
				String pathToPubmedDB = System.getProperty("user.home") + "/resources/gene2pubmed_human";
				String pubmedAttr = this.getAttribute(PUBMED_PATH);
				if (pubmedAttr != null) {
					pathToPubmedDB = pubmedAttr;
				}
				
				abstractDB = CachedPubmedAbstractDB.getDB(pathToPubmedDB);
				

				String dlAttr = this.getAttribute(NO_DOWNLOADS);
				if (dlAttr != null) {
					Logger.getLogger(Pipeline.primaryLoggerName).info("Abstract ranker is setting prohibit downloads to : " + dlAttr);
					Boolean prohibitDLs = Boolean.parseBoolean(dlAttr);
					abstractDB.setProhibitNewDownloads(prohibitDLs);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("IO error reading cached abstract db : " + e.getMessage());
		}

		//Actually annotate the genes
		super.performOperation();
		
		//Write map back to file when were done
		if (abstractDB != null && abstractDB.getMapSize() > 500) {
			try {
				abstractDB.writeMapToFile();
			} catch (IOException e) {
				//Probably not a big deal, some abstracts won't get cached
				e.printStackTrace();
			}
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info(this.getObjectLabel() + " found " + scored + " hits in " + examined + " total genes");

	}
	
	@Override
	public void annotateGene(Gene gene) {
		if (rankingMap == null) {
			try {
				buildRankingMap();
			} catch (IOException e) {
				throw new IllegalStateException("IO error reading pubmed terms file : " + e.getMessage());

			}
		}
		
		if (geneInfo == null) {
			try {
				String geneInfoPath = GeneInfoDB.defaultDBPath;
				String geneInfoAttr = this.getAttribute(GENE_INFO_PATH);
				if (geneInfoAttr != null) {
					geneInfoPath = geneInfoAttr;
				}
				
				geneInfo = GeneInfoDB.getDB();
				if (geneInfo == null)
					geneInfo = new GeneInfoDB(new File(geneInfoPath));
			} catch (IOException e) {
				throw new IllegalStateException("Error opening gene info file : " + e.getMessage());
			}
		}
		
	
		String geneName = gene.getName();
		if (geneName == null || geneName.startsWith("HLA-") || geneName.startsWith("MUC")) {
			return;
		}
		
		if (geneToPubmed == null) {
			try {
				String pubmedPath = System.getProperty("user.home") + "/resources/gene2pubmed_human";
				String pubmedAttr = this.getAttribute(PUBMED_PATH);
				if (pubmedAttr != null) {
					pubmedPath = pubmedAttr;
				}
				geneToPubmed = GenePubMedDB.getDB(new File(pubmedPath));
			} catch (IOException e) {
				throw new IllegalStateException("Error opening gene2pubmed file : " + e.getMessage());
			}
		}
		
		
		
		String idStr = geneInfo.idForSymbolOrSynonym(geneName);
		if (idStr == null) {
			if (geneName.length() < 8) {
				if (! (geneName.startsWith("C") && geneName.contains("orf")))
					System.err.println("Could not find gene id for symbol: " + geneName);
			}
			return;
		}
		
		Integer geneID = Integer.parseInt( idStr );
		
		List<Integer> pubmedIDs = geneToPubmed.getPubMedIDsForGene(geneID);
		if (pubmedIDs == null) {
			//System.out.println("Found 0 ids for gene : " + geneName);
			return;
		}
		
		//Grab in batches of 500 if there are more than 500

		List<PubMedRecord> records = new ArrayList<PubMedRecord>(Math.min(pubmedIDs.size(), 512));
		int start = 0;
		int end = 500;
		while(start < pubmedIDs.size()) {
			//System.out.println("Fetching records from " + start + " to " + Math.min(pubmedIDs.size(), end) + " of " + pubmedIDs.size() + " total requested");
			List<PubMedRecord> subrec = abstractDB.getRecordForIDs(pubmedIDs.subList(start, Math.min(pubmedIDs.size(), end)), disableCacheWrites);
			records.addAll(subrec);
			start = end;
			end += 500;
		}
		

		this.getPipelineOwner().fireMessage("Examining " + records.size() + " abstracts for gene : " + geneName);
		
		//System.out.println("Found " + records.size() + " records for " + pubmedIDs.size() + " ids for for gene : " + geneName);
		//We take the *maximum* score found among all abstracts
		
		int recordListSize = 10;
		List<ScoredRecord> scoredRecs = new ArrayList<ScoredRecord>(recordListSize);
		
		//Double maxScore = 0.0;
		//String maxHit = "-";
		for(PubMedRecord rec : records) {
			if (rec != null) {
				Double abstractScore = computeScore( rec );
			
				if (abstractScore > 0) {
					ScoredRecord sRec = new ScoredRecord();
					sRec.score = abstractScore;
					sRec.rec = rec;
					scoredRecs.add(sRec);
					Collections.sort(scoredRecs, new ScoreComparator());
					while (scoredRecs.size() > recordListSize) {
						scoredRecs.remove(scoredRecs.size()-1);
					}
				}
				
			}
		}
		 
		examined++;
		Double finalScore = 0.0;
		if (scoredRecs.size() > 0) {
			for(int i=0; i<scoredRecs.size(); i++) {
				double weight = Math.exp(-0.25 * i);
				double rawScore = scoredRecs.get(i).score;
				double modScore = weight*rawScore;
				finalScore += modScore;
			}
		}
		
		if (finalScore > 0)
			scored++;
		gene.addProperty(Gene.PUBMED_SCORE, finalScore);
		if (scoredRecs.size() > 0) {
			PubMedRecord rec = scoredRecs.get(0).rec;
			String maxHit = rec.getTitle() + "," + rec.getCitation();
			gene.addAnnotation(Gene.PUBMED_HIT, maxHit);
		}
	}

	/**
	 * Compute a score 
	 * @param rec
	 * @return
	 */
	private Double computeScore(PubMedRecord rec) {
		String title = rec.getTitle();
		String abs = rec.getAbstract();
		
		if (title != null)
			title = title.toLowerCase();
		if (abs != null)
			abs = abs.toLowerCase();
		
		double score = 0;
		for(String term : rankingMap.keySet()) {
			if (title != null && title.contains(term)) {
				score += 2.0*rankingMap.get(term);
			}
			if (abs != null && abs.contains(term)) {
				score += rankingMap.get(term);
			}
		}
		
		//Discount older papers
		Double mod = 1.0;
		Integer year = rec.getYear();
		if (year != null) {
			int age = Calendar.getInstance().get(Calendar.YEAR) - year;
			if (age > 8)
				mod = 0.75;
			if (age > 12) {
				mod = 0.5;
			}
			
		}
		score *= mod;
		
		return score;
	}


	private void buildRankingMap() throws IOException {
		rankingMap = new HashMap<String, Integer>();
		BufferedReader reader = new BufferedReader(new FileReader(termsFile.getAbsolutePath()));
		String line = reader.readLine();
		while(line != null) {
			if (line.trim().length()==0 || line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			if (toks.length != 2) {
				System.err.println("Warning : could not parse line for pubmed key terms : " + line);
				line = reader.readLine();
				continue;
			}
			Integer score = Integer.parseInt(toks[1].trim());
			if (toks[0].trim().length()<2) {
				System.err.println("Warning : could not parse line for pubmed key terms : " + line);
				line = reader.readLine();
				continue;
			}
			//System.err.println("Ranking map adding term: " + toks[0].trim().toLowerCase() + " score:" + score);
			rankingMap.put(toks[0].trim().toLowerCase(), score);
			
			line = reader.readLine();
		}
	}
	
	
	public void initialize(NodeList children) {
		super.initialize(children);

		for(int i=0; i<children.getLength(); i++) {	
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof TextBuffer) {
					termsFile = (TextBuffer)obj;
				}
			}
		}
	}
	
	
	class ScoredRecord {
		PubMedRecord rec;
		double score;
	}
	
	class ScoreComparator implements Comparator<ScoredRecord> {

		@Override
		public int compare(ScoredRecord arg0, ScoredRecord arg1) {
			if (arg0.score == arg1.score)
				return 0;
			if (arg0.score < arg1.score)
				return 1;
			else 
				return -1;
		}
		
	}
}
