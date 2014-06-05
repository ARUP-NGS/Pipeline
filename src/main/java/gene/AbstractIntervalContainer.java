package gene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Interval;

/**
 * A list of intervals, grouped by contig. This is (or should be) the basic class
 * for looking up information from intervals for a particular range or site.  
 * @author brendan
 *
 */
public abstract class AbstractIntervalContainer {

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
	 * Add a new interval to the exonMap, creating a new contig - and a new map - if necessary. 
	 * @param contig
	 * @param start
	 * @param end
	 * @param obj
	 */
	protected void addInterval(String contig, int start, int end, Object obj) {
		if (allIntervals == null) {
			allIntervals = new HashMap<String, List<Interval>>();
		}
		
		List<Interval> intervals = allIntervals.get(contig);
		if (intervals == null) {
			intervals = new ArrayList<Interval>(1024);
			allIntervals.put(contig, intervals);
		}
		
		intervals.add(new Interval(start, end, obj));
	}
}
