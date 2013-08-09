package buffer;

import json.JSONException;
import json.JSONObject;
import json.JSONString;
import math.Histogram;

/**
 * Basically just storage for a bunch of summary info about a BAM file
 * @author brendan
 *
 */
public class BAMMetrics extends FileBuffer implements JSONString {

	public String path;
	public int totalReads;
	public Histogram mqHistogram;
	public Histogram insertSizeHistogram;
	public Histogram baseQualityHistogram;
	public Histogram[] readPosQualHistos;
	public int unmappedReads;
	public int unmappedMates;
	public int duplicateReads;
	public int lowVendorQualityReads;
	public int hugeInsertSize;
	public long basesRead;
	public long basesQAbove30;
	public long basesQAbove20;
	public long basesQAbove10;
	
	@Override
	public String getTypeStr() {
		return "BAM Metrics";
	}

	@Override
	public String toJSONString()  {
		JSONObject obj = null;
		try {
			obj = new JSONObject();
			obj.put("total.reads", totalReads);
			obj.put("unmapped.reads", unmappedReads);
			obj.put("bases.read", basesRead);
			obj.put("bases.above.q30", basesQAbove30);
			obj.put("bases.above.q20", basesQAbove20);
			obj.put("bases.above.q10", basesQAbove10);
			obj.put("mq.histo",  mqHistogram.getRawCounts());
			if (insertSizeHistogram != null)
				obj.put("mean.insert.size", insertSizeHistogram.getMean());
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return obj.toString();
	}
	

}
