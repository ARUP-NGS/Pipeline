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
 * A class to facilitate lookups of gene and exon (but not c.dot or p.dot) information based on
 * chromosomal position. 
 * 
 * @author brendan
 *
 */
public class ExonLookupService {

	
	private Map<String, List<Interval>> exonMap = null;
	
	/**
	 * Read all info from file into exonMap
	 * @param file
	 * @throws IOException 
	 */
	public void buildExonMap(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line = reader.readLine();
		while(line != null) {
			String[] toks = line.split("\t");
			String contig = toks[0].replace("chr", "");
			
			//input is in bed (0-based) coords, so we switch to 1-based when we read in
			int start = Integer.parseInt(toks[1])+1;
			int end = Integer.parseInt(toks[2])+1;
			
			String nmInfo = toks[3];
			int idx = nmInfo.indexOf("_", 4);
			if (idx > 0) {
				nmInfo = nmInfo.substring(0, idx);
			}
			
			String geneName = toks[6];
			String exon = toks[7];
			
			String desc = geneName + "(" + nmInfo + ") " + exon;
			addInterval(contig, start, end, desc);
			
			line = reader.readLine();
		}
		
		reader.close();
		
		//Sort all intervals within contigs by start position. 
		if (exonMap != null) {
			for(String contig : exonMap.keySet()) {
				List<Interval> intervals = exonMap.get(contig);
				Collections.sort(intervals);
			}
		}
	}
	
	public String[] getInfoForRange(String contig, int start, int end) {
		if (exonMap == null) {
			throw new IllegalStateException("Exon information has not been initialized");
		}
		
		List<Interval> intervals = exonMap.get(contig);
		
		if (intervals == null) {
			return new String[]{};
		}
		
		//Find all intervals that contain pos and store their info in a list
		List<String> exons = new ArrayList<String>(4);
		for(Interval inter : intervals) {
			if (inter.intersects(start, end)) {
				exons.add( inter.getInfo() != null 
						? inter.getInfo().toString()
						: "");
			}
		}
		
		return exons.toArray(new String[exons.size()]);
	}

	/**
	 * The primary function of this class: returns a list of genes, NMs, and exons intersecting 
	 * the given position. Returns an empty array if there are no hits. 
	 * @param contig
	 * @param pos
	 * @return
	 */
	public String[] getInfoForPosition(String contig, int pos) {
		return getInfoForRange(contig, pos, pos+1);
	}
	
	/**
	 * Add a new interval to the exonMap, creating a new contig - and a new map - if necessary. 
	 * @param contig
	 * @param start
	 * @param end
	 * @param desc
	 */
	private void addInterval(String contig, int start, int end, String desc) {
		if (exonMap == null) {
			exonMap = new HashMap<String, List<Interval>>();
		}
		
		List<Interval> intervals = exonMap.get(contig);
		if (intervals == null) {
			intervals = new ArrayList<Interval>(1024);
			exonMap.put(contig, intervals);
		}
		
		intervals.add(new Interval(start, end, desc));
	}
	
	
	public static void main(String[] agrs) throws IOException {
		ExonLookupService es = new ExonLookupService();
		es.buildExonMap(new File("/home/brendan/resources/features20130508.bed"));
		
		String[] infos = es.getInfoForRange("15", 48937000, 48938200);
		
		for(int i=0; i<infos.length; i++) {
			System.out.println(infos[i]);
		}
	}
}
