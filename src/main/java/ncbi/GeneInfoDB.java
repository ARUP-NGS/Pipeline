package ncbi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores information about gene symbols, id's, NMs, and NPs. Primarily used to find the ref seq id# 
 * for a gene given its symbol but also  used as a gene synonym lookup service 
 * This generates its information from the the flat file  which can be 
 * obtained from :
 *  ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/
 * @author brendan
 *
 */
public class GeneInfoDB {

	private Map<String, GeneInfo> map = null;
	
	private static GeneInfoDB geneInfoDB = null;
	public static final String defaultDBPath = System.getProperty("user.home") + "/resources/Homo_sapiens.gene_info"; 
	
	public static GeneInfoDB getDB() {
		return geneInfoDB;
	}
	
	public GeneInfoDB(File dbFile) throws IOException {
		geneInfoDB = this;
		buildDB(dbFile);
	}
	
	/**
	 * Obtain the numerical NCBI id for the gene with the given symbol. The symbol must be the
	 * 'official' ncbi / refseq symbol. This thing doesn't look up synonyms
	 * @param symbol
	 * @return
	 */
	public String idForSymbol(String symbol) {
		GeneInfo rec = map.get(symbol.toUpperCase());
		if (rec == null)
			return null;
		else
			return rec.id;
	}
	
	/**
	 * Look up the (numeric) NCBI id for the given gene, with synonym lookup enabled 
	 * @param syn
	 * @return
	 */
	public String idForSymbolOrSynonym(String syn) {
		GeneInfo rec = findRecordForSynonym(syn);
		if (rec == null)
			return null;
		else 
			return rec.id;
	}
	
	/**
	 * Returns the set of all official symbols for all genes in the DB. 
	 * @return
	 */
	public Collection<String> getAllGenes() {
		return map.keySet();
	}
	
	
	/**
	 * Returns a list of the pubmed ID # for all genes in this DB 
	 * @return
	 */
	public Collection<Integer> getAllGeneIDs() {
		List<Integer> ids = new ArrayList<Integer>();
		for(String key : map.keySet()) {
			GeneInfo val = map.get(key);
			try {
				Integer id = Integer.parseInt( val.id );
				if (id != null)
					ids.add( id );
			}
			catch (NumberFormatException nfe) {
				System.err.println("Could not parse id for gene with symbol :" + key + " id is: " + val.id);
			}
		}
		return ids;
	}
	
	/**
	 * Look up the official symbol for the given gene
	 * @param syn
	 * @return
	 */
	public String symbolForSynonym(String syn) {
		GeneInfo rec = findRecordForSynonym(syn);
		if (rec == null)
			return null;
		else 
			return rec.symbol;
	}
	
	
	/**
	 * Returns the gene record for the gene whose official symbol OR synonym
	 * is the given symbol
	 * @param syn
	 * @return
	 */
	public GeneInfo findRecordForSynonym(String syn) {
		syn = syn.trim().toUpperCase();
		GeneInfo rec = map.get(syn);
		if (rec != null)
			return rec;
		else {
			//Conduct laborious search for synonyms
			for(String officialSymbol : map.keySet()) {
				rec = map.get(officialSymbol);
				for(int i=0; i<rec.synonyms.length; i++) {
					if (rec.synonyms[i].equals(syn))
						return rec;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Read the file and push all information to the map we use to store data
	 * @param file
	 * @throws IOException
	 */
	private void buildDB(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		
		map = new HashMap<String, GeneInfo>();
		
		while(line != null) {
			if (line.trim().length() ==0 || line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			String id = toks[1];
			String symbol = toks[2];
			String[] synonyms = toks[4].split("\\|");
			
			GeneInfo rec = new GeneInfo();

			rec = new GeneInfo();
			map.put(symbol, rec);
			rec.synonyms = synonyms;
			rec.id = id;
			rec.symbol = symbol;
			line = reader.readLine();
		}
		System.out.println("Built gene info db with " + map.size() + " elements");
		reader.close();
	}
	
	public class GeneInfo {
		public String symbol;
		public String id;
		public String[] synonyms;		
	}
	
}
