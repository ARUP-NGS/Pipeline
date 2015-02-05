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
 * @author brendan
 *
 */
public class BasicIntervalContainer implements HasIntervals {

	protected Map<String, List<Interval>> allIntervals = null;
	
	
	
	public List<Interval> getIntervalsForSite(String contig, int start) {
		return getIntervalsForRange(contig, start, start+1);
	}
	
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
	 * Create a new interval object and add it to the exonMap, creating a new contig - and a new map - if necessary. 
	 * @param contig
	 * @param start
	 * @param end
	 * @param obj
	 */
	public void addInterval(String contig, int start, int end, Object obj) {
		addInterval(contig, new Interval(start, end, obj));
	}

	/**
	 * Add the given interval 
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
	public Collection<String> getContigs() {
		return allIntervals.keySet();
	}

	@Override
	public List<Interval> getIntervalsForContig(String chr) {
		return allIntervals.get(chr);
	}
}
