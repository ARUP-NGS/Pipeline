package gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Interval;

/**
 * A class to facilitate lookups of gene and exon (but not c.dot or p.dot)
 * information based on chromosomal position. Right now uses the output the
 * Scrutil script ucsc_refseq2exon_bed.py to determine gene/exon/intron
 * locations
 * 
 * @author brendan
 *
 */
public class ExonLookupService extends BasicIntervalContainer {

	//Each feature gets categorized by exactly one of these
	public enum FeatureType { CODING_EXON, UTR5, UTR3, INTRON, UNKNOWN }
	
	
	//Map from gene name to transcript id (aka NM) to use for the gene. 
	//May be null or empty. 
	private Map<String, String> preferredNMs = null;

	
	//If specified, we report only the regions corresponding to these nms and ignore all else. 
	public void setPreferredNMs(Map<String, String> nms) {
		this.preferredNMs = nms;
	}

	/**
	 * Strips the transcript version identifier if present from the transcript id
	 * e.g. NM_000123.4 becomes NM_000123
	 * @param nm
	 * @return
	 */
	private static String stripTranscriptVersion(String nm) {
		int index = nm.indexOf(".");
		if (index > 0) {
			return nm.substring(0, index);
		}
		return nm;
	}
	
	/**
	 * Read all info from file into exonMap - this newer version uses output of
	 * the form produced by the Scrutil / ucsc_refseq2exon_bed.py script (which
	 * in turn uses gene / exon coordinates from the UCSC table browser) Stores
	 * extra information for the modified low coverage region reporting.
	 * While buildExonMap only
	 * 
	 * @param file File containing regions to parse
	 * @param strictNMs If true, don't report non-preferred NMs
	 * @throws IOException
	 */
	public void buildExonMapWithCDSInfo(File file, boolean strictNMs) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line = reader.readLine();
		line = reader.readLine(); // Skip first line
		while (line != null) {
		
			List<Interval> intervals = parseIntervals(line);
			
			for(Interval interval : intervals) {
				FeatureDescriptor fd = (FeatureDescriptor) interval.getInfo();
				if (preferredNMs == null) {
					addInterval(fd.contig, interval);
				} else {
					// Does this gene have a preferred NM?
					String nm = preferredNMs.get(fd.geneName);
					
					//Semi-confusing logic here: If there's no preferred NM for the gene, only
					//add the interval is strictNMs is set. If there is a preferred NM, then only add
					//the interval if the transcript matches the preferred transcript
					if (nm == null) {
						if (!strictNMs) {
							addInterval(fd.contig, interval);
						}
					} else if (fd.transcriptID.contains(nm)) {
						addInterval(fd.contig, interval);
					}
				}	
			}
			

			line = reader.readLine();
		}

		reader.close(); 

