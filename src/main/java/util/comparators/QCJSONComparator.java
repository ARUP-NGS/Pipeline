package util.comparators;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import util.ReviewDirTool;
import util.comparators.CompareReviewDirs.ComparisonType;
import util.reviewDir.ReviewDirectory;

/** Compares two qc.json files from two different review directories.
 * 
 * @author Kevin
 *
 */
class QCJSONComparator extends Comparator {

	public QCJSONComparator(ReviewDirectory rd1, ReviewDirectory rd2, String analysisHeader) {
		super(rd1, rd2, analysisHeader);
		this.summaryTable.setColNames(Arrays.asList("TRUTH", "TEST", "Notes"));
	}

	@Override
	public void performComparison() throws IOException, JSONException {
		/* 
		 * This is my assumption for what the json object will contain. I dont know how robust it is...
		 * Key #1: capture.extent
		 * Key #2: final.coverage.metrics
		 * Key #3: raw.coverage.metrics
		 * Key #4: raw.bam.metrics
		 * Key #5: variant.metrics 
		 * Key #6: final.bam.metrics
		 * Key #7: nocalls
		 */

		//Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		JSONObject json1 = ReviewDirTool.toJSONObj(rd1.getSampleManifest().getQCJSON().getAbsolutePath());
		JSONObject json2 = ReviewDirTool.toJSONObj(rd2.getSampleManifest().getQCJSON().getAbsolutePath());
		
		JSONObject coverage1 = new JSONObject(json1.get("final.coverage.metrics").toString());
		JSONObject coverage2 = new JSONObject(json2.get("final.coverage.metrics").toString());
		this.compareCoverageMetrics(coverage1, coverage2);

		JSONObject Bam1 = new JSONObject(json1.get("final.bam.metrics").toString());
		JSONObject Bam2 = new JSONObject(json2.get("final.bam.metrics").toString());
		this.compareBAMMetrics(Bam1, Bam2);
		
		JSONObject nocalls1 = new JSONObject(json1.get("nocalls").toString());
		JSONObject nocalls2 = new JSONObject(json2.get("nocalls").toString());
		this.compareNoCalls(nocalls1, nocalls2);

		JSONObject varmetrics1 = new JSONObject(json1.get("variant.metrics").toString());
		JSONObject varmetrics2 = new JSONObject(json2.get("variant.metrics").toString());
		this.compareVariantMetrics(varmetrics1, varmetrics2);
		
	}
	
