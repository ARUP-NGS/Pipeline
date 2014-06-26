package operator.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for info from DBNSFP_GENE database
 * @author brendan
 *
 */
public class DBNSFPGene {
	
	private static DBNSFPGene db = null;
	private Map<String, GeneInfo> map = null;

	public static DBNSFPGene getDB() {	
		return db;
	}
	
	public static DBNSFPGene getDB(File sourceFile) throws IOException {
		if (db == null) {
			db = new DBNSFPGene(sourceFile);
		}
		
		return db;
	}

	private DBNSFPGene(File sourceFile) throws IOException {
		readFile(sourceFile);
	}
	
	/**
	 * Obtain a geneInfo object for the gene with the given name
	 * @param geneName
	 * @return
	 */
	public GeneInfo getInfoForGene(String geneName) {
		return map.get(geneName);
	}
	

	private void readFile(File sourceFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		
		map = new HashMap<String, GeneInfo>();
		
		String line = reader.readLine();
		line = reader.readLine();
		while(line != null) {
			String[] toks = line.split("\t");
			GeneInfo info = new GeneInfo();
			info.geneName = toks[0];
			info.mimDisease = toks[16];
			info.diseaseDesc = toks[15];
			info.functionDesc = toks[14];
			info.expression = toks[19] + ";" + toks[20];
			map.put(info.geneName, info);
			line = reader.readLine();
		}
	
		System.err.println("Initialized dbNSFP2.0-gene database with " + map.size() + " elements");
		reader.close();
	}
	
	/**
	 * Return all gene names for which there's an entry
	 * @return
	 */
	public Collection<String> getGeneNames() {
		return map.keySet();
	}
	
	public class GeneInfo {
		public String geneName = null;
		public String mimDisease =  null; //Column 16
		public String diseaseDesc = null; //Column 15
		public String functionDesc = null; //Column 14
		public String expression = null; //Columns 19 and 20
	}
	
}
