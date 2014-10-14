package util.Comparators;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import buffer.FileBuffer;
import buffer.JSONBuffer;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import util.QCJsonReader;
import util.CompressGZIP;

/*
 * I think I should make a file comparison superclass which then gets implemented or extended for these, since I am sharing a lot of code.
 */

public class CompareQCMetrics extends IOOperator {

	static DecimalFormat formatter = new DecimalFormat("0.0##");
	static DecimalFormat smallFormatter = new DecimalFormat("0.00000");

	public static LinkedHashMap jsonToMap(JSONObject json) throws JSONException {
		LinkedHashMap<String, Object> retMap = new LinkedHashMap<String, Object>();

		if (json != JSONObject.NULL) {
			retMap = toMap(json);
		}
		return retMap;
	}

	public static LinkedHashMap toMap(JSONObject object) throws JSONException {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

		Iterator<String> keysItr = object.keys();
		while (keysItr.hasNext()) {
			String key = keysItr.next();
			Object value = object.get(key);

			if (value instanceof JSONArray) {
				value = toList((JSONArray) value);
			}

			else if (value instanceof JSONObject) {
				value = toMap((JSONObject) value);
			}
			map.put(key, value);
		}
		return map;
	}

	public static List toList(JSONArray array) throws JSONException {
		List<Object> list = new ArrayList<Object>();
		for (int i = 0; i < array.length(); i++) {
			list.add(array.get(i));
		}
		return list;
	}

	static LinkedHashMap<String, Object> JSONCompare(String jsonFile1,
			String jsonFile2) throws JSONException, IOException {
		/*
		 * JSON2 Key #1: capture.extent JSON2 Key #2: final.coverage.metrics
		 * JSON2 Key #3: capture.bed JSON2 Key #4: raw.coverage.metrics JSON2
		 * Key #5: raw.bam.metrics JSON2 Key #6: variant.metrics JSON2 Key #7:
		 * final.bam.metrics JSON2 Key #8: nocalls
		 */

		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		System.out.println("JSON Filename 1 is " + jsonFile1);
		System.out.println("JSON Filename 2 is " + jsonFile2);
		JSONObject JSON1 = QCJsonReader.toJSONObj(jsonFile1);
		JSONObject JSON2 = QCJsonReader.toJSONObj(jsonFile2);

		LinkedHashMap<String, Object> JSON1HM = toMap(JSON1);
		LinkedHashMap<String, Object> JSON2HM = toMap(JSON2);
		String[] JSON1Keys = JSON1HM.keySet().toArray(
				new String[JSON1HM.size()]);
		String[] JSON2Keys = JSON1HM.keySet().toArray(
				new String[JSON2HM.size()]);
		int i = 1;
		for (String key : JSON1Keys) {
			System.out.println("JSON1 Key #" + i + ": " + key);
			// System.out.println("JSON1 Value #" + i + ": " +
			// JSON1HM.get(key));
			i += 1;
		}
		i = 1;
		for (String key : JSON2Keys) {
			System.out.println("JSON2 Key #" + i + ": " + key);
			// System.out.println("JSON2 Value #" + i + ": " +
			// JSON2HM.get(key));
			i += 1;
		}
		LinkedHashMap<String, Object> QC_ComparisonLHM = new LinkedHashMap<String, Object>();
		QC_ComparisonLHM.put("JSON1.qc", JSON1);
		QC_ComparisonLHM.put("JSON2.qc", JSON2);
		return QC_ComparisonLHM;
	}

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {

		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		List<FileBuffer> JSONs = this
				.getAllInputBuffersForClass(JSONBuffer.class);
		if (JSONs.size() != 2) {
			throw new OperationFailedException(
					"Exactly two JSONs required for this operator.", this);
		}
		String JSON1 = JSONs.get(0).getAbsolutePath();
		String JSON2 = JSONs.get(1).getAbsolutePath();

		LinkedHashMap<String, Object> Results = JSONCompNew_configuration (2)are(JSON1, JSON2);

		JSONObject ResultsJson = new JSONObject(Results);
		String ResultsStr = ResultsJson.toString();

		byte[] bytes = CompressGZIP.compressGZIP(ResultsStr);

		// Write compresssed JSON to file
		File dest = this.getOutputBufferForClass(JSONBuffer.class).getFile();
		BufferedOutputStream writer = new BufferedOutputStream(
				new FileOutputStream(dest));
		writer.write(bytes);
		writer.close();

	}

}