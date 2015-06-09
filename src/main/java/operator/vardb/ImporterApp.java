package operator.vardb;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import util.reviewDir.ReviewDirectory;
import buffer.variant.VariantPool;

import com.mongodb.MongoClient;

public class ImporterApp {

	public VariantPool readFromJSON(JSONObject obj) throws JSONException {
		VariantPool pool = new VariantPool();
		JSONArray vars = obj.getJSONArray("variant.list");
		for(int i=0; i<vars.length(); i++) {
			JSONObject var = vars.getJSONObject(i);
			
			String chr = var.getString("chr");
			int pos = var.getInt("pos");
			String ref = var.getString("ref");
			String alt = var.getString("alt");
			
//			VariantRec varRec = new VariantRec(chr, pos, pos+alt.length(), ref, alt);
//			for(String key : var.keys()) {
//				try {
//					
//				}
//			}
			
			
		}
		return null;
	}
	
	public static void importDir(ReviewDirectory dir, VariantImporter importer, String sampleID) {
//		VariantPool vars = dir.getVariantsFromJSON();
//		try {
//			importer.importPool(vars, System.getProperty("user.name"), sampleID);
//		}
//		catch (Exception ex) {
//			System.err.println("Error importing variants for sample " + dir.getSampleName() + " (id: " + sampleID + ")\n " + ex.getLocalizedMessage() );
//		}
	}
	
	
	public static void main(String[] args) throws JSONException {
		VariantImporter importer = new VariantImporter(new MongoClient(), "blah", "test", null);
		//importer.importPool(null);
	}

}
