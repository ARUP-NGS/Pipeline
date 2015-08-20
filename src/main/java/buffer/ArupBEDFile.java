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

import operator.OperationFailedException;
import pipeline.Pipeline;
import util.Interval;


public class ArupBEDFile extends BEDFile {

	public ArupBEDFile() {
	}
	
	public ArupBEDFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "ArupBEDFile";
	}
	
	/**
	 * Construct/initialize a map which allows us to easily look up which sites are in
	 * the intervals described by this BED file. If arg is true, strip chr from all contig labels
	 * @throws IOException, IllegalArgumentException 
	 */
	@Override
	public void buildIntervalsMap(boolean stripChr) throws IOException, IllegalArgumentException {
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
				
				if (toks.length < 3) {
					throw new IllegalArgumentException("ARUP Bed file line is malformed\n" 
							+ line + "\n" + "in bed file " + this.file.getName());
				}
				Integer begin;
				Integer end;
				try {
				toks[1] = toks[1].trim();
				toks[2] = toks[2].trim();
					begin = Integer.parseInt(toks[1])+1;
					end = Integer.parseInt(toks[2])+1;
				} catch (Exception e) {
					// REPLACES: Logger.getLogger(Pipeline.primaryLoggerName).warning("Skipping invalid line in bed file: " + line);
					e.printStackTrace();
					throw new IllegalArgumentException("Bed file appears to be malformed\n" + e);
				}
				if (toks.length < 4) {
					throw new IllegalArgumentException("ARUP Bed file has no 4th column for transcript ids on line \n" 
							+ line + "\n" + "in bed file " + this.file.getName());
				}

				String[] untrimmedTrs = toks[3].split("\\|");
				String[] transcripts = new String[untrimmedTrs.length];
				for (int i = 0; i < untrimmedTrs.length; i++) {
					transcripts[i] = untrimmedTrs[i].trim();
					if (transcripts[i].length() < 1) {
					throw new IllegalArgumentException("ARUP BED file transcript id appears to be blank in 4th column for line \n" 
							+ line + "\n" + "in bed file " + this.file.getName());
					}
				}

				Interval interval = new Interval(begin, end, transcripts);

				List<Interval> contigIntervals = intervals.get(contig);
				if (contigIntervals == null) {
					contigIntervals = new ArrayList<Interval>(1024);
					intervals.put(contig, contigIntervals);
					//System.out.println("BED file adding contig: " + contig);
				}
				contigIntervals.add(interval);
			}
			line = reader.readLine();
		}
		
		reader.close();
		sortAllContigs();
	}
	
}
