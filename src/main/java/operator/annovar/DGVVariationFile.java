package operator.annovar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import pipeline.Pipeline;
import util.Interval;
import buffer.IntervalsFile;

/**
 * Provides access to the information in a 'variation' file from the DGV, which catalogues large chromosomal 
 * variants in healthly individuals
 * @author brendan
 *
 */
public class DGVVariationFile extends IntervalsFile {

	@Override
	public void buildIntervalsMap() throws IOException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Building intervals map for BED file " + getFilename());
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
				String contig = toks[2];
				contig = contig.replace("chr", "");
				Integer begin = Integer.parseInt(toks[3]);
				Integer end = Integer.parseInt(toks[4]);
				String gains = toks[12];
				String losses = toks[13];
				if (gains.trim().length()==0)
					gains = "?";
				if (losses.trim().length()==0)
					losses = "?";
				String id = toks[0];
				String infoStr = "ID=" + id + ",gains=" + gains + ",losses=" + losses;
				Interval interval = new Interval(begin, end, infoStr);

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
		
	//	sortAllContigs();
		
		logger.info("Done building intervals map for " + getFilename());
	}

	@Override
	public String getTypeStr() {
		// TODO Auto-generated method stub
		return null;
	}



}
