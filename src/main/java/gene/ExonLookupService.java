package gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import util.Interval;

/**
 * A class to facilitate lookups of gene and exon (but not c.dot or p.dot) information based on
 * chromosomal position. 
 * 
 * @author brendan
 *
 */
public class ExonLookupService extends AbstractIntervalContainer {

	
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
			
			//Eliminate weird unknown number after utrs
			if (exon.contains("utr")) {
				exon = exon.trim().split(" ")[0];
			}
			
			String desc = geneName + "(" + nmInfo + ") " + exon;
			addInterval(contig, start, end, desc);
			
			line = reader.readLine();
		}
		
		reader.close();
		
		//Sort all intervals within contigs by start position. 
		if (allIntervals != null) {
			for(String contig : allIntervals.keySet()) {
				List<Interval> intervals = allIntervals.get(contig);
				Collections.sort(intervals);
			}
		}
	}
	
	

	/**
	 * The primary function of this class: returns a list of genes, NMs, and exons intersecting 
	 * the given position. Returns an empty array if there are no hits. 
	 * @param contig
	 * @param pos
	 * @return
	 */
	public Object[] getInfoForPosition(String contig, int pos) {
		return getIntervalObjectsForRange(contig, pos, pos+1);
	}
	

	
	
	public static void main(String[] agrs) throws IOException {
		ExonLookupService es = new ExonLookupService();
		es.buildExonMap(new File("/home/brendan/resources/features20130508.bed"));
		
		Object[] infos = es.getIntervalObjectsForRange("15", 48937000, 48938200);
		
		for(int i=0; i<infos.length; i++) {
			System.out.println(infos[i]);
		}
	}
}
