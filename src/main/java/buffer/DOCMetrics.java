package buffer;

import java.util.List;

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
		JSONObject obj = null;
		try {
			obj = new JSONObject();
			obj.put("mean.coverage", meanCoverage);
			obj.put("coverage.cutoffs", cutoffs);
			obj.put("fraction.above.cov.cutoff", fractionAboveCutoff);
			if (coverageProportions != null)
				obj.put("fraction.above.index", coverageProportions);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (obj == null)
			return null;
		else 
			return obj.toString();
	}
	

	public double[] getCoverageProportions() {
		return coverageProportions;
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
	}
	
}
