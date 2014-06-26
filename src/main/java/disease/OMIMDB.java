package disease;

import gene.Gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import json.JSONException;
import json.JSONObject;
import json.JSONTokener;

/**
 * Object representation of all OMIM info
 * @author brendan
 *
 */
public class OMIMDB {
	
	private File rootDir = null;
	
	//Map from gene name to list of OMIM entries
	private Map<String, List<OMIMEntry>> geneMap = new HashMap<String, List<OMIMEntry>>();
	
	private Map<String, DiseaseInfo> diseaseMap = new HashMap<String, DiseaseInfo>();
	/**
	 * Create an OMIM DB based on info in the directory provided
	 * @param omimDir
	 * @throws IOException 
	 */
	public OMIMDB(File omimDir) throws IOException {
		if (!omimDir.exists() || (! omimDir.isDirectory())) {
			throw new IllegalArgumentException("Can't read OMIM directory at : " + omimDir.getAbsolutePath());
		}
		
		rootDir = omimDir;
		readMorbidMap();
		
		try {
			readDiseaseInfo();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Obtain a list of entries associated with the given gene. This may return
	 * an empty list or null if there are no entries. 
	 * @param g
	 * @return
	 */
	public List<OMIMEntry> getEntriesForGene(Gene g) {
		String name = g.getName();
		return geneMap.get(name);
	}
	
	/**
	 * Parse the omim.json file to read disease and phenotype information
	 * @throws IOException
	 * @throws JSONException
	 */
	private void readDiseaseInfo() throws IOException, JSONException {
		String pathToOMIMJSON = rootDir.getAbsolutePath() + "/omim.json";
		File omimJSON = new File(pathToOMIMJSON);
		if (! omimJSON.exists()) {
			throw new IllegalArgumentException("OMIM json file at path " + pathToOMIMJSON + " does not exist");
		}
		
		BufferedReader reader = new BufferedReader(new FileReader(omimJSON));
		JSONTokener jsonReader = new JSONTokener(reader);
		
		JSONObject omim = new JSONObject(jsonReader);
		
		Iterator it = omim.keys();
		while(it.hasNext()) {
			Object key = it.next();
			String id = key.toString();
			JSONObject jobj = omim.getJSONObject(id);
			DiseaseInfo disInfo = new DiseaseInfo(id, jobj);
			diseaseMap.put(id, disInfo);
		}
		
		System.out.println("Found " + diseaseMap.size() + " disease descriptions");
		reader.close();
	}
	
	private void readMorbidMap() throws IOException {
		
		String pathToMorbidMap = rootDir.getAbsolutePath() + "/morbidmap";
		File morbidmap = new File(pathToMorbidMap);
		if (! morbidmap.exists()) {
			throw new IllegalArgumentException("OMIM 'morbidmap' file at path " + pathToMorbidMap + " does not exist");
		}
		
		int entryCount = 0; //Count total entries
		
		BufferedReader reader = new BufferedReader(new FileReader(morbidmap));
		String line = reader.readLine();
		while(line != null) {
			String[] toks = line.split("\\|");
			String diseaseStr = toks[0];
			String genesStr = toks[1];
			String geneOMIMId = toks[2];
			
			//Parse disease name,  
			int lastCommaIndex = diseaseStr.lastIndexOf(',');
			if (lastCommaIndex == -1) {
				//In cases where there's no gene associated the phenotype id will not be in the disease name,
				//so skip these
				line = reader.readLine();
				continue;
			}
			String disName = diseaseStr.substring(0, lastCommaIndex);
			//Try to parse a six-digit number from string
			String disID = parseID(diseaseStr.substring(lastCommaIndex));
			if (disID != null && disID.length() == 6) {
				String gene;
				if (genesStr.contains(",")) {
					gene = genesStr.substring(0, genesStr.indexOf(',')).trim();
				}
				else {
					gene = genesStr.trim();
				}


				OMIMEntry entry = new OMIMEntry(disName, disID, gene, geneOMIMId);
				List<OMIMEntry> entries = geneMap.get(gene);
				if (entries == null) {
					entries = new ArrayList<OMIMEntry>(4);
					geneMap.put(gene, entries);
				}
				entries.add(entry);
				entryCount++;
			}
			else {
				System.out.println("Skipping entry " + diseaseStr + ", could not parse phenotype id");
			}
			line = reader.readLine();
		}
		reader.close();
		
		System.out.println("Found " + entryCount + " total disease entries with " + geneMap.size() + " genes");
		
	}

	
	/**
	 * Parse and return the first six-digit number encountered
	 * @param substring
	 * @return
	 */
	private String parseID(String str) {
		Pattern pat = Pattern.compile("\\d{6}");
		Matcher matcher = pat.matcher(str);
		boolean found = matcher.find();
		if (found) {
			String match = matcher.group();
			return match;
		}
		else 
			return null;
	}

	/**
	 * Collection of all genes with entries
	 * @return
	 */
	public Collection<String> allGenes() {
		return geneMap.keySet();
	}
	
	/**
	 * List of entries associated with given gene
	 * @param gene
	 * @return
	 */
	public List<OMIMEntry> getEntriesForGene(String gene) {
		return geneMap.get(gene);
	}
	
	/**
	 * Convenience method, returns all disease ids associated with the gene
	 * @param gene
	 * @return
	 */
	public List<String> getDiseaseIDsForGene(String gene) {
		List<String> disIds = new ArrayList<String>();
		List<OMIMEntry> entries = getEntriesForGene(gene);
		if (entries != null) {
			for(OMIMEntry entry : entries) {
				if (! disIds.contains(entry.diseaseID)) {
					disIds.add(entry.diseaseID);
				}
			}
		}
		
		return disIds;
	}
	
	public DiseaseInfo getDiseaseInfoForID(String id) {
		return diseaseMap.get(id);
	}

	
	
	public static void main(String[] args) throws IOException {
		OMIMDB db = new OMIMDB(new File("/home/brendan/resources/OMIM/OMIM-Nov14-2012/"));
		
		int geneTot = 0;
		int disTot = 0;
		int disFound = 0;
		
		for(String gene : db.allGenes()) {
			List<String> disIDs = db.getDiseaseIDsForGene(gene);
			System.out.println("Gene : " + gene);
			geneTot++;
			for(String id : disIDs) {
				disTot++;
				System.out.print("\t" + id + " : ");
				DiseaseInfo info = db.getDiseaseInfoForID(id);
				if (info == null)
					System.out.println(" no info found ");
				else {
					System.out.println(info.getName());
					disFound++;
				}
			}
		}
		
		System.out.println("Found " + geneTot + " genes, " + disTot + " disease refs, " + disFound + " diseases with phenotypes");
	}
	
	/**
	 * Storage for some info about disease-gene relationships from OMIM
	 * @author brendan
	 *
	 */
	public class OMIMEntry {
		public final String diseaseName;
		public final String diseaseID;
		public final String geneID;
		public final String geneName;
		
		public OMIMEntry(String disease, String diseaseID, String geneName, String geneID) {
			this.diseaseName = disease;
			this.diseaseID = diseaseID;
			this.geneID = geneID;
			this.geneName = geneName;
		}
	}
}
