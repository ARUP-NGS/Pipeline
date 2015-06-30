package operator.vardb;

import java.io.IOException;

import json.JSONException;
import util.reviewDir.ManifestParseException;
import util.reviewDir.ReviewDirectory;
import buffer.variant.VariantPool;

import com.mongodb.MongoClient;

public class ImporterApp {

	
	public static void importDir(ReviewDirectory dir, VariantImporter importer, String sampleID, String groupID) throws IOException, JSONException {
		VariantPool vars = dir.getVariantsFromJSON();
		try {
			importer.importPool(vars, System.getProperty("user.name"), sampleID, groupID);
		}
		catch (Exception ex) {
			System.err.println("Error importing variants for sample " + dir.getSampleName() + " (id: " + sampleID + ")\n " + ex.getLocalizedMessage() );
		}
	}
	
	
	public static void main(String[] args) throws JSONException {
		String host = "10.90.225.19:27017";
		String varDBName = "test";
		String varCollectionName = "vars_from_ngsweb";
		String metadataCollectionName = "metadata_from_ngsweb";
		

		if (args.length==0) {
			System.err.println("Please enter the sampel id, group id, and the path to the review directory to upload");
			return;
		}
		
		String sampleID = args[0];
		String groupID = args[1];
		String dirPath = args[2];
		
		
		VariantImporter importer = new VariantImporter(new MongoClient(host), varDBName, varCollectionName, metadataCollectionName);

		try {
			ReviewDirectory dir = new ReviewDirectory(dirPath);
			importDir(dir, importer, sampleID, groupID);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ManifestParseException e) {
			e.printStackTrace();
		}
		
		
	}

}
