package ncbi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import pipeline.Pipeline;

/**
 * Analagous to CachedGeneSUmmaryDB, this stores pubmed abstract information locally so we're not always
 * re-downloading it from ncbi, but grabs and stores new IDs as needed. Thus, the first lookup will be slow,
 * but all subsequent lookups should be very fast
 * @author brendan
 *
 */
public class CachedPubmedAbstractDB {

	private Map<Integer, PubMedRecord> map = null;
	private Set<Integer> brokenIDs = new HashSet<Integer>(); //Stores IDs for which retrieving data 
	private String cacheFilePath = System.getProperty("user.home") + System.getProperty("file.separator") + ".pubmedcache"; 
	private PubMedFetcher fetcher = new PubMedFetcher(); //Fetches gene summaries from ncbi
    private GenePubMedDB genePubMed; //Stores gene-pubmed id mapping
    private int missesSinceLastWrite = 0; //Number of cache misses since last writeToFile
    
    public static final String defaultDBPath = System.getProperty("user.home") + "/resources/gene2pubmed_human";
    
    private boolean prohibitNewDownloads = false; //Prevent new downloads if true
    
    private static CachedPubmedAbstractDB db = null;

    public static CachedPubmedAbstractDB getDB(String pathToPubmedDB) throws IOException {
    	if (db == null) {
    		db = new CachedPubmedAbstractDB(pathToPubmedDB);
    	}
    	return db;
    }
    
    public static CachedPubmedAbstractDB getDB() throws IOException {
    	if (db == null) {
    		db = new CachedPubmedAbstractDB(defaultDBPath);
    	}
    	return db;
    }
    
    public CachedPubmedAbstractDB(String pathToGene2PubmedFile) throws IOException {
    	Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
    	logger.info("Creating NEW Pubmed abstract db");
    	db = this;
	    genePubMed = new GenePubMedDB(new File(pathToGene2PubmedFile));
	    File pubmedFile = new File(pathToGene2PubmedFile);
	    File baseDir = pubmedFile.getParentFile(); 
	    cacheFilePath = baseDir + System.getProperty("file.separator") + ".pubmedcache";
	    
	    logger.info("Creating pubmed cache from : " + pathToGene2PubmedFile + " and using cache in : " + cacheFilePath);
	    
		buildMapFromFile();
		logger.info("..done initializing pubmed cache");
	}
    
	public CachedPubmedAbstractDB() throws IOException {
		this( defaultDBPath );
	}

	/**
	 * Get the number of entries in the map
	 * @return
	 */
	public int getMapSize() {
		if (map == null)
			return 0;
		else 
			return map.size();
	}
	/**
	 * Obtain a list of pubmed records for the given ids. If you're downloading lots of records
	 * this is a lot more efficient than getting them one at a time. 
	 * @param pubmedIDs
	 * @return 
	 */
	public synchronized List<PubMedRecord> getRecordForIDs(List<Integer> pubmedIDs) {
		return getRecordForIDs(pubmedIDs, false);
	}
	
	/**
	 * Whether or not we're set to download new records if there's not one in the local cache
	 * @return
	 */
	public boolean isProhibitNewDownloads() {
		return prohibitNewDownloads;
	}

	/**
	 * Turn on/off downloading of new records if none are found in local cache
	 * @param prohibitNewDownloads
	 */
	public void setProhibitNewDownloads(boolean prohibitNewDownloads) {
		this.prohibitNewDownloads = prohibitNewDownloads;
	}

