package util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import json.AnnotatedVarsJsonConverter;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import json.JSONWriter;
import operator.variant.VariantPoolWriter;
import util.text.StreamWriter;
import buffer.CSVFile;
import buffer.variant.CSVLineReader;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * A VariantPoolWriter that writes variants in JSON form. It tries to be smart about this and 
 * writes variants in a stream so we don't end up holding a huge object in memory. 
 * There are also a couple of static convenience methods provided if you just want to write
 * all the variants to an OutputStream or a file
 * @author brendan
 *
 */
public class JSONVarsGenerator extends VariantPoolWriter {

	public static final String VARIANT_LIST = "variant.list";
	protected AnnotatedVarsJsonConverter varConverter = new AnnotatedVarsJsonConverter();
	protected JSONWriter writer = null;
	
	//Keys that will be included in the output, no matter what. If the key
	//doesn't exist as an annotation in a variant, the json will include the annotation but
	//associate it with a 'null' value
	private Set<String> includeKeys = new HashSet<String>();
	
	//A list of annotation keys that we DONT WANT included with typical 
	//output
	private static List<String> DEFAULT_EXCLUDED_KEYS = Arrays.asList(new String[]{
			VariantRec.EXOMES_63K_AFR_HET,
			VariantRec.EXOMES_63K_AFR_HOM,
			VariantRec.EXOMES_63K_AMR_HET,
			VariantRec.EXOMES_63K_AMR_HOM,
			VariantRec.EXOMES_63K_EAS_HET,
			VariantRec.EXOMES_63K_EAS_HOM,
			VariantRec.EXOMES_63K_FIN_HET,
			VariantRec.EXOMES_63K_FIN_HOM,
			VariantRec.EXOMES_63K_NFE_HET,
			VariantRec.EXOMES_63K_NFE_HOM,
			VariantRec.EXOMES_63K_OTH_HET,
			VariantRec.EXOMES_63K_OTH_HOM,
			VariantRec.EXOMES_63K_SAS_HET,
			VariantRec.EXOMES_63K_SAS_HOM,
			VariantRec.EXOMES_63K_HET_FREQ
			});
	
	
	/**
	 * Obtain the set of included keys 
	 * @return
	 */
	public Collection<String> getIncludeKeys() {
		return Collections.unmodifiableCollection(includeKeys);
	}

	/**
	 * Set the annotation keys to include in the json output. If a variant does not have the key
	 * an entry will still be created but with a value of false 
	 * @return
	 */
	public void setIncludeKeys(Set<String> includeKeys) {
		this.includeKeys = includeKeys;
	}

	
	
	@Override
	public void writeHeader(PrintStream outputStream) throws IOException {

		//Set the keys to include
		this.varConverter.setKeys( new ArrayList<String>(this.includeKeys) );
		
		//Nothing to write, but we use this to initialize the JSONWriter
		writer = new JSONWriter(new StreamWriter(outputStream));
		try {
			writer
				.object()
				.key(VARIANT_LIST)
				.array();
		} catch (JSONException e) {
			e.printStackTrace();
			throw new IOException(e.getLocalizedMessage());
		}
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) throws IOException {
		try {
			JSONObject objVar = varConverter.toJSON(rec);
			writer.value(objVar);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new IOException(e.getLocalizedMessage());
		}
	}
	
	@Override
	public void writeFooter(PrintStream outputStream) throws IOException {
		try {
			writer.endArray();
			writer.endObject();
		} catch (JSONException e) {
			e.printStackTrace();
			throw new IOException(e.getLocalizedMessage());
		}
	}
	
	/**
	 * Convert all variants in the pool into a (potentially huge) json object. This works
	 * fine for exomes and smaller, but will probably break for genomes. 
	 * @param rec
	 * @return
	 * @throws JSONException 
	 */
	public JSONObject convertPoolToObject(VariantPool vars) throws JSONException {
		JSONObject jsonResponse = new JSONObject();
		
		if(variants==null){
			variants = new VariantPool();
		}
		
		varConverter.setExcludeKeys( DEFAULT_EXCLUDED_KEYS );
		
		JSONArray jsonVarList = new JSONArray();
		
		Set<String> keys = new HashSet<String>();
		
		for(String contig : variants.getContigs()) {
			for(VariantRec var : variants.getVariantsForContig(contig)) {
					keys.addAll(var.getAnnotationKeys());
					keys.addAll(var.getPropertyKeys());
			}
		}
		
		varConverter.setKeys(new ArrayList<String>(keys));
		
		for(String contig : variants.getContigs()) {
			for(VariantRec var : variants.getVariantsForContig(contig)) {
				JSONObject varObj = varConverter.toJSON(var);
				jsonVarList.put( varObj );				
			}
		}
		
		
		jsonResponse.put("variant.list", jsonVarList);		
		return jsonResponse;
	}
	
	
	public static String createEmptyJSONVariants(File destDir) throws JSONException, IOException {
		String destFilename = "annotated.json.gz";
		File dest = new File(destDir.getAbsolutePath() + "/" + destFilename);
		createJSONVariants(new VariantPool(), dest);
		return destFilename;
	}
	
	
	/**
	 * Write all variants in JSON form to the given output stream, gzipping the string
	 * @param variants
	 * @param outputStream
	 * @throws IOException
	 * @throws JSONException
	 */
	public static void createJSONVariantsGZIP(VariantPool variants, OutputStream outputStream) throws IOException, JSONException {
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
		JSONVarsGenerator.createJSONVariants(variants, gzipOutputStream);
		gzipOutputStream.close();
	}
	
