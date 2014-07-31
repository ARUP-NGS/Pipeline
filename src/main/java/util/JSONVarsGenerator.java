package util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import json.AnnotatedVarsJsonConverter;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import util.reviewDir.DefaultManifestFactory;
import util.reviewDir.ManifestParseException;
import util.reviewDir.SampleManifest;
import util.reviewDir.WritableManifest;
import buffer.CSVFile;
import buffer.variant.CSVLineReader;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Handles creation of a new 'annotated.json.gz' file from an input annotated.csv file
 * @author brendan
 *
 */
public class JSONVarsGenerator {

	/**
	 * Create a JSON object representing the given variants.
	 * @param variants
	 * @return
	 * @throws JSONException
	 */
	public static JSONObject createJSONVariants(VariantPool variants) throws JSONException{
		JSONObject jsonResponse = new JSONObject();
		
		AnnotatedVarsJsonConverter converter = new AnnotatedVarsJsonConverter();

		JSONArray jsonVarList = new JSONArray();
		
		Set<String> keys = new HashSet<String>();
		
		//Danger: could create huge json object if variant list is big
		for(String contig : variants.getContigs()) {
			for(VariantRec var : variants.getVariantsForContig(contig)) {
					keys.addAll(var.getAnnotationKeys());
					keys.addAll(var.getPropertyKeys());
			}
		}
		
		converter.setKeys(new ArrayList<String>(keys));
		
		for(String contig : variants.getContigs()) {
			for(VariantRec var : variants.getVariantsForContig(contig)) {
				jsonVarList.put( converter.toJSON(var) );				
			}
		}
		
		
		jsonResponse.put("variant.list", jsonVarList);		
		return jsonResponse;
	}
	
	/**
	 * Create a json representation of the variant pool and all of the annotations & properties in
	 * all of the variants, then write the json to the given file.
	 * @param variants
	 * @param dest
	 * @throws JSONException
	 * @throws IOException
	 */
	public static void createJSONVariants(VariantPool variants, File dest) throws JSONException, IOException {
		JSONObject jsonVars = createJSONVariants(variants);

		//Get the json string, then compress it to a byte array
		String str = jsonVars.toString();			
		byte[] bytes = compressGZIP(str);

//		if (dest.exists()) {
//			throw new IOException("Destination file already exists");
//		}

		BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(dest));
		writer.write(bytes);
		writer.close();
	}

	public static String createJSONVariants(CSVFile inputVars, File destDir) throws JSONException, IOException {
		String destFilename = inputVars.getFilename().replace(".csv", ".json.gz");
		destFilename = destFilename.replace(".xls", ".json.gz");
		File dest = new File(destDir.getAbsolutePath() + "/" + destFilename);
		
		VariantLineReader varReader = new CSVLineReader(inputVars.getFile());

		VariantPool variants = new VariantPool(varReader);
		createJSONVariants(variants, dest);
		return destFilename;
	}
	

	
	/** 
	 * GZIP compress the given string to a byte array
	 * @param str
	 * @return
	 */
	public static byte[] compressGZIP(String str){
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try{
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gzipOutputStream.write(str.getBytes("UTF-8"));
			gzipOutputStream.close();
		} catch(IOException e){
			throw new RuntimeException(e);
		}
		return byteArrayOutputStream.toByteArray();
	}
	
	
	public static void main(String[] args) {
		
		DefaultManifestFactory manifestReader = new DefaultManifestFactory();
		
		boolean overwrite = false;
		int start = 0;
		if (args[0].equals("-f")) {
			overwrite = true;
			start = 1;
		}
		
		for(int i=start; i<args.length; i++) {
			File resultsDir = new File(args[i]);
			if (! resultsDir.exists()) {
				System.err.println("Results directory " + resultsDir.getAbsolutePath() + " does not exist, skipping it.");
				continue;
			}
			try {
				SampleManifest manifest = manifestReader.readManifest(resultsDir.getAbsolutePath());
				
				String jsonVars = manifest.getProperty("json.vars");
				if (jsonVars != null && (!overwrite)) {
					System.err.println(resultsDir.getAbsolutePath() + " already has a json.vars file, not replacing it.");
					continue;
				}
				
				String annotatedCSV = manifest.getProperty("annotated.vars");
				//Handles special case for lung panel 
				if (annotatedCSV == null) {
					annotatedCSV = manifest.getProperty("annotated.vars.dna");
				}
				File annotatedVarsFile = new File(resultsDir.getAbsolutePath() + "/" + annotatedCSV);
				if (! annotatedVarsFile.exists()) {
					System.err.println("Annotated variant file " + annotatedCSV + " is specified in manifest, but does not exist!");
					continue;
				}
				
				File dest = new File(resultsDir.getAbsolutePath() + "/var/");
				String destFilename = JSONVarsGenerator.createJSONVariants(new CSVFile(annotatedVarsFile), dest);
				
				WritableManifest writable = new WritableManifest(manifest);
				writable.put("json.vars", "var/" + destFilename);
				writable.save();
				System.err.println("Created new json.vars file for " + resultsDir.getName());
				
			} catch (ManifestParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
	}
}