	/**
	 * Obtain a list of pubmed records for the given ids. If you're downloading lots of records
	 * this is a lot more efficient than getting them one at a time. 
	 * @param pubmedIDs
	 * @param disableCacheWrites If true we will not write newly downloaded records to local cache
	 * @return 
	 */
	public synchronized List<PubMedRecord> getRecordForIDs(List<Integer> pubmedIDs, boolean disableCacheWrites) {
		
		//Make a list of all the ids we need to grab
		List<Integer> idsToGrab = new ArrayList<Integer>();
		for(Integer recID : pubmedIDs) {
			if (! map.containsKey(recID) && (!brokenIDs.contains(recID))) {
				idsToGrab.add(recID);
			}
		}
		
		//Grab the list of ids in an efficient way
		if ((! prohibitNewDownloads) && idsToGrab.size() > 0) {
			int recordsDownloaded = forceFetchIDs(idsToGrab);
			missesSinceLastWrite += recordsDownloaded;
		}

		
		if (missesSinceLastWrite > 200 && (!disableCacheWrites)) {
			try {
				writeMapToFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		List<PubMedRecord> records = new ArrayList<PubMedRecord>(pubmedIDs.size());
		for(Integer id : pubmedIDs) {
			PubMedRecord rec = map.get(id);
			if (rec != null) {
				records.add(rec);
			}
		}
		return records;
	}
	
	/**
	 * Forces re-downloading of all ids in the list. These are then added directly to the map
	 * @param pubmedIDs
	 */
	private int forceFetchIDs(List<Integer> pubmedIDs) {
		int recordsObtained = 0;

		List<PubMedRecord> records;
		try {
			records = fetcher.getPubMedRecordForIDs(pubmedIDs);
			for(PubMedRecord rec : records) {
				if (rec != null) {
					map.put(rec.pubMedID, rec);
					recordsObtained++;
				}
			}
			
			//We need to determine which, if any, IDs resulted in errors, so see if there are any ids in 
			//the input list which are not associated with an entry in the map. These are all 'broken'
			for(Integer id : pubmedIDs) {
				if (! map.containsKey(id))
					brokenIDs.add(id);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return recordsObtained;
	}
	
	/**
	 * Obtain the full string containing the pubmed summary information for the given pubmed id 
	 * If the summary is already in the local cache and has not expired, just return it. If not, we 
	 * query ncbi and try to get it. 
	 * @param symbol
	 * @return
	 */
	public synchronized PubMedRecord getRecordForID(Integer pubmedID) {
		PubMedRecord rec = map.get(pubmedID);
		
		//Sweet, cache hit. Return the info right away
		if (rec != null) {
			return rec;
		}

		if (! prohibitNewDownloads) {
			try {
				rec = fetcher.getPubMedRecordForID(pubmedID);

				//Sanity check
				if (!(rec.pubMedID == pubmedID)) {
					throw new IllegalArgumentException("Obtained pubmedID does not match requested ID!");
				}

				map.put(pubmedID, rec);
				missesSinceLastWrite++;
				if (missesSinceLastWrite > 100)
					writeMapToFile();
				return rec;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;        
		
	}
	
	public synchronized void writeMapToFile() throws IOException {
		//System.out.println("Writing cached gene summaries to " + cacheFilePath);
		if (map == null) //Map may not have been initialized
			return;
		if (map.size() < 500) {
			return; 
		}
		
		File cache = new File(cacheFilePath);
		if (!cache.exists()) {
			try {
				cache.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(cache));
		for(Integer id : map.keySet()) {
			PubMedRecord sum = map.get(id);
			writer.write(sum.toString() + "\n");
		}
		System.out.println("Wrote " + map.keySet().size() + " pubmed abstracts to cache");
		writer.close();
		missesSinceLastWrite = 0;
	}
	
	public void buildMapFromFile() throws IOException {
		map = new HashMap<Integer, PubMedRecord>();
		//System.out.println("Initializing cached pubmed db");
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
			Integer id = Integer.parseInt(toks[0]);
			Integer year = Integer.parseInt(toks[1]);
			String title = toks[2];
			String citation = toks[3];
			String abs = toks[4];
			
			PubMedRecord rec = new PubMedRecord();
			rec.pubMedID = id;
			rec.yearCreated = year;
			rec.title = title;
			rec.citation = citation;
			rec.abs = abs;
			map.put(id, rec);
			line = reader.readLine();
		}
		System.out.println("Read in " + map.size() + " cached pubmed abstracts from cache");
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
	

	
}
