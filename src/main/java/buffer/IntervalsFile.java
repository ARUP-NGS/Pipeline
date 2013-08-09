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

/**
 * Any file that describes a list of genomic intervals
 * @author brendan
 *
 */
public abstract class IntervalsFile extends FileBuffer {

	protected Map<String, List<Interval>> intervals = null;
	protected final IntervalComparator intComp = new IntervalComparator();
	
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
