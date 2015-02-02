package gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
=======
import java.util.Collections;
>>>>>>> a0f11effdcc8959a0df04068632a9b9d76b3e493
import java.util.List;
import java.util.Map;

import util.Interval;

/**
<<<<<<< HEAD
 * A class to facilitate lookups of gene and exon (but not c.dot or p.dot)
 * information based on chromosomal position. Right now uses the output the
 * Scrutil script ucsc_refseq2exon_bed.py to determine gene/exon/intron
 * locations
 * 
=======
 * A class to facilitate lookups of gene and exon (but not c.dot or p.dot) information based on
 * chromosomal position. Right now uses the output the Scrutil script ucsc_refseq2exon_bed.py
 * to determine gene/exon/intron locations
>>>>>>> a0f11effdcc8959a0df04068632a9b9d76b3e493
 * @author brendan
 *
 */
public class ExonLookupService extends BasicIntervalContainer {

	private Map<String, String> preferredNMs = null;
<<<<<<< HEAD

	// If specified, we report only the regions corresponding to these nms and
	// ignore all else.
=======
	
	//If specified, we report only the regions corresponding to these nms and ignore all else. 
>>>>>>> a0f11effdcc8959a0df04068632a9b9d76b3e493
	public void setPreferredNMs(Map<String, String> nms) {
		this.preferredNMs = nms;
	}

