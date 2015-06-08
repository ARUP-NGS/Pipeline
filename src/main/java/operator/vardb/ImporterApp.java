package operator.vardb;

import json.JSONException;

import com.mongodb.MongoClient;

public class ImporterApp {

	public static void main(String[] args) throws JSONException {
		VariantImporter importer = new VariantImporter(new MongoClient(), "blah", "test");
		importer.importPool(null);
	}

}
