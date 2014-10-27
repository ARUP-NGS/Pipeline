package operator.pindel;

import gene.ExonLookupService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import buffer.FileBuffer;

public class PindelResultsContainer extends FileBuffer {
	
	//List of PINDEL output file suffices we deal with
	private String[] outputFileSuffixes = new String[]{"_D", "_LI", "_TD", "_SI"};

	//Storage for all parsed results, grouped by suffix (_D, _LI, etc)
	private Map<String, List<PindelResult>> results = null;

	//Parses PINDEL output and converts it into a PindelResult object
	private PindelParser parser;

	
	public void readResults(String prefix, int threshold, int mergeDistance) throws IOException {
		
		results = new HashMap<String, List<PindelResult>>();
		for (String currentFile : outputFileSuffixes) {
			File thisFile = new File(prefix + currentFile);
			File filteredFile = new File(prefix + "2" + currentFile);
			if (thisFile.exists() && thisFile.length() > 0) {
				parser = new PindelParser(thisFile);
				parser.filter(threshold);
				parser.makePindelFile(filteredFile);
				parser.combineResults(mergeDistance);
				results.put(currentFile, parser.getResults());
			}
		}
	}

	public Map<String, List<PindelResult>> getPindelResults() {
		return results;
	}
	
	public JSONObject resultsToJSON() throws JSONException {
		JSONObject resultObj = new JSONObject();
		
		for(String resultType : results.keySet()) {
			JSONArray categoryObj = new JSONArray();
			
			for(PindelResult hit : results.get(resultType)) {
				JSONObject singleResult = pindelResultToJSON(hit);
				categoryObj.put(singleResult);
			}
			
			resultObj.put(resultType, categoryObj);
		}
		
		return resultObj;
	}
	
	private static JSONObject pindelResultToJSON(PindelResult pr) throws JSONException {
		JSONObject res = new JSONObject();
		res.put("chr", pr.getChromo());
		res.put("start", pr.getRangeStart());
		res.put("end", pr.getRangeEnd());
		res.put("supportingReads", pr.getSupportReads());
		res.put("meanDepth", pr.getMeanDepth());
		
		JSONArray features = new JSONArray();
		for(String feat : pr.getAllAnnotations()) {
			features.put(feat);
		}
		res.put("features", features);
		return res;
	}
	
	
	public static void main(String[] args) {
		File pindelOutput = new File("/home/brendan/DATA2/testsamples/g7PindelOutput/");
		try {
			PindelResultsContainer cont = new PindelResultsContainer();
			cont.readResults(pindelOutput.getAbsolutePath(), 15, 25);
			
			Map<String, List<PindelResult>> results = cont.getPindelResults();
			ExonLookupService featureLookup = new ExonLookupService();
			String featureFile = "/home/brendan/resources/features20140909.v2.bed";
			
			File features = new File(featureFile);
			if (!features.exists()) {
				throw new IOException("Feature file " + features.getAbsolutePath() + " does not exist!");
			}
			featureLookup.buildExonMap(features);
			
			for(String svType : results.keySet()) {
				for(PindelResult sv : results.get(svType)) {
					Object[] overlappingFeatures = featureLookup.getIntervalObjectsForRange(sv.getChromo(), sv.getRangeStart(), sv.getRangeEnd());
					for(Object feat : overlappingFeatures) {
						sv.addFeatureAnnotation(feat.toString());
					}
				}
			}
			
			for(String svType : results.keySet()) {
				System.out.println("SV Type : " + svType);
				List<PindelResult> svs = results.get(svType);
				for(PindelResult res : svs) {
					System.out.println("\t" + res.toShortString());
				}
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	@Override
	public String getTypeStr() {
		return "PindelResults";
	}

}