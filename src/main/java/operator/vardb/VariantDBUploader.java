package operator.vardb;

import java.io.IOException;
import java.util.logging.Logger;

import json.JSONException;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.variant.VariantPool;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * Uses a VariantImporter to upload all variants in the variant pool to a mongo database. 
 * Requires a url, databasename, collection name, and metadata collection name as Pipeline properties.
 * 
 * ...and when we use authentication, this will also require username / password 
 * @author brendan
 *
 */
public class VariantDBUploader extends Operator {

	public static final String SAMPLEID = "sampleID";
	public static final String SERVER_ADDRS="mongo.server.addrs";
	public static final String MONGO_VARDB_NAME="mongo.vardb.name";
	public static final String MONGO_VAR_COLLECTION_NAME="mongo.var.collection.name";
	public static final String MONGO_METADATA_COLLECTION_NAME = "mongo.metadata.collection.name";
	
	
	//These all get set during initialization
	private VariantPool variants = null;
	private MongoClient mongoClient = null; 
	private VariantImporter importer = null;
	private String sampleID = null;
	
	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Importing " + variants.size() + " variants into Mongo db.");
		
		try {
			
			importer.importPool(variants, System.getProperty("user.name"), sampleID);
			
		} catch (Exception e) {
			e.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).severe("Could not upload variants to variant db: " + e.getLocalizedMessage());
			throw new OperationFailedException(e.getLocalizedMessage(), this);
		}
		
		//Dont forget to close the client 
		mongoClient.close();
		Logger.getLogger(Pipeline.primaryLoggerName).info("Finished import successfully.");

	}

	@Override
	public void initialize(NodeList children) {
				
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VariantPool) {
					variants = (VariantPool)obj;
				}
			}
		}
		
		if (variants == null) {
			throw new IllegalArgumentException("No variant pool specified!");
		}
		
		sampleID = this.getAttribute(SAMPLEID);
		if (sampleID == null) {
			throw new IllegalArgumentException("Must specify a sample id using attribute " + SAMPLEID);
		}
				
		//Initialize MongoDB connection
		//Grab server address / port list
		//We expect a comma-separated list of ipaddress:port names, like 12.43.123.4321:27017,11.22.33.44:8888 
		String servers = this.getPipelineProperty(SERVER_ADDRS);
		if (servers == null) {
			throw new IllegalArgumentException("The server address for MongoDB must be supplied as a pipeline property.");
		}
		
		String databaseName = this.getPipelineProperty(MONGO_VARDB_NAME);
		if (databaseName==null) {
			throw new IllegalArgumentException("The name of the variant database must be supplied as a pipeline property.");
		}
		
		String collectionName = this.getPipelineProperty(MONGO_VAR_COLLECTION_NAME);
		if (collectionName==null) {
			throw new IllegalArgumentException("The name of the variant collection must be supplied as a pipeline property.");
		}
		
		String metadataCollectionName = this.getPipelineProperty(MONGO_METADATA_COLLECTION_NAME);
		if (metadataCollectionName==null) {
			throw new IllegalArgumentException("The name of the variant metadata collection must be supplied as a pipeline property.");
		}
		
		MongoClientURI connectionString = new MongoClientURI("mongodb://" + servers);
		mongoClient = new MongoClient(connectionString);
		
		
		importer = new VariantImporter(mongoClient, databaseName, collectionName, metadataCollectionName); 
	}

}
