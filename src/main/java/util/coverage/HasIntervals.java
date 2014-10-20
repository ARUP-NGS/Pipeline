package util.coverage;

import java.util.Collection;
import java.util.List;

import util.Interval;

//Generic interface for anything that provides access to intervals
public interface HasIntervals {

	/**
	 * The collection of contigs to iterate over to get the Intervals
	 * @return
	 */
	public Collection<String> getContigs();
	
	/**
	 * A list of intervals in the given contig
	 * @param chr
	 * @return
	 */
	public List<Interval>  getIntervalsForContig(String chr);
	
}
