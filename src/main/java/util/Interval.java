package util;

/**
 * An immutable, discrete range with half-open boundaries
 * @author brendan
 *
 */
public class Interval implements Comparable<Interval> {
	
	public final int begin;
	public final int end;
	private Object info = null; //Optional information associated with this interval. 
	
	public Interval(int begin, int end, Object info) {
		this.begin = begin;
		this.end = end;
		this.info = info;
	}
	
	public Interval(int begin, int end) {
		this.begin = begin;
		this.end = end;
	}

	/**
	 * Return optional object attached to this interval
	 * @return
	 */
	public Object getInfo() {
		return info;
	}
	
	/**
	 * Returns true if any site falls into both this and the other interval
	 * @param other
	 * @return
	 */
	public boolean intersects(Interval other) {
		return intersects(other.begin, other.end);
	}
	
	/**
	 * Merge two overlapping intervals into a single interval that includes all sites in both
	 * @param other
	 * @return
	 */
	public Interval merge(Interval other) {
		if (! this.intersects(other)) {
			throw new IllegalArgumentException("Intervals must overlap to merge");
		}
		
		return new Interval(Math.min(begin, other.begin), Math.max(end, other.end));
	}
	
	/**
	 * True if this pos >= begin and pos < end. 
	 * @param pos
	 * @return
	 */
	public boolean contains(int pos) {
		return pos >= begin && pos < end;
	}
	
	/**
	 * True if the given range shares any sites with this Interval
	 * @param start
	 * @param end
	 * @return
	 */
	public boolean intersects(int bStart, int bEnd) {
		if (bEnd <= begin ||
				bStart >= end)
			return false;
		else
			return true;
	}
	
	@Override
	public int compareTo(Interval inter) {
			return this.begin - inter.begin;
	}
	
	public String toString() {
		return "[" + begin + "-" + end + "]";
	}
}
