package buffer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

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
		BufferedReader reader = null;

		if (this.file.getName().endsWith(".gz")) {
		    FileInputStream fileIs = null;
		    BufferedInputStream bufferedIs = null;
		    GZIPInputStream gzipIs = null;
		    try {
		    	fileIs = new FileInputStream(getAbsolutePath());
		        // Even though GZIPInputStream has a buffer it reads individual bytes
		        // when processing the header, better to add a buffer in-between
		    	gzipIs = new GZIPInputStream( new BufferedInputStream(fileIs, 65535));
		    	reader = new BufferedReader(new InputStreamReader(gzipIs));
		    } catch (IOException e) {
		      throw new UncheckedIOException(e);
		    }
		} else {
			reader = new BufferedReader(new FileReader(getAbsolutePath()));
		}
		
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
				try {
					Integer begin = Integer.parseInt(toks[1])+1;
					Integer end = Integer.parseInt(toks[2])+1;
					Interval interval = new Interval(begin, end);

					List<Interval> contigIntervals = intervals.get(contig);
					if (contigIntervals == null) {
						contigIntervals = new ArrayList<Interval>(1024);
						intervals.put(contig, contigIntervals);
						//System.out.println("BED file adding contig: " + contig);
					}
					contigIntervals.add(interval);
				}
				catch (Exception ex) {
					Logger.getLogger(Pipeline.primaryLoggerName).warning("Skipping invalid line in bed file: " + line);
				}
			}
			line = reader.readLine();
		}
		
		reader.close();
		sortAllContigs();
	}
	
	/**
	 * Sort all intervals in all contigs by starting position
	 */
	protected void sortAllContigs() {
		for(String contig : intervals.keySet()) {
			List<Interval> list = intervals.get(contig);
			Collections.sort(list, new IntervalComparator());
		}
	}

	
	
	
	

}
