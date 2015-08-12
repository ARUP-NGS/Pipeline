package buffer.variant;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import pipeline.Pipeline;
import util.vcfParser.VCFParser.GTType;

/**
 * Reads a JSON file (specifically annotated.json.gz) into a buffer.variant.VariantPool object.
 * 
 * @author KevinBoehme
 * 
 */

public class JSONtoVariantPool {


	/**
	 * Obtain a buffer.variant.VariantPool representation of a given annotated JSON file (as specified by the path)
	 * @param pathToJSONFile
	 * @return VariantPool
	 * @throws JSONException 
	 * @throws IOException 
	 */
	public static VariantPool toVariantPool(String pathToJSONFile) throws JSONException, IOException {
		GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(pathToJSONFile));
		BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

		StringBuilder JSONFilecontents = new StringBuilder();
		String line = br.readLine();
		while(line != null) {
			JSONFilecontents.append(line);
			line = br.readLine();
		}
		br.close();

		JSONObject annotatedJSON = new JSONObject(JSONFilecontents.toString());
		JSONArray annotatedVariantList = annotatedJSON.getJSONArray("variant.list");

		ArrayList<VariantRec> varList = new ArrayList<VariantRec>();

		for(int i = 0 ; i < annotatedVariantList.length() ; i++){
			//Convert each JSON variant into a VariantRec and collect them in a list
			varList.add(toVariantRec(annotatedVariantList.getJSONObject(i)));
		}

		return new VariantPool(varList);
	}

	/**
	 * 
	 * Obtain a VariantRec representation of a given annotated JSON variant
	 * 
	 * @param annotatedJSON
	 * @return VariantRec
	 * @throws IOException 
	 * @throws JSONException 
	 */
	private static VariantRec toVariantRec(JSONObject jsonObject) throws JSONException, IOException {

		String contig = (String) jsonObject.opt("chr");
		int start = jsonObject.optInt("pos");
		String ref = (String) jsonObject.opt("ref");
		String alt = (String) jsonObject.opt("alt");
		
		Double quality = -1.0;
		Object qualityObj = jsonObject.opt("quality");
		if (qualityObj != null) {
			quality = Double.parseDouble( qualityObj.toString() );
		}
		String zygStr = (String) jsonObject.opt("zygosity");
		GTType gt = GTType.UNKNOWN;
		if (zygStr.equals("1/1")) {
			gt =GTType.HOM;
		}
		if (zygStr.equals("0/1") || zygStr.equals("1/0")) {
			gt =GTType.HET;
		}
		
		VariantRec varObj = new VariantRec(contig, start, -1, ref, alt, quality, gt);

		//Loop through the Variant Rec entry and put each key value into the VariantRec object as either a property (Double) or an annotation (String)
		for(int i = 0; i < jsonObject.names().length(); i++){
			String key = jsonObject.names().getString(i);
			Object value = jsonObject.get(jsonObject.names().getString(i));

			//It's permissible to have null values in JSON, which we don't try to convert into 
			//an annotation or property
			if (value == null) {
				continue;
			}
			
			if (value instanceof Double) {
				varObj.addProperty(key, (Double) value);
			}
			else if (value instanceof Integer) {
				varObj.addProperty(key, Double.valueOf(Integer.toString((Integer) value)));
			}
			else if (value instanceof String) {
				varObj.addAnnotation(key, (String) value);
			}
			else {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not parse VariantRec from json, unrecognized value type: " + value.getClass());
			}
		}
		
		return varObj;
	}
}