	/**
<<<<<<< HEAD
	 * Read all info from file into exonMap - this newer version uses output of
	 * the form produced by the Scrutil / ucsc_refseq2exon_bed.py script (which
	 * in turn uses gene / exon coordinates from the UCSC table browser) Stores
	 * extra information for the modified low coverage region reporting.
	 * While buildExonMap only
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void buildExonMapWithCDSInfo(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line = reader.readLine();
		line = reader.readLine(); // Skip first line
		while (line != null) {
			String[] toks = line.split("\t");
			String contig = toks[0].replace("chr", "");

			// input is in bed (0-based) coords, so we switch to 1-based when we
			// read in
			int start = Integer.parseInt(toks[1]) + 1;
			int end = Integer.parseInt(toks[2]) + 1;
			int cdsStart = Integer.parseInt(toks[10]) + 1;
			int cdsEnd = Integer.parseInt(toks[11]) + 1;
			if(cdsEnd < start || cdsStart > end)
				break;
			else{
				if(end > cdsEnd)
					end = cdsEnd;
				if(start < cdsStart)
					start = cdsStart;
			}
			int numBases = end - start;

			String nmInfo = toks[3];

			String geneName = toks[4];
			String exon = toks[5];

			String exonLoc = toks[6].toUpperCase(); // Should be either 'unk'
													// 'cds' '5UTR' or similar
			exonLoc = exonLoc.replace("UNK", "");
			exonLoc = exonLoc.replace("CDS", "Coding");
			exonLoc = exonLoc.replace("5UTR", "");
			exonLoc = exonLoc.replace("3UTR", "");
			exonLoc = exonLoc.replace("/", " / ");

			if (exonLoc.length() > 1 && !exonLoc.equals("INTRON")) {
				exonLoc = "Exon #" + exon + " " + exonLoc;
			}

			if (exonLoc.length() > 1 && exonLoc.equals("INTRON"))
				break;
			if(!exonLoc.contains("Coding"))
				break;
			String desc = geneName + "(" + nmInfo + ") (" + String.valueOf(numBases) + " bases)";
			
			if (preferredNMs == null) {
				addInterval(contig, start, end, desc);
			} else {
				// Does this gene have a preferred NM?
				String nm = preferredNMs.get(geneName);
				if (nm == null) {
					addInterval(contig, start, end, desc);
				} else if (nmInfo.contains(nm)) {
					addInterval(contig, start, end, desc);
				}
			}

			line = reader.readLine();
		}

		reader.close(); // iouyoiu

		// Sort all intervals within contigs by start position.
		if (allIntervals != null) {
			for (String contig : allIntervals.keySet()) {
				List<Interval> intervals = allIntervals.get(contig);
				Collections.sort(intervals);
			}
		}
	}

	/**
	 * Read all info from file into exonMap - this newer version uses output of
	 * the form produced by the Scrutil / ucsc_refseq2exon_bed.py script (which
	 * in turn uses gene / exon coordinates from the UCSC table browser)
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void buildExonMap(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line = reader.readLine();
		line = reader.readLine(); // Skip first line
		while (line != null) {
			String[] toks = line.split("\t");
			String contig = toks[0].replace("chr", "");

			// input is in bed (0-based) coords, so we switch to 1-based when we
			// read in
			int start = Integer.parseInt(toks[1]) + 1;
			int end = Integer.parseInt(toks[2]) + 1;

			String nmInfo = toks[3];

			String geneName = toks[4];
			String exon = toks[5];

			String exonLoc = toks[6].toUpperCase(); // Should be either 'unk'
													// 'cds' '5UTR' or similar
=======
	 * Read all info from file into exonMap - this newer version uses output of the form 
	 * produced by the Scrutil / ucsc_refseq2exon_bed.py script (which in turn uses gene / exon 
	 * coordinates from the UCSC table browser) 
	 * @param file
	 * @throws IOException 
	 */
	public void buildExonMap(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line = reader.readLine();
		line = reader.readLine(); //Skip first line
		while(line != null) {
			String[] toks = line.split("\t");
			String contig = toks[0].replace("chr", ""); 
			
			//input is in bed (0-based) coords, so we switch to 1-based when we read in
			int start = Integer.parseInt(toks[1])+1;
			int end = Integer.parseInt(toks[2])+1;
			
			String nmInfo = toks[3];
			
			
			String geneName = toks[4];
			String exon = toks[5];
			
			String exonLoc = toks[6].toUpperCase(); //Should be either 'unk' 'cds' '5UTR' or similar
>>>>>>> a0f11effdcc8959a0df04068632a9b9d76b3e493
			exonLoc = exonLoc.replace("UNK", "");
			exonLoc = exonLoc.replace("CDS", "Coding");
			exonLoc = exonLoc.replace("5UTR", "5 UTR");
			exonLoc = exonLoc.replace("3UTR", "3 UTR");
			exonLoc = exonLoc.replace("/", " / ");
<<<<<<< HEAD

			if (exonLoc.length() > 1 && !exonLoc.equals("INTRON")) {
				exonLoc = "Exon #" + exon + " " + exonLoc;
			}

			if (exonLoc.length() > 1 && exonLoc.equals("INTRON")) {
				exonLoc = "Intron #" + exon;
			}

			String desc = geneName + "(" + nmInfo + ") " + exonLoc;

			if (preferredNMs == null) {
				addInterval(contig, start, end, desc);
			} else {
				// Does this gene have a preferred NM?
=======
			
			if (exonLoc.length()>1 && !exonLoc.equals("INTRON")) {
				exonLoc = "Exon #" + exon + " " + exonLoc;
			}
			
			if (exonLoc.length()>1 && exonLoc.equals("INTRON")) {
				exonLoc = "Intron #" + exon;
			}
			
			String desc = geneName + "(" + nmInfo + ") " + exonLoc;
			
			if (preferredNMs == null) {
				addInterval(contig, start, end, desc);
			} 
			else {
				//Does this gene have a preferred NM? 
>>>>>>> a0f11effdcc8959a0df04068632a9b9d76b3e493
				String nm = preferredNMs.get(geneName);
				if (nm == null) {
					addInterval(contig, start, end, desc);
				} else if (nmInfo.contains(nm)) {
					addInterval(contig, start, end, desc);
<<<<<<< HEAD
				}
			}

			line = reader.readLine();
		}

		reader.close(); // iouyoiu

		// Sort all intervals within contigs by start position.
		if (allIntervals != null) {
			for (String contig : allIntervals.keySet()) {
=======
				} 
			}
			
			line = reader.readLine();
		}
		
		reader.close(); //iouyoiu
		
		//Sort all intervals within contigs by start position. 
		if (allIntervals != null) {
			for(String contig : allIntervals.keySet()) {
>>>>>>> a0f11effdcc8959a0df04068632a9b9d76b3e493
				List<Interval> intervals = allIntervals.get(contig);
				Collections.sort(intervals);
			}
		}
	}
<<<<<<< HEAD

