package buffer;

import java.util.List;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import json.JSONString;

import org.w3c.dom.NodeList;

/**
 * A small container class for some Depth Of Coverage metrics computed by the GaTK
 * @author brendan
 *
 */
public class DOCMetrics extends FileBuffer implements JSONString {

	protected String sourceFile = null;
	protected double meanCoverage = -1;
	protected int[] cutoffs;
	protected double[] fractionAboveCutoff;
	protected List<FlaggedInterval> flaggedIntervals = null;
	protected double[] coverageProportions = null; //When non-null should be proportion of reads with coverage greater than index
	
	public DOCMetrics() {
	}
	
	@Override
	public String toJSONString() {
		JSONObject obj = toJSONObject();
		if (obj == null) {
			return null;
		}
		else {
			return obj.toString();
		}
	}
	
	public JSONObject toJSONObject() {
		JSONObject obj = null;
		try {
			obj = new JSONObject();
			obj.put("mean.coverage", meanCoverage);
			obj.put("coverage.cutoffs", cutoffs);
			obj.put("fraction.above.cov.cutoff", toPrecision(fractionAboveCutoff, 4));
			if (coverageProportions != null)
				obj.put("fraction.above.index", toPrecision(coverageProportions, 4));
			if (flaggedIntervals != null) {
				obj.put("lowcov.exons", flaggedIntervalsToJSON());
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return obj;
	}
	
	private JSONArray flaggedIntervalsToJSON() {
		JSONArray arr = new JSONArray();
		for(FlaggedInterval fi : flaggedIntervals) {
			try {
				arr.put( flaggedIntervalToJSON(fi) );
			} catch (JSONException e) {
				System.err.println("Exception parsing flagged interval " + fi.chr + " : " + fi.start + "-" + fi.end);
			}
		}
		return arr;
	}
	
	private JSONObject flaggedIntervalToJSON(FlaggedInterval fi) throws JSONException {
		JSONObject obj = new JSONObject();
		
		obj.put("chr", fi.chr);
		obj.put("start", fi.start);
		obj.put("end", fi.end);
		obj.put("meancov", fi.mean);
		obj.put("feature", fi.info);
		
		return obj;
	}

	public double[] getCoverageProportions() {
		return coverageProportions;
	}

	public static double[] toPrecision(double[] vals, int precision) {
		for(int i=0; i<vals.length; i++) {
			vals[i] = toPrecision(vals[i], precision);
		}
		return vals;
	}
	
	public static double toPrecision(double val, int precision) {
		double mul = Math.pow(10.0, precision);
		return Math.round(val*mul)/mul;
	}
	
	
	public String getSourceFile() {
		return sourceFile;
	}

	public List<FlaggedInterval> getFlaggedIntervals() {
		return flaggedIntervals;
	}

	public void setFlaggedIntervals(List<FlaggedInterval> flaggedIntervals) {
		this.flaggedIntervals = flaggedIntervals;
	}
	
	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}

	public double getMeanCoverage() {
		return meanCoverage;
	}

	public void setMeanCoverage(double meanCoverage) {
		this.meanCoverage = meanCoverage;
	}

	public int[] getCutoffs() {
		return cutoffs;
	}

	public void setCutoffs(int[] cutoffs) {
		this.cutoffs = cutoffs;
	}

	public double[] getFractionAboveCutoff() {
		return fractionAboveCutoff;
	}

	public void setFractionAboveCutoff(double[] fractionAboveCutoff) {
		this.fractionAboveCutoff = fractionAboveCutoff;
	}
	
	public void setCoverageProportions(double[] prop) {
		coverageProportions = prop;
	}
	
	
	@Override
	public void initialize(NodeList children) {
		// Nothing to do
	}

	@Override
	public String getTypeStr() {
		return "DOCMetrics";
	}

	public String toString() {
		return "DOC metrics for " + sourceFile + " : " + getMeanCoverage(); // + " coverage at " + cutoffs[0] + " : " + fractionAboveCutoff[0];
	}
	
	

	public static class FlaggedInterval {
		public String info = null;
		public double mean = 0;
		public double frac = 0;
		public String chr;
		public int start;
		public int end;
	}
	
}
