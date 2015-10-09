package util;

import json.JSONException;
import json.JSONObject;
import json.JSONString;

/**
 * An immutable, discrete range with half-open boundaries
 * @author brendan
 *
 */
public class Interval implements Comparable<Interval>, JSONString {
	
	public final int begin;
	public final int end;
	private Object info = null; //Optional information associated with this interval. 
	//public String[] trans = null; //Optional attribute added for ArupBEDFile class
										//which has transcript(s) with each interval
	
//	public Interval(int begin, int end, Object info, String[] trans) {
//		this.begin = begin;
//		this.end = end;
//		this.info = info;
//		this.trans = trans;
//	}

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
	 * Set optional object attached to this interval
	 * @return
	 */
	public void setInfo(Object info) {
		this.info = info;
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
	
	public JSONObject toJSON() throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("begin", this.begin);
		jo.put("end", this.end);
		return jo;
	}
	
	/**
	 * Return a new Interval object parsed from the given JSON obj. Expects 
	 * integer-valued 'begin' and 'end' fields
	 * @param jo
	 * @return
	 * @throws JSONException
	 */
	public static Interval fromJSON(JSONObject jo) throws JSONException {
		Integer begin = jo.getInt("begin");
		Integer end = jo.getInt("end");
		return new Interval(begin, end);
	}

	@Override
	public String toJSONString() {
		try {
			return this.toJSON().toString();
		} catch (JSONException e) {
			return "{\'error\':\'" + e.getLocalizedMessage() + "\'}";
		}
	}
}