	/**
	 * Read all info from file into exonMap
	 * 
	 * @param file
	 * @throws IOException
	 */
	// public void buildExonMap(File file) throws IOException {
	// BufferedReader reader = new BufferedReader(new FileReader(file));
	//
	// String line = reader.readLine();
	// while(line != null) {
	// String[] toks = line.split("\t");
	// String contig = toks[0].replace("chr", "");
	//
	// //input is in bed (0-based) coords, so we switch to 1-based when we read
	// in
	// int start = Integer.parseInt(toks[1])+1;
	// int end = Integer.parseInt(toks[2])+1;
	//
	// String nmInfo = toks[3];
	// int idx = nmInfo.indexOf("_", 4);
	// if (idx > 0) {
	// nmInfo = nmInfo.substring(0, idx);
	// }
	//
	// String geneName = toks[6];
	// String exon = toks[7];
	//
	// //Eliminate weird unknown number after utrs
	// if (exon.contains("utr")) {
	// exon = exon.trim().split(" ")[0];
	// }
	//
	// String desc = geneName + "(" + nmInfo + ") " + exon;
	// addInterval(contig, start, end, desc);
	//
	// line = reader.readLine();
	// }
	//
	// reader.close();
	//
	// //Sort all intervals within contigs by start position.
	// if (allIntervals != null) {
	// for(String contig : allIntervals.keySet()) {
	// List<Interval> intervals = allIntervals.get(contig);
	// Collections.sort(intervals);
	// }
	// }
	// }

	/**
	 * The primary function of this class: returns a list of genes, NMs, and
	 * exons intersecting the given position. Returns an empty array if there
	 * are no hits.
	 * 
=======
	
	
	/**
	 * Read all info from file into exonMap
	 * @param file
	 * @throws IOException 
	 */
//	public void buildExonMap(File file) throws IOException {
//		BufferedReader reader = new BufferedReader(new FileReader(file));
//		
//		String line = reader.readLine();
//		while(line != null) {
//			String[] toks = line.split("\t");
//			String contig = toks[0].replace("chr", "");
//			
//			//input is in bed (0-based) coords, so we switch to 1-based when we read in
//			int start = Integer.parseInt(toks[1])+1;
//			int end = Integer.parseInt(toks[2])+1;
//			
//			String nmInfo = toks[3];
//			int idx = nmInfo.indexOf("_", 4);
//			if (idx > 0) {
//				nmInfo = nmInfo.substring(0, idx);
//			}
//			
//			String geneName = toks[6];
//			String exon = toks[7];
//			
//			//Eliminate weird unknown number after utrs
//			if (exon.contains("utr")) {
//				exon = exon.trim().split(" ")[0];
//			}
//			
//			String desc = geneName + "(" + nmInfo + ") " + exon;
//			addInterval(contig, start, end, desc);
//			
//			line = reader.readLine();
//		}
//		
//		reader.close();
//		
//		//Sort all intervals within contigs by start position. 
//		if (allIntervals != null) {
//			for(String contig : allIntervals.keySet()) {
//				List<Interval> intervals = allIntervals.get(contig);
//				Collections.sort(intervals);
//			}
//		}
//	}
	
	

	/**
	 * The primary function of this class: returns a list of genes, NMs, and exons intersecting 
	 * the given position. Returns an empty array if there are no hits. 
>>>>>>> a0f11effdcc8959a0df04068632a9b9d76b3e493
	 * @param contig
	 * @param pos
	 * @return
	 */
	public Object[] getInfoForPosition(String contig, int pos) {
<<<<<<< HEAD
		return getIntervalObjectsForRange(contig, pos, pos + 1);
	}

	public static void main(String[] agrs) throws IOException {
		ExonLookupService es = new ExonLookupService();
		es.buildExonMap(new File(
				"/home/brendan/resources/features20140909.v2.bed"));

		Object[] infos = es
				.getIntervalObjectsForRange("10", 89690337, 89693769);

		for (int i = 0; i < infos.length; i++) {
=======
		return getIntervalObjectsForRange(contig, pos, pos+1);
	}
	

	
	
	public static void main(String[] agrs) throws IOException {
		ExonLookupService es = new ExonLookupService();
		es.buildExonMap(new File("/home/brendan/resources/features20140909.v2.bed"));
		
		Object[] infos = es.getIntervalObjectsForRange("10", 89690337, 89693769);
		
		for(int i=0; i<infos.length; i++) {
>>>>>>> a0f11effdcc8959a0df04068632a9b9d76b3e493
			System.out.println(infos[i]);
		}
	}
}
