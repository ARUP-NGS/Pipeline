package ncbi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import pipeline.Pipeline;

/**
 * A class that stores Gene Summaries that have been downloaded from NCBI (using the FetchGeneInfo class)
 * so that we're not constantly re-downloading the same summaries over and over.  This creates a hidden file
 * in the users home directory to store cached data, but should be able to gracefully handle cases where
 * the file gets deleted. In fact, deleting the file periodically may be good since it will force a re-download
 * of all genes. 
 *  Right now we write all information to the cache file every 10 cache misses, so if we're constantly missing
 * we write a lot, but if we we haven't downloaded any new info then we don't write much.
 *  
 * @author brendan
 *
 */
public class CachedGeneSummaryDB {
	
	private Map<String, GeneSummary> map = null;
	private String cacheFilePath = System.getProperty("user.home") + System.getProperty("file.separator") + ".geneinfocache"; 
	private FetchGeneInfo fetcher = new FetchGeneInfo(); //Fetches gene summaries from ncbi
    private GeneInfoDB geneInfo; //Stores symbol / refgene id information so we can look genes up by symbol
    
    public static int expirationDays = 90; // Force re-downloading of records older than a few months
    private int missesSinceLastWrite = 0; //Number of cache misses since last writeToFile
    
    public static final int maxToDownload = 19; //Don't ever expire more than this number of records in any single run 
    //public int newRecordsDownloaded = 0; //Number of new records so far downloaded
    
    private boolean prohibitNewDownloads = false; //If true, nothing new will be downloaded
    
	public CachedGeneSummaryDB() throws IOException {
	    this( GeneInfoDB.defaultDBPath );
	}

	public CachedGeneSummaryDB(String pathToGeneInfoFile) throws IOException {
	    geneInfo = GeneInfoDB.getDB();
	    if (geneInfo == null)
	    	geneInfo = new GeneInfoDB(new File(pathToGeneInfoFile));
	    File geneInfoFile = new File(pathToGeneInfoFile);
	    File parentDir = geneInfoFile.getParentFile();
	    cacheFilePath =  parentDir + System.getProperty("file.separator") + ".geneinfocache";
	    
	    Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
	    logger.info("Creating gene summary cache from : " + pathToGeneInfoFile + " and using cache in : " + cacheFilePath);
	    
		buildMapFromFile();
	}
	
	/**
	 * If true, nothing new will be downloaded and only local cache will be used
	 * @param prohibitNewDownloads
	 */
	public void setProhibitNewDownloads(boolean prohibitNewDownloads) {
		this.prohibitNewDownloads = prohibitNewDownloads;
	}

	/**
	 * Sets the expiration length of records in days. You should always follow 
	 * this up with a call to buildMapFromFile(), which will discard the current info and
	 * re-read the map, excluding all expired records 
	 * @param days
	 */
	public static void setExpirationLength(int days) {
		expirationDays = days;
	}
	
	/**
	 * Returns true if the local cache contains a summary for the given gene
	 * @param syn
	 * @return
	 */
	public boolean hasLocalSummary(String syn) {
		String id = geneInfo.symbolForSynonym(syn);
		return map.containsKey(id);
	}
	
