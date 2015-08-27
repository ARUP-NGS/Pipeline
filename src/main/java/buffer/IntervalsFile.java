package buffer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Interval;
import util.coverage.HasIntervals;


/**
 * Any file that describes a list of genomic intervals. Usually a BED file.
 * Annoyingly, this can't subclass BasicIntervalContainer since it must also subclass FileBuffer
 * Ideally, this thing would wrap some implementation of BasicIntervalContainer and delegate
 * most methods to it, right now we're duplicating a lot of code. 
 * @author brendan
 *
 */
public abstract class IntervalsFile extends FileBuffer implements HasIntervals {

	protected Map<String, List<Interval>> intervals = null;
	protected final IntervalComparator intComp = new IntervalComparator();
	protected boolean wasMerged = false; //set true if any intervals were merged
	
	public IntervalsFile(File source) {
		super(source);
	}
	
	public IntervalsFile() {
		//Blank on purpose
	}
	
	/**
	 * Return collection of all contigs in the intervals
	 * @return
	 */
	public Collection<String> getContigs() {
		if (!isMapCreated()) {
			try {
				buildIntervalsMap();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return intervals.keySet();
	}
	
	/**
	 * Merge in all intervals in the given file
	 * @param aFile
	 */
	public void mergeIntervalsFrom(IntervalsFile aFile) {
		for(String contig : aFile.getContigs()) {
			List<Interval> ints = aFile.getIntervalsForContig(contig);
			addIntervals(contig, ints);
		}
	}
	
	/**
	 * Write all the intervals in BED form to the given stream
	 * @param stream
	 */
	public void toBED(PrintStream stream) {
		if (intervals == null)
			return;
		
		List<String> contigs = new ArrayList<String>();
		contigs.addAll( getContigs() );
		Collections.sort(contigs);
		
		for(String contig : contigs) {
			for(Interval inter : getIntervalsForContig(contig)) {
				stream.println(contig + "\t" + inter.begin + "\t" + inter.end);
			}
		}
		
		stream.flush();
	}
	
	/**
	 * Add a new list of intervals to the given contig. If contig doesn't exist it is created
	 * If contig does exist and contains intervals, given intervals are merged with old intervals
	 * @param contig
	 * @param newIntervals
	 */
	public void addIntervals(String contig, List<Interval> newIntervals) {
		if (intervals == null) {
			intervals = new HashMap<String, List<Interval>>();
		}
		
		if (! intervals.containsKey(contig)) {
			List<Interval> ints = new ArrayList<Interval>();
			ints.addAll(newIntervals);
			intervals.put(contig, ints);
		}
		else {
			List<Interval> oldInts = intervals.get(contig);
			oldInts.addAll(newIntervals);
			Collections.sort(oldInts);
			mergeIntervals(oldInts);
		}
	}
	
	/**
	 * Merges all mergeable intervals in the given list
	 * @param inters
	 */
	private void mergeIntervals(List<Interval> inters) {
		List<Interval> merged = new ArrayList<Interval>();
		if (inters.size() == 0)
			return;
		
		merged.add( inters.get(0));
		inters.remove(0);
		
		for(Interval inter : inters) {
			Interval last = merged.get( merged.size()-1);
			if (inter.intersects(last)) {
				Interval newLast = inter.merge(last);
				merged.remove(last);
				merged.add(newLast);
				this.wasMerged = true;
			}
			else {
				merged.add(inter);
			}
			
		}
		
		inters.clear();
		inters.addAll(merged);
	}
	
	/**
	 * Return sorted list of all intervals in the given contig
	 * @return
	 */
	public List<Interval> getIntervalsForContig(String contig) {
		return intervals.get(contig);
	}
	
	/**
	 * Read the source file and build a list of intervals in memory
	 * @throws IOException
	 */
	public abstract void buildIntervalsMap() throws IOException;
	
	
	/**
	 * Returns true if the intervals map has been created
	 * @return
	 */
	public boolean isMapCreated() {
		return intervals != null;
	}

	/**
	 * Returns a list of all intervals overlapping the given position
	 * @param contig
	 * @param pos
	 * @return
	 */
	public List<Interval> getOverlappingIntervals(String contig, int pos) {
		List<Interval> cInts = intervals.get(contig);
		List<Interval> intervals = new ArrayList<Interval>();
		
		if (cInts == null) {
			return intervals;
		}
		else {
			for (Interval inter : cInts) {
				if (inter.contains(pos)) {
					intervals.add(inter);
				}
			}
			
		}
		return intervals;
	}
	
	public boolean contains(String contig, int pos) {
		return contains(contig, pos, true);
	}
	
	public boolean contains(String contig, int pos, boolean warn) {
		List<Interval> cInts = intervals.get(contig);
		Interval qInterval = new Interval(pos, pos);
		if (cInts == null) {
			if (warn)
				System.out.println("Contig " + contig + " is not in BED file!");
			return false;
		}
		else {
			int index = Collections.binarySearch(cInts, qInterval, intComp);
			if (index >= 0) {
				//System.out.println("Interval " + cInts.get(index) + " contains the position " + pos);
				//An interval starts with the query position so we do contain the given pos
				return true;
			}
			else {
				//No interval starts with the query pos, but we 
				int keyIndex = -index-1 -1;
				if (keyIndex < 0) {
					//System.out.println("Interval #0 does NOT contain the position " + pos);
					return false;
				}
				Interval cInterval = cInts.get(keyIndex);
				if (pos >= cInterval.begin && pos < cInterval.end) {
					//System.out.println("Interval " + cInterval + " contains the position " + pos);
					return true;
				}
				else {
					//System.out.println("Interval " + cInterval + " does NOT contain the position " + pos);
					return false;
				}
			}
		}
	}

	public boolean intersects(String contig, Interval qInterval) {

		return intersects(contig, qInterval, true);
	}

	public boolean intersects(String contig, Interval qInterval, boolean warn) {
		List<Interval> cInts = intervals.get(contig);
		Interval qIntervalBegin = new Interval(qInterval.begin, qInterval.begin);
		Interval qIntervalEnd = new Interval(qInterval.end - 1, qInterval.end - 1);
		if (cInts == null) {
			if (warn)
				System.out.println("Contig " + contig + " is not in BED file!");
			return false;
		}
		else {
			int indexBegin = Collections.binarySearch(cInts, qIntervalBegin, intComp);
			int indexEnd = Collections.binarySearch(cInts, qIntervalEnd, intComp);
			if (indexBegin >= 0 || indexEnd >= 0) {
				//An interval starts with one the query interval begin or end
				return true;
			}
			else {
				//No interval starts with begin or end
				int keyIndexBegin = -indexBegin-1 -1;
				int keyIndexEnd = -indexEnd-1 -1;
				if (keyIndexBegin < 0 && keyIndexEnd < 0) {
					//System.out.println("Interval " + cInts.get(indexBegin) + " does not contain the interval " + qInterval.begin + ", " + qInterval.end);
					//System.out.println("False - keyIndexBegin<0 and keyIndexEnd<0");
					//System.out.println("Interval #0 does NOT contain the interval " + begin + ", " + end);
					return false;
				} 
				if (keyIndexBegin < keyIndexEnd) {
					//System.out.println("spans across the start of a bed region");
					//System.out.println("True - keyIndexBegin < keyIndexEnd");
					return true;
				}
				if (keyIndexBegin == keyIndexEnd) {
					//System.out.println("start and end are both directly downstream the same interval start");
					Interval cInterval = cInts.get(keyIndexBegin);
					if (qInterval.begin < cInterval.end) { // use < rather than <= because of 0-based intervals assumed (bed style)
						//System.out.println("True - the query interval starts before the binarySearch interval end");
						return true;
					} else {
						//System.out.println("False - the query interval starts after the binarySearch interval end");
						return false;
					}
				}
				//If nothing else stuck then something is potentially wrong with the intervals
				//System.out.println("Interval " + cInterval + " does NOT contain the position " + pos);
				throw new IllegalArgumentException("Intervals appear to be malformed for intersects method");
			}
		}
	}

	/**
	 * Returns an int array with two elements holding the indices of the first and last interval overlap with qInterval
	 * If no intersect, the {-1,-1} is returned
	 * @param contig
	 * @param qInterval
	 * @return
	 */
	public int[] intersectsWhich(String contig, Interval qInterval) {

		return intersectsWhich(contig, qInterval, true);
	}
	
	public int[] intersectsWhich(String contig, Interval qInterval, boolean warn) {
		List<Interval> cInts = intervals.get(contig);
		Interval qIntervalBegin = new Interval(qInterval.begin, qInterval.begin);
		Interval qIntervalEnd = new Interval(qInterval.end - 1, qInterval.end - 1);
		int indexBegin;
		int indexEnd;
		int[] intersects = new int[2];
		
		if (cInts == null) {
			if (warn)
				System.out.println("Contig " + contig + " is not in BED file!");
			//return array of nulls
			return intersects;
		}
		else {
			indexBegin = Collections.binarySearch(cInts, qIntervalBegin, intComp);
			indexEnd = Collections.binarySearch(cInts, qIntervalEnd, intComp);

			intersects = intersectsWhichCalc(cInts, qIntervalBegin, qIntervalEnd, indexBegin, indexEnd);
			return intersects;
		}
	}

	private int[] intersectsWhichCalc(List<Interval> cInts, Interval qIntervalBegin, Interval qIntervalEnd, int indexBegin, int indexEnd) {
		int[] intersects = {-1, -1}; //default -1 indicates no intervals intersect with query
		int keyIndexBegin = -indexBegin-1 -1;
		int keyIndexEnd = -indexEnd-1 -1;
		int firstIntersect; //first possible intersect
		int lastIntersect; //last possible intersect


		if (indexBegin >= 0) {
			//An interval starts with the query begin
			firstIntersect = indexBegin;
		} else if (keyIndexBegin < 0) {
			//query start before first interval
			firstIntersect = 0;
		} else if (qIntervalBegin.begin <= cInts.get(keyIndexBegin).end) {
			//query begin in one of the intervals
			firstIntersect = keyIndexBegin;
		} else {
			//query begin between intervals
			//first possible index will be the next interval to the right
			firstIntersect = keyIndexBegin + 1;
		}
			
		if (indexEnd >= 0) {
			//An interval starts with the query end
			lastIntersect = indexEnd;
		} else if (keyIndexEnd < 0) {
			//query end before first interval
			lastIntersect = -1; //no intersect possible
		} else if (qIntervalEnd.end <= cInts.get(keyIndexEnd).end) {
			//query end in one of the intervals
			lastIntersect = keyIndexEnd;
		} else {
			//query end between intervals
			//last possible index will be this interval (which begins to the left)
			lastIntersect = keyIndexEnd;
		}
		
		if (lastIntersect >= firstIntersect) {
			intersects[0] = firstIntersect;
			intersects[1] = lastIntersect;
		}
			
		return intersects; //may return [-1,-1] if no intersect found
	}


	/**
	 * Returns an int array list with a decending ranked indexes of nearest intervals results 
	 * If intersect between querry and intervals, intervals are listed from left to right position
	 * If no overlap, then the index of the closest interval is passed back in array position 0
	 * If query is between two intervals, then the more distant interval index is passed back in array position 1
	 * @param contig
	 * @param qInterval
	 * @return
	 */
	public ArrayList<Integer> nearest(String contig, Interval qInterval) {
		
		return nearest(contig, qInterval, true);
	}

	public ArrayList<Integer> nearest(String contig, Interval qInterval, boolean warn) {
		List<Interval> cInts = intervals.get(contig);
		Interval qIntervalBegin = new Interval(qInterval.begin, qInterval.begin);
		Interval qIntervalEnd = new Interval(qInterval.end, qInterval.end);
		int indexBegin;
		int indexEnd;
		ArrayList<Integer> nearest = new ArrayList<Integer>();

		
		if (cInts == null) {
			if (warn)
				System.out.println("Contig " + contig + " is not in BED file!");
			return nearest;
		}
		else {
			indexBegin = Collections.binarySearch(cInts, qIntervalBegin, intComp);
			indexEnd = Collections.binarySearch(cInts, qIntervalEnd, intComp);

			int[] intersects = intersectsWhichCalc(cInts, qIntervalBegin, qIntervalEnd, indexBegin, indexEnd);
			if (intersects[0] == -1) { 
				//query does not intersect any of the intervals (returns -1s)
				//we need to find nearest
				// in order of nearer then farther
				nearest = nearestCalc(contig, cInts, qIntervalBegin, qIntervalEnd, indexBegin, indexEnd);
				return nearest;
			} else { // make nearest list from all of the intersected intervals (from left to right)
				for (int i=0; i < intersects.length; i++) {
					if (!nearest.contains(intersects[i])) {
						nearest.add(intersects[i]);
					}
				}
				return nearest; 
			}
		}
	}
	
	private ArrayList<Integer> nearestCalc(String contig, List<Interval> cInts, Interval qIntervalBegin, Interval qIntervalEnd, int indexBegin, int indexEnd) {
		//WARNING: Assumes you already checked and there are no intersects!
		int keyIndexBegin = -indexBegin-1 -1;
		int keyIndexEnd = -indexEnd-1 -1;
		int beginDistance;
		int endDistance;
		ArrayList<Integer> nearest = new ArrayList<Integer>();
		
		//Look for special cases
		if (indexBegin >= 0) {
			//An interval starts with the query begin
			//beginIntersects = true!;
			throw new IllegalArgumentException("No intersections allowed for this method, but the query " + contig + ":" + 
					qIntervalBegin.begin + "-" + qIntervalEnd.end + " is at the start of an interval");
		}
		if (indexEnd >= 0) {
			//An interval starts with the query end
			//endIntersects = true!;
			throw new IllegalArgumentException("No intersections allowed for this method, but the query " + contig + ":" + 
					qIntervalBegin.begin + "-" + qIntervalEnd.end + " is at the end of an interval");
		}
		if (keyIndexBegin < 0 || keyIndexEnd < 0) {
			//query before first interval (assumes no intersect)
			//just set nearest to first interval
			nearest.add(0);
			return nearest;
		}
		if (keyIndexBegin >= (cInts.size() - 1) || keyIndexEnd >= (cInts.size() - 1)) {
			//query begin or query end after last interval (assumes no intersect)
			//just set nearest to last interval
			nearest.add(cInts.size() - 1);
			return nearest;
		}


		//Now assume that query must be between two intervals (assumes no intersect)
		Interval intervalToLeft = cInts.get(keyIndexBegin);
		beginDistance = qIntervalBegin.begin - intervalToLeft.end;
		if (beginDistance < 0) {
			//query must overlap an interval
			throw new IllegalArgumentException("No intersections allowed for this method, but the query " + contig + ":" + 
					qIntervalBegin.begin + "-" + qIntervalEnd.end + " begin appears to overlap with the end of an interval " +
					contig + ":" + cInts.get(keyIndexBegin).begin + "-" + cInts.get(keyIndexBegin).end);
		}
		Interval intervalToRight = cInts.get(keyIndexEnd + 1);
		endDistance = intervalToRight.begin - qIntervalEnd.end;
		if (endDistance < 0) {
			//query must intersect an interval
			throw new IllegalArgumentException("No intersections allowed for this method, but the query " + contig + ":" + 
					qIntervalBegin.begin + "-" + qIntervalEnd.end + " end appears to overlap with the begin of an interval " +
					contig + ":" + cInts.get(keyIndexBegin).begin + "-" + cInts.get(keyIndexBegin).end);
		}

		if (beginDistance <= endDistance) {
			nearest.add(keyIndexBegin);
			nearest.add(keyIndexEnd + 1);
			return nearest;
		} else {
			nearest.add(keyIndexEnd + 1);
			nearest.add(keyIndexBegin);
			return nearest;
		}
	}

	
	/**
	 * Returns the number of bases covered by all of the intervals
	 * @return
	 */
	public int getExtent() {
		int size = 0;
		if (! isMapCreated()) {
			try {
				buildIntervalsMap();
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
		}
		
		if (intervals == null) {
			return 0;
		}
		
		for(String contig : intervals.keySet()) {
			List<Interval> intList = intervals.get(contig);
			for(Interval interval : intList) {
				size += interval.end - interval.begin;
			}
		}
		return size;
	}
	
	/**
	 * Returns the number of intervals in this interval collections
	 * @return
	 */
	public int getIntervalCount() {
		if (! isMapCreated()) {
			try {
				buildIntervalsMap();
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
		}
		
		if (intervals == null) {
			return 0;
		}
		
		int size = 0;
		for(String contig : intervals.keySet()) {
			List<Interval> intList = intervals.get(contig);
			size += intList.size();
		}
		return size;
	}
	
	
	
	public class IntervalComparator implements Comparator<Interval> {

		@Override
		public int compare(Interval o1, Interval o2) {
			return o1.begin - o2.begin;
		}

		
	}
	
}