		// Sort all intervals within contigs by start position.
		if (allIntervals != null) {
			for (String contig : allIntervals.keySet()) {
				List<Interval> intervals = allIntervals.get(contig);
				Collections.sort(intervals);
			}
		}
	}

	

		/**
		 * Examine the given line of the input file and extract from it one or more intervals to be added to
		 * the intervals list. Each interval will contain a FeatureDescriptor object with some info
		 * about the genomic region covered.
		 * If 'strictNMs' is true and preferred NMs are set, then ignore all transcripts that are not preferred. 
		 * @param line
		 * @param strictNMs If true, ignore non-preferred transcripts
		 * @return
		 */
	private static List<Interval> parseIntervals(String line) {
		List<Interval> intervals = new ArrayList<Interval>();

		String[] toks = line.split("\t");
		String contig = toks[0].replace("chr", "");

		// input is in bed (0-based) coords, so we switch to 1-based when we
		// read in
		int start = Integer.parseInt(toks[1]) + 1;
		int end = Integer.parseInt(toks[2]) + 1;
		int cdsStart = Integer.parseInt(toks[10]) + 1;
		int cdsEnd = Integer.parseInt(toks[11]) + 1;
		char strand = toks[7].charAt(0);
		
		String nmInfo = stripTranscriptVersion(toks[3]);
		String geneName = toks[4];
		String exon = toks[5];

		String exonLoc = toks[6].toUpperCase(); // Should be either 'unk'
												// 'cds' '5UTR' or similar
		String[] locs = exonLoc.split("/");
		
		for(String loc : locs) {		
			FeatureDescriptor fd = new FeatureDescriptor();
			fd.contig = contig;
			fd.geneName = geneName;
			fd.transcriptID = nmInfo;
			fd.exonIntronIndex = Integer.parseInt(exon);
			fd.strand = strand;
			
			Interval interval = null;
			int lower = -1;
			int upper = -1;
			
			if (loc.contains("INTRON")) {	
				fd.featureType = FeatureType.INTRON;
				lower = start;
				upper = end;
				
			} else if (loc.contains("5UTR")) {
				fd.featureType = FeatureType.UTR5;
				if (fd.strand == '+') {
					lower = Math.min(start, cdsStart);
					upper = Math.min(end, cdsStart);
				} else {
					lower = Math.max(cdsEnd, start);
					upper = end;
				}
			} else if (loc.contains("CDS")) {
				fd.featureType = FeatureType.CODING_EXON;
				lower = Math.max(start, cdsStart);
				upper = Math.min(end, cdsEnd);
				
			} else if (loc.contains("3UTR")) {
				fd.featureType = FeatureType.UTR3;
				if (fd.strand == '+') {
					lower = Math.max(start, cdsStart);
					upper = end;
				} else {
					lower = Math.min(start, cdsStart);
					upper = Math.min(end, cdsStart);
				}
			} else {
				fd.featureType = FeatureType.UNKNOWN;
				lower = start;
				upper = end;
			}
				
			//Sanity checks
			if (lower < 0 || upper < 0) {
				throw new IllegalArgumentException("Bounds not initialized");
			}
			if (upper < lower) {
				throw new IllegalArgumentException("Bounds inferred incorrectly, line: " + line);
			}
			
			
			fd.start = lower;
			fd.end = upper;
			
			interval = new Interval(lower, upper, fd);
			intervals.add(interval);
			
		}			
		
		return intervals;
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
			exonLoc = exonLoc.replace("UNK", "");
			exonLoc = exonLoc.replace("CDS", "Coding");
			exonLoc = exonLoc.replace("5UTR", "");
			exonLoc = exonLoc.replace("3UTR", "");
			exonLoc = exonLoc.replace("/", " / ");

			if (exonLoc.length() > 1 && !exonLoc.equals("INTRON")) {
				exonLoc = "Exon #" + exon + " " + exonLoc;
			}

			if (exonLoc.length() > 1 && exonLoc.equals("INTRON")) {
				exonLoc = "Intron #" + exon;
			}

			String desc = geneName + "(" + nmInfo + ") " + exonLoc;
			
			if (preferredNMs == null) {
				addInterval(contig, start, end, desc);
			} 
			else {
				//Does this gene have a preferred NM? 
				String nm = preferredNMs.get(geneName);
				if (nm == null) {
					addInterval(contig, start, end, desc);
				} else if (nm.contains(nmInfo)) {
					addInterval(contig, start, end, desc);
				}
			}

			line = reader.readLine();
		}
		reader.close();
	}
	

	/**
	 * The primary function of this class: returns a list of genes, NMs, and exons intersecting 
	 * the given position. Returns an empty array if there are no hits. 
	 * @param contig
	 * @param pos
	 * @return
	 */
	public Object[] getInfoForPosition(String contig, int pos) {
		return getIntervalObjectsForRange(contig, pos, pos + 1);
	}

	
	public static String featureToString(FeatureType ft) {
		switch (ft) {
		case UNKNOWN:
			return "?";
		case CODING_EXON:
			return "Coding exon";
		case INTRON:
			return "Intron";
		case UTR3:
			return "3 UTR";
		case UTR5:
			return "5 UTR";
		}
		return "?";
	}
	/**
	 * Just a container for some basic information about a feature, including gene name, contig, etc. 
	 * No functionality here, just data
	 * @author brendan
	 *
	 */
	public static class FeatureDescriptor {

		String geneName;
		FeatureType featureType; 
		int exonIntronIndex;  //1-based index of exon or intron
		String transcriptID; // NM_000123 or similar
		char strand; //Either + or -
		String contig;		
		int start;
		int end;
		
		public String getGeneName() {
			return geneName;
		}

		public FeatureType getFeatureType() {
			return featureType;
		}

		public int getExonIntronIndex() {
			return exonIntronIndex;
		}

		public String getTranscriptID() {
			return transcriptID;
		}

		public char getStrand() {
			return strand;
		}

		public String getContig() {
			return contig;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}
		
		public String toShortString() {
			String msg = featureToString(featureType);
			if (featureType == FeatureType.CODING_EXON || featureType == FeatureType.INTRON) {
				msg = msg + " " + exonIntronIndex;
			}
			return msg;
		}
		
		public String toString() {
			String msg = geneName + "(" + transcriptID + ") " + featureToString(featureType);
			if (featureType == FeatureType.CODING_EXON || featureType == FeatureType.INTRON) {
				msg = msg + " " + exonIntronIndex;
			}
			return msg;
		}
		
	}
	
	
	 /**
	  * Merges multiple feature descriptors (from the ExonLookupService) into a single sensible string. 
	  * @param fds
	  * @return
	  */
	 public static String mergeFeatures(List<FeatureDescriptor> fds) {
		 if (fds.size() == 0) {
			 return "";
		 }
		 if (fds.size() == 1) {
			 return fds.get(0).toString();
		 }
		 
		 Map<String, List<FeatureDescriptor>> byNM = new HashMap<String, List<FeatureDescriptor>>();
		 for(FeatureDescriptor fd : fds) {
			 String nm = fd.getTranscriptID();
			 List<FeatureDescriptor> val = byNM.get(nm);
			 if (val == null) {
				 val = new ArrayList<FeatureDescriptor>();
				 byNM.put(nm, val);
			 }
			 val.add(fd);
		 }
		 
		 //For each NM produce a nice summary
		 String result = "";
		 boolean firstNM = true;
		 for(String nm : byNM.keySet()) {
			 List<FeatureDescriptor> feats = byNM.get(nm);
			 if (feats.size()==0) {
				 continue;
			 }
			 
			 if (!firstNM) {
				 result = result + "; ";
			 }
			 firstNM = false;
			 result = result + feats.get(0).getGeneName() + " (" + feats.get(0).getTranscriptID() + ") ";
			 boolean first = true;
			 for(FeatureDescriptor fd : feats) {
				 if (!first) {
					 result = result + ", ";
				 }
				 result = result + fd.toShortString();
				 
				 first = false;
			 }
			 
			 
		 }
		 return result;
	 }
	 
//Testing only code
//	public static void main(String[] agrs) throws IOException {
//		ExonLookupService es = new ExonLookupService();
//		Map<String, String> prefNMs = new HashMap<String, String>();
//		prefNMs.put("TGFB2", "NM_001135599");
//		prefNMs.put("CCDC107", "NM_174923");
//		es.setPreferredNMs(prefNMs);
//		es.buildExonMapWithCDSInfo(new File("/home/brendan/resources/features20150106.v3.bed"), true);
//		
//		Object[] infos = es.getIntervalObjectsForRange("9", 35658000, 35658400);
//		
//		List<FeatureDescriptor> fds = new ArrayList<FeatureDescriptor>();
//		for(Object o : infos) {
//			fds.add((FeatureDescriptor)o);
//		}
//		
//		System.out.println( mergeFeatures(fds) );
//	}
	
}
