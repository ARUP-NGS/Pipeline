package gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Interval;
import util.coverage.HasIntervals;

/**
 * A list of intervals, grouped by contig. This is (or should be) the basic class
 * for looking up information from intervals for a particular range or site.  
 * 
 * Might be fun to subclass this to use an Interval Tree or some other sort of index
 * to allow faster lookups. But so far performance hasn't been an issue so we don't really 
 * worry about it. 
 * @author brendan
 *
 */
public class BasicIntervalContainer implements HasIntervals {

	//A map from contig name to list of intervals
	protected Map<String, List<Interval>> allIntervals = null;
	
	
	/**
	 * Get all intervals that overlap the given position on the given chromosome.
	 * Returns an empty (non-null) list if no intersecting intervals are found.
	 * @param contig
	 * @param start
	 * @return
	 */
	public List<Interval> getIntervalsForSite(String contig, int start) {
		return getIntervalsForRange(contig, start, start+1);
	}
	
	/**
	 * Get all intervals that overlap the given region. This just iterates over all intervals
	 * and returns those that intersect. 
	 * Returns an empty (non-null) list if no intersecting intervals are found 
	 * @param contig
	 * @param start
	 * @param end
	 * @return
	 */
	public List<Interval> getIntervalsForRange(String contig, int start, int end) {
		List<Interval> intervals = allIntervals.get(contig);
		List<Interval> returnedIntervals = new ArrayList<Interval>(4);
		
		if (intervals == null) {
			return returnedIntervals;
		}
		
		//Find all intervals that contain pos and store their info in a list
		//Right now this stupidly scans every interval, a modified sort of binary search
		//or an index of some kind would be a lot faster
		for(Interval inter : intervals) {
			if (inter.intersects(start, end)) {
				returnedIntervals.add( inter);
			}
		}
		
		return returnedIntervals;
	}
	
	/**
	 * Intervals can have arbitrary Objects stored with them - this method returns all the Objects
	 * stored for each interval that overlaps the given range. 
	 * @param contig
	 * @param start
	 * @param end
	 * @return
	 */
	public Object[] getIntervalObjectsForRange(String contig, int start, int end) {
		List<Object> objs = new ArrayList<Object>();
		for(Interval interval : getIntervalsForRange(contig, start, end)) {
			if (interval.getInfo() != null) {
				objs.add(interval.getInfo());
			}
		}
		
		return objs.toArray();
	}
	
	/**
	 * Create a new interval object and add it to this container, creating a new 
	 * contig - and a new map - if necessary. 
	 * @param contig Chromosome name of interval
	 * @param start Interval start position
	 * @param end Interval end position (half-open intervals assumed, should be first base NOT in interval)
	 * @param obj Arbitrary object to store with the interval (may be null)
	 */
	public void addInterval(String contig, int start, int end, Object obj) {
		addInterval(contig, new Interval(start, end, obj));
	}

	/**
	 * Add the given interval object to this container.  
	 * @param interval
	 */
	protected void addInterval(String contig, Interval interval) {
		if (allIntervals == null) {
			allIntervals = new HashMap<String, List<Interval>>();
		}
		
		List<Interval> intervals = allIntervals.get(contig);
		if (intervals == null) {
			intervals = new ArrayList<Interval>(1024);
			allIntervals.put(contig, intervals);
		}
		
		intervals.add(interval);
	}
	
	@Override
	/**
	 * Return all chromosome names / contigs in this container
	 */
	public Collection<String> getContigs() {
		return allIntervals.keySet();
	}

	@Override
	/**
	 * Returns all intervals found in given contig / chromosome name
	 */
	public List<Interval> getIntervalsForContig(String chr) {
		return allIntervals.get(chr);
	}
}
