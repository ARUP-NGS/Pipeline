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

import java.util.regex.*;

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
	public void initializeMap(File snvFile, File indelFile) throws IOException {
		
		//Import SNVs
		BufferedReader reader = new BufferedReader(new FileReader(snvFile));
		String line = reader.readLine();
		while(line != null) {
			HGMDInfo info = importSNVFromLine(line);
			if (info == null) {
				line = reader.readLine();
				continue;
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
			line = reader.readLine();
		}
		reader.close();
		
		//Import indels
		reader = new BufferedReader(new FileReader(indelFile));
		line = reader.readLine();
		while(line != null) {
			HGMDInfo info = importIndelFromLine(line);
			if (info == null) {
				line = reader.readLine();
				continue;
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
	 * Obtain the record associated with the given contig, position, ref, and alt, or null if there is no such record
	 * @param contig
	 * @param pos
	 * @param ref
	 * @param alt
	 * @return
	 */
	public HGMDInfo getRecordRefAlt(String contig, int pos, String ref, String alt) {
		List<HGMDInfo> list2 = db.get(contig);
		if (list2 == null)
			return null;

		qInfo_exact.pos = pos;
		qInfo_exact.ref = ref;
		qInfo_exact.alt = alt;

		int index = Collections.binarySearch(list2, qInfo_exact, new InfoComparator());

			if (index < 0)
				return null;
			else{
				if (qInfo_exact.ref.equals(list2.get(index).ref) && qInfo_exact.alt.equals(list2.get(index).alt))
					return list2.get(index);
				else
					return null;
		}
	}

	/**
	 * Return list of all records associated with given gene name
	 * @param geneName
	 * @return
	 */
	public List<HGMDInfo> getRecordsForGene(String geneName) {
		return geneMap.get(geneName);
	}
	
	
	/**
	 * Newer version, works with updated HGMD files since October, 2013
	 * @param line
	 * @return
	 */
	private HGMDInfo importSNVFromLine(String line) {
		if (line.startsWith("//")) {
			return null;
		}
		if (line.length() < 2) {
			return null;
		}
		String[] toks = line.split("\t");
		HGMDInfo info = new HGMDInfo();
		String coords = toks[5];
		String[] coordToks = coords.split(":");
		if (coordToks.length == 1) {
			return null;
		}
		String contig = coordToks[0].replace("chr", "");
		
		info.contig = contig;
		
		try {
			info.pos = Integer.parseInt(coordToks[1]);
			info.posEnd = info.pos;
		}
		catch(NumberFormatException nfe) {
			//Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not import HGMD variant from line: " + line);
			return null;
		}
		
		info.cDot = toks[6];
		info.pDot = toks[7];
		info.geneName = toks[8];
		info.condition = toks[9];
		info.assocType = toks[1];
		info.ref = "";
		info.alt = "";



		try{ // Grabbing ref and alt from c.dot
			String[] cDotsplit = toks[6].split(":");
			String actualcDot = cDotsplit[1];
			Pattern pattern = Pattern.compile("([0-9])+([ATCG])[-]([ATCG])");
			Matcher m = pattern.matcher(actualcDot);

			if (m.find()){
				if (coordToks[2].equals("-")){
					info.ref = m.group(2).replace("A", "t").replace("T", "a").replace("G", "c").replace("C", "g").toUpperCase();
					info.alt = m.group(3).replace("A", "t").replace("T", "a").replace("G", "c").replace("C", "g").toUpperCase();
				}
				else{
					info.ref = m.group(2);
					info.alt = m.group(3);
				}
			}
		}
		catch (Exception ex){//NULL values are caused by variant falling in INTRONIC REGIONS!!!!
			info.ref = "?";
			info.alt = "?";
		}

		try {
			info.citation = toks[18] + " " + toks[19] + " vol. " + toks[20] + ": " + toks[21] + " (" + toks[22] + ")" ;
			info.pmid = toks[23];
		}
		catch (Exception ex) {
			//Sometimes this happens, if  so fine, but no citation
		}

		return info;
	}
	

	private HGMDInfo importIndelFromLine(String line) {
		if (line.startsWith("//")) {
			return null;
		}
		if (line.length() < 2) {
			return null;
		}
		String[] toks = line.split("\t");
		if (toks.length < 6) {
			return null;
		}

		HGMDInfo info = new HGMDInfo();
		
		String coords = toks[6];
		String[] coordToks = coords.split(":");
		if (coordToks.length == 1) {
			return null;
		}
		String contig = coordToks[0].replace("chr", "");
		
		
		info.contig = contig;
		
		try {
			String noStrand = coordToks[1].replace(" (-)", "").replace(" (+)", "");
			String posEnd = null;
			if (noStrand.contains("-")) {
				posEnd = noStrand.substring(noStrand.indexOf("-")+1);
				noStrand = noStrand.substring(0, noStrand.indexOf("-"));
			}
			info.pos = Integer.parseInt(noStrand);
			if (posEnd != null) {
				info.posEnd = Integer.parseInt(posEnd);
			}
			
		}
		catch(NumberFormatException nfe) {
			//Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not import HGMD variant from line: " + line);
			return null;
		}
		
		info.cDot = toks[4];
		info.pDot = null;
		info.geneName = toks[2];
		info.condition = toks[3];
		info.assocType = toks[1];


		try {
			info.citation = toks[13] + " " + toks[14] + " vol. " + toks[15] + ": " + toks[16] + " (" + toks[17] + ")" ;
			info.pmid = toks[18];
		}
		catch (Exception ex) {
			//OK, not always citation info available, in this case there are no columns
		}
		

		return info;
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
		public int pos; //Chromosomal position 
		public int posEnd; //End position of variant
		public String nm;
		public String geneName;
		public String cDot;
		public String condition;
		public String assocType;
		public String citation;
		public String ref;
		public String alt;
		
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
	private HGMDInfo qInfo_exact = new HGMDInfo(); //Looking at exact ref and alt matches in HGMD database file
	
}