	private void compareBAMMetrics(JSONObject bam1, JSONObject  bam2) {
		try {
			double basesAboveQ10_1 = bam1.getDouble("bases.above.q10")/bam1.getDouble("bases.read");
			double basesAboveQ10_2 = bam2.getDouble("bases.above.q10")/bam2.getDouble("bases.read");
			
			double basesAboveQ20_1 = bam1.getDouble("bases.above.q20");
			double basesAboveQ20_2 = bam2.getDouble("bases.above.q20");
			
			double basesAboveQ30_1 = bam1.getDouble("bases.above.q30");
			double basesAboveQ30_2 = bam2.getDouble("bases.above.q30");
			
			//mean.insert.size
			double meanInsertSize_1 = bam1.getDouble("mean.insert.size");
			double meanInsertSize_2 = bam2.getDouble("mean.insert.size");
			
			this.addNewEntry("fraction.bases.above.q10", "Fraction bases above Q10", String.format("%.1f", basesAboveQ10_1), String.format("%.1f", basesAboveQ10_2), ComparisonType.TWONUMBERS);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void compareCoverageMetrics(JSONObject coverage1, JSONObject  coverage2) throws JSONException {
		try {
			double mean1 = coverage1.getDouble("mean.coverage");
			double mean2 = coverage2.getDouble("mean.coverage");
			this.addNewEntry("mean.coverage", "Mean coverage", String.format("%.1f", mean1), String.format("%.1f", mean2), ComparisonType.TWONUMBERS);

			JSONArray cutoffs1 = processCoverage(coverage1.getJSONArray("fraction.above.cov.cutoff"));
			JSONArray cutoffs2 = processCoverage(coverage2.getJSONArray("fraction.above.cov.cutoff"));
			
			//Map quality cutoff to index of that quality cutoff. i.e 10x, 50x, 200x, etc..
			Map<String, Integer> cutOffMap1 = getCutoffIndexes(coverage1);
			Map<String, Integer> cutOffMap2 = getCutoffIndexes(coverage2);
			
			for (Map.Entry<String, Integer> entry : cutOffMap1.entrySet()) {
				
				if (entry.getValue() != -1 && cutOffMap2.get(entry.getKey()) != -1 ) {
					
					Double json1facAbove = cutoffs1.getDouble(entry.getValue()); 			
					Double json2facAbove = cutoffs2.getDouble(cutOffMap2.get(entry.getKey()));
					
					this.addNewEntry("fraction.greater." + entry.getKey(),  "Fraction of bases > " + entry.getKey() + "X coverage", String.valueOf(json1facAbove), String.valueOf(json2facAbove), ComparisonType.TWONUMBERS);
				} else {	
					this.addNewEntry("fraction.greater." + entry.getKey(), "Fraction of bases > " + entry.getKey() + "X coverage", "NA", "NA", ComparisonType.NONE);
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void compareNoCalls(JSONObject nocalls1, JSONObject nocalls2) {		
		try {

			Double nocallInterval1 = nocalls1.getDouble("interval.count");
			Double nocallInterval2 = nocalls2.getDouble("interval.count");
			this.addNewEntry("no.call.regions", "Number of no-call regions", String.valueOf(nocallInterval1), String.valueOf(nocallInterval2), ComparisonType.TWONUMBERS);


			Double nocallExtent1 = nocalls1.getDouble("no.call.extent");
			Double nocallExtent2 = nocalls2.getDouble("no.call.extent");
			this.addNewEntry("no.call.extent", "Extent (total size) of no-call regions", String.valueOf(nocallExtent1), String.valueOf(nocallExtent2), ComparisonType.TWONUMBERS);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** Not implemented now, but could be in the future.
	 * @param varmetrics1
	 * @param varmetrics2
	 */
	private void compareVariantMetrics(JSONObject varmetrics1, JSONObject varmetrics2) {
		
	/*	//Variant rundown
		if (obj.has("variant.metrics") ) {
			output.println("\n\t Variant summary:");
			JSONObject varMetrics = obj.getJSONObject("variant.metrics");
			output.println("Total variants: " + safeInt(varMetrics, "total.vars"));
			output.println("Total snps: " + safeInt(varMetrics, "total.snps"));
			output.println("Overall TT: " + safeDouble(varMetrics, "total.tt.ratio"));
			output.println("Known / Novel TT: " + safeDouble(varMetrics, "known.tt") + " / " + safeDouble(varMetrics, "novel.tt"));
			Double totVars = varMetrics.getDouble("total.vars");
			Double knowns = varMetrics.getDouble("total.known");
			if (totVars > 0) {
				Double novelFrac = 1.0 - knowns / totVars;
				output.println("Fraction novel: " + formatter.format(novelFrac));
			}
			//output.println("Total novels: " + safeDouble(varMetrics, " "));

			if (obj.has("capture.extent")) {
				long extent = obj.getLong("capture.extent");
				double varsPerBaseCalled = totVars / (double)extent;
				output.println("Vars per base called: " + smallFormatter.format(varsPerBaseCalled));
			}
			else {
				output.println("Vars per base called: No capture.extent available");
			}

		}
		else {
			output.println("No variant metrics found");
		}
		
		try {
			String totVars1 = String.valueOf(varmetrics1.getInt("total.vars"));
			String totVars2 = String.valueOf(varmetrics2.getInt("total.vars"));
			this.addNewEntry("Number of variants", totVars1, totVars2, createNotes(totVars1, totVars2));
		} catch (JSONException e) {
			e.printStackTrace();
		}*/
	}

	private int findIndex(JSONArray arr, int val) {
		for (int i = 0 ; i < arr.length(); i++) {
			try {
				if (val==arr.getInt(i)) {
					return i;
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return -1; //We didnt find it.
	}

	/** This function attempts to find certain cutoff values (10,50,500) that we will use to compare two runs.
	 * @param coverageJSON
	 * @return
	 * @throws JSONException
	 */
	private Map<String, Integer> getCutoffIndexes(JSONObject coverageJSON) throws JSONException {

		Map<String, Integer> myMap = new LinkedHashMap<String, Integer>();

		JSONArray cutoffValues = coverageJSON.getJSONArray("coverage.cutoffs");

		myMap.put("10", this.findIndex(cutoffValues, 10)); //returns -1 if not found..
		myMap.put("50", this.findIndex(cutoffValues, 50));
		myMap.put("200", this.findIndex(cutoffValues, 200));
		myMap.put("500", this.findIndex(cutoffValues, 500));
		myMap.put("1000", this.findIndex(cutoffValues, 1000));

		return myMap;
	}
	
	/** Convert values in a JSONArray from fractions to percentages. This function is intented to deal with multiple representations of coverage 
	 * cutoffs for pipeline runs. Some are in percentages and some are in fractions, and this normalizes to percentages.
	 * will convert
	 * @param num
	 * @return
	 */
	private JSONArray processCoverage(JSONArray json) {
		//Loop through the json and convert to percentage if it isnt already..
		for(int i=0; i < json.length(); i++) {
			Double curItem;
			try {
				curItem = json.getDouble(i);
				if (curItem.toString().startsWith("0.") || curItem.toString().equals("1.0")) {
					curItem = curItem * 100.0;
					json.put(i, String.format("%.1f", curItem));
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return json;
	}
}