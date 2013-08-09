package operator.gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import pipeline.Pipeline;

/**
 * A class to read and store entries from the HGMD database file. The file is in a custom, BED-like format, with
 * the first three columns assumed to be chromosome, start, and stop position of a feature, and the remaining
 *  columns describing the feature.
 * @author brendan
 *
 */
public class HGMDB {

	protected Map<String, List<HGMDInfo>> db = new HashMap<String, List<HGMDInfo>>();
	
	protected Map<String, List<HGMDInfo>> geneMap = new HashMap<String, List<HGMDInfo>>();
	
	/**
	 * Create the db by reading in information from the given db file
	 * @param dbFile
	 * @throws IOException
	 */
	public void initializeMap(File dbFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(dbFile));
		String line = reader.readLine();
		while(line != null) {
			importFromLine(line);
			line = reader.readLine();
		}
		reader.close();
		sortAll();
		
		int count = 0;
		for(String contig : db.keySet()) {
			count += db.get(contig).size();
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("HGMDb initialzed with " + count + " total variants in " + geneMap.size() + " genes");
	}
	
	/**
	 * Sort all info objects within each contig by starting position for quicker finding
	 */
	private void sortAll() {
		for(String contig : db.keySet()) {
			List<HGMDInfo> list = db.get(contig);
			Collections.sort(list, new InfoComparator());
		}
	}
	
	/**
	 * Obtain the record associated with the given contig and position, or null if
	 * there is no such record
	 * @param contig
	 * @param pos
	 * @return
	 */
	public HGMDInfo getRecord(String contig, int pos) {
		List<HGMDInfo> list = db.get(contig);
		if (list == null)
			return null;
		
		qInfo.pos = pos;
		int index = Collections.binarySearch(list, qInfo, new InfoComparator());
		if (index < 0)
			return null;
		else 
			return list.get(index);
		
	}
	
	/**
	 * Return list of all records associated with given gene name
	 * @param geneName
	 * @return
	 */
	public List<HGMDInfo> getRecordsForGene(String geneName) {
		return geneMap.get(geneName);
	}
	
	private void importFromLine(String line) {
		String[] toks = line.split("\t");
		HGMDInfo info = new HGMDInfo();
		info.contig = toks[0];
		
		try {
			info.pos = Integer.parseInt(toks[1]);
		}
		catch(NumberFormatException nfe) {
			//Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not import HGMD variant from line: " + line);
			return;
		}
		info.cDot = toks[2];
		info.pDot = toks[3];
		info.geneName = toks[4];
		info.condition = toks[5];
		info.assocType = toks[6];
		info.citation = toks[7];
		
		if (toks.length>8) {
			info.pmid = toks[8];
		}
		else {
			info.pmid = "";
		}

		List<HGMDInfo> list = db.get(info.contig);
		if (list == null) {
			list = new ArrayList<HGMDInfo>(1024);
			db.put(info.contig, list);
		}
		list.add(info);

		List<HGMDInfo> geneList = geneMap.get(info.geneName);
		if (geneList == null) {
			geneList = new ArrayList<HGMDInfo>(256);
			geneMap.put(info.geneName, geneList);
		}
		geneList.add(info);
	}
	
	/**
	 * Read information in this single line into the map that stores all of the data
	 * @param line
	 */
	private void importFromLineOld(String line) {
		String[] toks = line.split("\t");
		
		String contig = toks[0].replace("chr", "");
		Integer pos = Integer.parseInt(toks[2]); //CRITICAL THAT THIS IS THE THIRD COLUMN, not the second!
		String id = toks[5];
		String nmAndCDot = toks[9];
		String cDot = "?";
		String pDot = "?";
		String nm = "?";
		boolean strand = toks[8].equals("+");
		if (!nmAndCDot.contains(":")) {
			//System.err.println("Confusing nm and cdot str..." + nmAndCDot);
		}
		else {
			String[] cToks = nmAndCDot.split(":");
			nm = cToks[0];
			cDot = cToks[1];
		}
		
		String npAndPDot = toks[10];
		if (npAndPDot.contains(":")) {
			String[] pToks = npAndPDot.split(":");
			pDot = pToks[1];
		}
		String gene = toks[11];
		String condition = toks[12].replace("$$", " ").trim();
		HGMDInfo info = new HGMDInfo();
		info.geneName = gene;
		info.condition = condition;
		info.cDot = cDot;
		//info.hgmdID = id;
		info.nm = nm;
		info.pos = pos;
		//info.strand = strand;
		
		//System.out.println(contig + ":" + pos + " " + info.toString());
		List<HGMDInfo> list = db.get(contig);
		if (list == null) {
			list = new ArrayList<HGMDInfo>(1024);
			db.put(contig, list);
		}
		list.add(info);
		
		List<HGMDInfo> geneList = geneMap.get(info.geneName);
		if (geneList == null) {
			geneList = new ArrayList<HGMDInfo>(256);
			geneMap.put(info.geneName, geneList);
		}
		geneList.add(info);
	}
	
	class InfoComparator implements Comparator<HGMDInfo> {

		@Override
		public int compare(HGMDInfo a, HGMDInfo b) {
			return b.pos - a.pos;
		}
		
	}
	
//	public void emitAsCSV(PrintStream out) {
//		for(String contig: db.keySet()) {
//			for(HGMDInfo info : db.get(contig)) {
//				String cd = info.cDot;
//				if (! cd.startsWith("c.")) {
//					continue;
//				}
//				char ref = cd.charAt(cd.length()-3);
//				char alt = cd.charAt(cd.length()-1);
//				if (! info.strand) {
//					ref = complement(ref);
//					alt = complement(alt);
//				}
//				out.println(contig + "\t" + info.pos + "\t" + ref + "\t" + alt);
//			}
//		}
//	}
	
	public static char complement(char b) {
		if (b=='A')	return 'T';
		if (b=='C')	return 'G';
		if (b=='G')	return 'C';
		if (b=='T')	return 'A';
		return '?';
	}
	
	/**
	 * Structure to store a bit of info about each position
	 * @author brendan
	 *
	 */
	public class HGMDInfo {
		public String pDot;
		public String pmid;
		String contig;
		int pos; //Chromosomal position 
		public String nm;
		public String geneName;
		public String cDot;
		public String condition;
		public String assocType;
		String pubmedID;
		public String citation;
		String rsNum;
		
		public String toString() {
			return " nm: " + nm + " gene: " + geneName + " condition: " + condition + " cdot: " + cDot;
		}
	}
	
	
//	public static void main(String[] args) {
//		HGMDB db = new HGMDB();
//		try {
//			db.initializeMap(new File("/home/brendan/resources/HGMD_DMonly_b37.csv"));
//			
//			PrintStream dbStream = new PrintStream(new FileOutputStream("HGMD_DMonly.vars.csv"));
//			db.emitAsCSV(dbStream);
//			dbStream.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	
	//Used to speed up db lookups
	private HGMDInfo qInfo = new HGMDInfo();
	
}
