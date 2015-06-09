package operator.vardb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import json.JSONException;
import json.JSONObject;

import org.bson.Document;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

/**
 * Provides a utility for importing a VariantPool into a MongoDB database. Each variantRec in the pool
 * is converted to a Mongo document and then added to the collection with 'collectionName'. 
 * For each VariantPool uploaded, we also update a document in a metadata collection that tracks a sampleID
 * upload status, error state, etc.   
 *   
 * @author brendan
 *
 */
public class VariantImporter {

	private static final String SET_ID = "set_id";
	private static final String ERROR = "error_message";
	private static final String COMPLETE = "complete";
	private static final String PERM_LOCK = "perm_lock";
	private static final String PERM_ERROR = "perm_error";
	private static final String DATE = "date";
	private static final String PERCENT_COMPLETE = "percent_complete";
	private static final String USER = "user";
	private static final String TYPE = "type";
	private static final String STATUS = "status";
	private static final String NUM_VARIANTS = "num_variants";
	private static final String IMPORT_DATE = "import_date";

	private MongoClient client;
	private MongoDatabase database;
	private String collectionName;
	private String metadataCollectionName;
	
	public VariantImporter(MongoClient client, String databaseName, String collectionName, String metadataCollectionName) {
		this.client = client;
		this.collectionName = collectionName;
		this.metadataCollectionName = metadataCollectionName;
		database = client.getDatabase(databaseName);
	}
	
	/**
	 * Iterate over all VariantRecs in the pool, create a Document object for each one, and add
	 * them all to the collection with the name 'collectionName'
	 * 
	 * This is done in chunks because we may be dealing with really huge variant pools and we dont
	 * want to create a document for each one and store them all in memory before importing (but we
	 * DO want to import in batches, which is probably way faster than import each one at a time)
	 * @param vars
	 * @throws Exception 
	 */
	public void importPool(VariantPool vars, String userID, String setID) throws Exception {
		
		MongoCollection<Document> metadataCollection = database.getCollection(metadataCollectionName);
		
		//First things first - is there already an entry with the given sampleID in the metadata?
		Document existingDoc = metadataCollection.find( Filters.eq(SET_ID, setID)).first();
		if (existingDoc != null) {
			throw new IllegalArgumentException("A set with id " + setID + " already exists (doc id: " + existingDoc.get("_id") + ")");
		}
		
		
		//Create metadata doc, set status to 'uploading'
		Document metadata = new Document();
		metadata.append(IMPORT_DATE, new Date());
		metadata.append(NUM_VARIANTS, vars.size());
		metadata.append(PERCENT_COMPLETE, 0.0);
		metadata.append(STATUS, (new Document())
									.append(TYPE, PERM_LOCK)
									.append(USER, userID)
									.append(DATE, new Date()));
		
		metadata.append(SET_ID, setID);
		metadataCollection.insertOne(metadata);
		
		Object metadataDocID = metadata.get("_id");
		
		try {
			
			//These are used to calculate percent completion
			double totVars = vars.size();
			double varsAdded = 0;
			
			//Iterate over all variants grouping them into lists of at most 'chunksize' items, then using
			//uploading those chunks one at a time
			MongoCollection<Document> collection = database.getCollection(collectionName);
			
			int maxChunkSize = 10000;
			List<Document> docs = new ArrayList<Document>();

			for(String contig : vars.getContigs()) {
				for(VariantRec var : vars.getVariantsForContig(contig)) {

					docs.add( toDocument(var, setID) );
					if (docs.size()>maxChunkSize) {
						collection.insertMany(docs);
						varsAdded += docs.size();
						metadataCollection.updateOne( Filters.eq("_id", metadataDocID), new Document("$set", new Document(PERCENT_COMPLETE, 100.0*varsAdded/totVars)));

						docs = new ArrayList<Document>(1024);
					
					}

				}
			}
		
			//Don't forget the last few
			collection.insertMany(docs);
			varsAdded += docs.size();
			metadataCollection.updateOne( Filters.eq("_id", metadataDocID), new Document("$set", new Document(PERCENT_COMPLETE, 100.0*varsAdded/totVars)));
		
			//TODO: Double check to make sure the correct number of entries are in the db!!
			long count = collection.count(Filters.eq(SET_ID, setID));
			if (count != (long)vars.size()) {
				throw new IllegalStateException("Number of variants in database (" + count + ") does not equal variant pool size (" + vars.size() + ")!");
			}
			
			//Finalize metadata doc, set status to 'done'
			metadataCollection.updateOne( Filters.eq("_id", metadataDocID), new Document("$set", new Document(STATUS + "." + DATE, new Date())));
			metadataCollection.updateOne( Filters.eq("_id", metadataDocID), new Document("$set", new Document(STATUS + "." + TYPE, COMPLETE)));
			
		} catch(Exception ex) {
			//Set metadata doc status of 'error'

			metadataCollection.updateOne( Filters.eq("_id", metadataDocID), new Document("$set", new Document(STATUS + "." + DATE, new Date())));
			metadataCollection.updateOne( Filters.eq("_id", metadataDocID), new Document("$set", new Document(STATUS + "." + TYPE, PERM_ERROR)));
			metadataCollection.updateOne( Filters.eq("_id", metadataDocID), new Document("$set", new Document(STATUS + "." + ERROR, ex.getLocalizedMessage())));
			throw ex;
		}
		
				
	}
	
	
	/**
	 * Convert the VariantRec into a Mongo Document that can be inserted
	 * @param var
	 * @return
	 */
	private static Document toDocument(VariantRec var, String setID) {
		Document doc = new Document();
		
		doc.append("chr", var.getContig());
		doc.append("pos", var.getStart());
		doc.append("ref", var.getRef());
		doc.append("alt", var.getAlt());
		doc.append(SET_ID, setID);
		
		for(String key : var.getAnnotationKeys()) {
			String normalizedKey = normalize(key);
			String val = var.getAnnotation(key);
			
			if (val != null && val.length()>0) {
				doc.append(normalizedKey, val);
			}
			
		}
		
		for (String key : var.getPropertyKeys()) {
			String normalizedKey = normalize(key);
			Double val = var.getProperty(key);
			
			if (val != null) {
				doc.append(normalizedKey, val.doubleValue());
			}			
		}
		
		return doc;
	}

	/**
	 * Create a JSON representation of the variant, including 
	 * @param var
	 * @return
	 * @throws JSONException
	 */
	private JSONObject toJSON(VariantRec var) throws JSONException {
		JSONObject obj = new JSONObject();
		
		for(String key : var.getAnnotationKeys()) {
			String normalizedKey = normalize(key);
			String val = var.getAnnotation(key);
			
			if (val != null && val.length()>0) {
				obj.put(normalizedKey, val);
			}
			
		}
		
		for (String key : var.getPropertyKeys()) {
			String normalizedKey = normalize(key);
			Double val = var.getProperty(key);
			
			if (val != null) {
				obj.put(normalizedKey, val.doubleValue());
			}			
		}
		
		return obj;
	}


	/**
	 * Replaces .'s and spaces with _'s, and converts all text to lowercase
	 * @param key
	 * @return
	 */
	private static String normalize(String key) {
		return key.replace(".", "_").replace(" ", "_").toLowerCase();
	}
}
