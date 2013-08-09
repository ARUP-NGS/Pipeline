package buffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import pipeline.Pipeline;
import util.Interval;

public class BEDFile extends IntervalsFile {

	
	public BEDFile() {
	}
	
	public BEDFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "BEDfile";
	}
	
	/**
	 * Build an intervals map from this BED file and strip 'chr' from all contigs
	 * @throws IOException
	 */
	public void buildIntervalsMap() throws IOException {
		buildIntervalsMap(true);
	}
	
	/**
	 * Construct/initialize a map which allows us to easily look up which sites are in
	 * the intervals described by this BED file. If arg is true, strip chr from all contig labels
	 * @throws IOException 
	 */
	public void buildIntervalsMap(boolean stripChr) throws IOException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		//logger.info("Building intervals map for BED file " + getFilename());
		BufferedReader reader = new BufferedReader(new FileReader(getAbsolutePath()));
		String line = reader.readLine();
		intervals = new HashMap<String, List<Interval>>();
		while (line != null) {
			if (line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			if (line.length()>0) {
				String[] toks = line.split("\\s");
				String contig = toks[0];
				if (stripChr)
					contig = contig.replace("chr", "");
				toks[1] = toks[1].trim();
				toks[2] = toks[2].trim();
				Integer begin = Integer.parseInt(toks[1]);
				Integer end = Integer.parseInt(toks[2]);
				Interval interval = new Interval(begin, end);

				List<Interval> contigIntervals = intervals.get(contig);
				if (contigIntervals == null) {
					contigIntervals = new ArrayList<Interval>(2048);
					intervals.put(contig, contigIntervals);
					//System.out.println("BED file adding contig: " + contig);
				}
				contigIntervals.add(interval);
			}
			line = reader.readLine();
		}
		
		reader.close();
		sortAllContigs();
		
//		logger.info("Done building intervals map for " + getFilename() + " Interval count: " + this.getIntervalCount() + " extent: " + this.getExtent());
//		for(String contig : intervals.keySet()) {
//			List<Interval> list = intervals.get(contig);
//			int tot = countSize(list);
//			System.out.println(contig + "\t :" + list.size() + "\t" + tot);
//		}
	}
	
	/**
	 * Sort all intervals in all contigs by starting position
	 */
	private void sortAllContigs() {
		for(String contig : intervals.keySet()) {
			List<Interval> list = intervals.get(contig);
			Collections.sort(list, new IntervalComparator());
		}
	}

	/**
	 * Count number of bases subtended by all intervals in the list
	 * @param list
	 * @return
	 */
	private static int countSize(List<Interval> list) {
		int size = 0;
		for(Interval inter : list) {
			size += inter.end - inter.begin;
		}
		return size;
	}
	
	
	
	

}
