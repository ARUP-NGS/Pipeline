package util.varFreqDB;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.bson.Document;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoFreqUploader {

	public static final String CHR = "chr";
	public static final String POS = "pos";
	public static final String REF = "ref";
	public static final String ALT = "alt";
	public static final String TEST_TYPE = "test_type";
	public static final String TOT_SAMPLES = "tot_samples";
	public static final String HETS = "hets";
	public static final String HOMS = "homs";
	
	public static final String TEMP_COLLECTION_PREFIX = "varfreq_uploadtmp";
	

	private MongoDatabase database;
	
	@Parameter(names = {"-d", "--db"}, description = "Name of database to upload into")
	String databaseName = "arupfreq";
	
	@Parameter(names = {"-c", "--collection"}, description = "Name of collection to upload into")
	String collectionName = "variant_frequencies";
	
	@Parameter(names = {"-h", "--host"}, description = "Mongo URL")
	String mongoURL = "localhost:27017";
	
	@Parameter(names = {"-f", "--file"}, description = "Path to file to upload", required=true)
	String inputFilepath;
	
	public void uploadFrequencies() throws IOException {
		BufferedReader reader;
		if (inputFilepath.endsWith(".gz")) {
			reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(inputFilepath))));
		} else {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilepath)));
		}
		
		
		String line = reader.readLine();
		
		if (! mongoURL.startsWith("mongodb://")) {
			mongoURL = "mongodb://" + mongoURL;
		}
		MongoClient client = new MongoClient(new MongoClientURI(mongoURL));
		this.database = client.getDatabase(databaseName);
		
		//We upload into a new temp collection, then we replace th existing real collection with the temp one
		//if all goes well. 
		
		String randomDigits = ("" + System.currentTimeMillis());
		randomDigits = randomDigits.substring(randomDigits.length()-5, randomDigits.length());
		
		String tmpCollectionName = TEMP_COLLECTION_PREFIX + randomDigits;
		
		MongoCollection<Document> collection = database.getCollection(tmpCollectionName);
			
		List<Document> docs = new ArrayList<Document>();
		
		int entriesUploaded = 0;
		while(line != null) {
			
			//Iterate over all variants grouping them into lists of at most 'chunksize' items, then using
			//uploading those chunks one at a time
			int maxChunkSize = 10000;
			
			Document doc = toDocument(line);
			if (doc != null) {
				docs.add( doc );
				entriesUploaded++;
			}
			if (docs.size()>maxChunkSize) {
				collection.insertMany(docs);
				docs.clear();
			}
			
			if (entriesUploaded%10000==0) {
				System.err.println("Uploaded " + entriesUploaded + " to " + mongoURL + " " + databaseName + ":" + collectionName);
			}
			
			line = reader.readLine();
		}
		
		collection.insertMany(docs);
		entriesUploaded += docs.size();
		
		if (entriesUploaded > 0) {
			MongoCollection<Document> finalCollection = database.getCollection(this.collectionName);
			MongoNamespace ns = finalCollection.getNamespace();
			finalCollection.drop();
			collection.renameCollection(ns);
			
			System.err.println("All done, uploaded " + entriesUploaded + " total entries");
			System.err.println("Creating indexes...");
			collection = database.getCollection(ns.getCollectionName());
			collection.createIndex(new Document().append(CHR, 1)
					.append(POS, 1)
					.append(REF, 1)
					.append(ALT, 1));
			
		} else {
			System.err.println("Error, no variants detected in input file " + inputFilepath + ", refusing to update existing data with empty collection.");
		}
		
		client.close();
	}
	
	
	/**
	 * Create a new mongo Document by parsing information from the given line  
	 * @param line
	 * @return
	 */
	private static Document toDocument(String line) {
		String[] toks = line.split("\t");
		if (toks.length < 7) {
			return null;
		}

		try {
			Document doc = new Document();
			doc.append(CHR, toks[0])
			.append(POS, Integer.parseInt(toks[1]))
			.append(REF, toks[2])
			.append(ALT, toks[3])
			.append(TEST_TYPE, toks[4])
			.append(TOT_SAMPLES, Integer.parseInt(toks[5]))
			.append(HETS, Integer.parseInt(toks[6]))
			.append(HOMS, Integer.parseInt(toks[7]));

			return doc;
		}
		catch(NumberFormatException nfe) {
			return null;
		}
		
	}
	

	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		if (args.length==0) {
			System.err.println("Mongo ARUP frequency importer tool");
			System.err.println("\t This tools takes a csv or csv.gz and imports it into a mongo db.");
			System.err.println("\t Typically the csv file to upload is generated by computeVarFreqs.jar, and then uploaded with this thing.");
			System.err.println("\n\t Example usage: java -jar mongoUploader.jar -h [mongo server addr] -d [database name] -c [collection name] -f [file to upload]");
			return;
		}
		MongoFreqUploader mfu = new MongoFreqUploader();
		
		new JCommander(mfu, args);
				
		mfu.uploadFrequencies();

	}
}