	/**
	 * Obtain the full string containing ncbi's summary for the given gene symbol (i.e. 'RASA1'). 
	 * If the summary is already in the local cache and has not expired, just return it. If not, we 
	 * query ncbi and try to get it. 
	 * @param symbol
	 * @return
	 */
	public String getSummaryForGene(String syn) {
		if (syn==null || syn.length()<2 || syn.length() > 8) {
			return null;
		}
		
		String symbol = geneInfo.symbolForSynonym(syn);
		if (symbol == null) {
			//System.err.println("** No official symbol found for gene : " + syn + " : cannot find summary");
			return null;
		}
		
		GeneSummary summary = map.get(symbol);
		
		//Sweet, cache hit. Return the info right away
		if (summary != null) {
			return summary.summary;
		}
		
		//Dang, gotta look this one up... and put it in the cache
		String id = geneInfo.idForSymbol(symbol);
		if (id == null) {
			//System.out.println("Invalid id lookup for gene symbol : " + symbol);
			return null;
		}
		
		if (! prohibitNewDownloads) {
			try {

				System.out.println("Fetching summary from NCBI for gene : " + symbol);
				GeneRecord rec = fetcher.fetchInfoForGene(id);
				String summaryString = rec.getSummary();

				summary= new GeneSummary();
				summary.symbol = symbol;
				summary.date = "" + System.currentTimeMillis();
				summary.summary = summaryString;
				map.put(symbol, summary);
				missesSinceLastWrite++;
				if (missesSinceLastWrite > 20)
					writeMapToFile();
				//newRecordsDownloaded++;
				return summaryString;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;        
		
	}
	
	public void writeMapToFile() throws IOException {
		System.out.println("Writing cached gene summaries to " + cacheFilePath);
		if (map == null) //Map may not have been initialized
			return;
		
		File cache = new File(cacheFilePath);
		if (!cache.exists()) {
			try {
				cache.createNewFile();
			} catch (IOException e) {

				Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
			    logger.warning("Could not create gene summary cache file at path : " + cache.getAbsolutePath() + " reason: " + e.getMessage());
				//e.printStackTrace();
				return;
			}
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(cache));
		for(String symbol : map.keySet()) {
			GeneSummary sum = map.get(symbol);
			writer.write(symbol + "\t" + sum.date + "\t" + sum.summary + "\n");
		}
		System.out.println("Wrote " + map.keySet().size() + " summaries to cache");
		writer.close();
		missesSinceLastWrite = 0;
	}
	
	public void buildMapFromFile() throws IOException {
		map = new HashMap<String, GeneSummary>();
		System.out.println("Initializing cached gene summary db from path: " + cacheFilePath);
		File cache = new File(cacheFilePath);
		if (!cache.exists()) {
			try {
				cache.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		int expiredRecords = 0;
		BufferedReader reader = new BufferedReader(new FileReader(cache));
		String line = reader.readLine();
		while(line != null) {
			if (line.trim().length() == 0) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			String symbol = toks[0];
			String time = toks[1];
			String summary = toks[2];
			GeneSummary sum = new GeneSummary();
			sum.symbol = symbol;
			sum.date = time;
			sum.summary = summary;
			boolean expired = isExpired(sum); 
			if (!expired || expiredRecords > maxToDownload) {
				if (expired) {
					//System.out.println("Preserving expired record for gene: " + symbol + " since we've already missed so many");
				}
				
				if (map.containsKey(symbol)) {
					GeneSummary existingSummary = map.get(symbol);	
					//If record already exists somehow, be sure to use newest version found
					Long existingBirthDate = Long.parseLong(existingSummary.date);
					Long newBirthDate = Long.parseLong(sum.date);
					System.out.println("Found multiple gene summary records for symbol " + symbol + ", using newest one");
					if (newBirthDate > existingBirthDate) {
						map.put(symbol, sum);
					}
					else {
						//map already contains other one, no need to put it in
					}
					
				}
				else {
					map.put(symbol, sum);
				}
			}
			else {
				expiredRecords++;
			}
			line = reader.readLine();
		}
		System.out.println("Read in " + map.size() + " cached summaries from file " + cache.getAbsolutePath() + " " + expiredRecords + " of which were past expiration date");
	}
			
	/**
	 * Returns true if the date portion of a gene summary is sufficiently old. This ensures that we're always
	 * using relatively recent gene summary info. 
	 * @param sum
	 * @return
	 */
	private static boolean isExpired(GeneSummary sum) { 
		String recordBirthDate = sum.date;
		Long bd = Long.parseLong(recordBirthDate);
		long elapsedMS = System.currentTimeMillis() - bd;
		long elapsedSecs = elapsedMS / 1000;
		long elapsedMins = elapsedSecs / 60;
		long elapsedHours = elapsedMins / 60;
		long elapsedDays = elapsedHours / 24;
		if (elapsedDays >= expirationDays) {
			return true;
		}
			return false;
		
	}
	
	public void finalize() {
		try {
			writeMapToFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			super.finalize();
		} catch (Throwable e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	class GeneSummary {
		String symbol;
		String summary;
		String date;
		//Potentially more information here
	}
}