	/**
	 * Write all of the variants in json form to the given output stream
	 * @param variants
	 * @param outputStream
	 * @throws JSONException
	 * @throws IOException
	 */
	public static void createJSONVariants(VariantPool variants, OutputStream outputStream) throws JSONException, IOException {
		JSONVarsGenerator jsonGenerator = new JSONVarsGenerator();
		
		BufferedOutputStream writer = new BufferedOutputStream(outputStream);
		PrintStream ps = new PrintStream(writer);
		
		//Collect all the keys we want to include
		Set<String> keys = new HashSet<String>();
		for(String contig: variants.getContigs()) {
			for (VariantRec var: variants.getVariantsForContig(contig)) {
				keys.addAll( var.getAnnotationKeys() );
				keys.addAll( var.getPropertyKeys() );
			}
		}
		jsonGenerator.setIncludeKeys(keys);
		
		jsonGenerator.writeHeader(ps);
		for(String contig: variants.getContigs()) {
			for (VariantRec var: variants.getVariantsForContig(contig)) {
				jsonGenerator.writeVariant(var, ps);
			}
		}
		jsonGenerator.writeFooter(ps);
		ps.close();
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
		FileOutputStream fs = new FileOutputStream(dest);
		JSONVarsGenerator.createJSONVariants(variants, fs);
		fs.close();
	}

	public static String createJSONVariants(CSVFile inputVars, File destDir) throws JSONException, IOException {
		String destFilename = inputVars.getFilename().replace(".csv", ".json.gz");
		destFilename = destFilename.replace(".xls", ".json.gz");
		File dest = new File(destDir.getAbsolutePath() + "/" + destFilename);
				
		VariantLineReader varReader = new CSVLineReader(inputVars.getFile());

		VariantPool variants = new VariantPool(varReader);
		createJSONVariantsGZIP(variants, new FileOutputStream(dest));
		return destFilename;
	}

	public static void main(String[] args) throws IOException {
		File annotatedVarsFile = new File(args[0]);
		CSVLineReader reader = new CSVLineReader(annotatedVarsFile);
		
		JSONVarsGenerator jGen = new JSONVarsGenerator();
		
		jGen.writeHeader(System.out);
		
		while(reader.advanceLine()) {
			VariantRec var = reader.toVariantRec();	
			jGen.writeVariant(var, System.out);
		}
		
		jGen.writeFooter(System.out);
		
	}
	
	
//	public static void main(String[] args) {
//		
//		DefaultManifestFactory manifestReader = new DefaultManifestFactory();
//		
//		boolean overwrite = false;
//		int start = 0;
//		if (args[0].equals("-f")) {
//			overwrite = true;
//			start = 1;
//		}
//		
//		for(int i=start; i<args.length; i++) {
//			File resultsDir = new File(args[i]);
//			if (! resultsDir.exists()) {
//				System.err.println("Results directory " + resultsDir.getAbsolutePath() + " does not exist, skipping it.");
//				continue;
//			}
//			try {
//				
//				SampleManifest manifest = manifestReader.readManifest(resultsDir.getAbsolutePath());
//								
//				String destFilename="";
//				String jsonVars = manifest.getProperty("json.vars");
//				if (jsonVars != null && (!overwrite)) {
//					System.err.println(resultsDir.getAbsolutePath() + " already has a json.vars file, not replacing it.");
//					continue;
//				}
//				
//				//Moved here (up 20 lines or so) by Dave to work for NA case
//				File dest = new File(resultsDir.getAbsolutePath() + "/var/");
//								
//				String annotatedCSV = manifest.getProperty("annotated.vars");
//								
//				//Handles special case for lung panel 
//				if (annotatedCSV == null) {
//					annotatedCSV = manifest.getProperty("annotated.vars.dna");
//				}
//				
//				
//				//Dave changed area
//				
//				File annotatedVarsFile = new File(resultsDir.getAbsolutePath() + "/" + annotatedCSV);
//		
//				if (annotatedVarsFile.exists()) {
//					destFilename = JSONVarsGenerator.createJSONVariants(new CSVFile(annotatedVarsFile), dest);	
//				}
//				else {
//					//Annotated vars file does not exist, so create an empty JSON variants file
//					destFilename = JSONVarsGenerator.createEmptyJSONVariants(dest);
//				}
//
//				
//				WritableManifest writable = new WritableManifest(manifest);
//				writable.put("json.vars", "var/" + destFilename);
//				writable.save();
//				System.err.println("Created new json.vars file for " + resultsDir.getName());
//				
//			} catch (ManifestParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} 
//		}
//		
//	}

	

	
}
