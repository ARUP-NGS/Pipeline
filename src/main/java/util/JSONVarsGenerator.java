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
import buffer.GeneList;
import buffer.variant.VariantRec;
import gene.Gene;

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
	public static final AnnotatedVarsJsonConverter DEFAULT_CONVERTER = new AnnotatedVarsJsonConverter(); 
	protected AnnotatedVarsJsonConverter varConverter = DEFAULT_CONVERTER;
	protected JSONWriter writer = null;
	
	//Keys that will be included in the output, no matter what. If the key
	//doesn't exist as an annotation in a variant, the json will include the annotation but
	//associate it with a 'null' value
	private Set<String> includeKeys = new HashSet<String>();
	private static List<String> excludedKeys = new ArrayList<String>();
	
	//A list of annotation keys that we DONT WANT included with typical 
	//output
	private static List<String> DEFAULT_EXCLUDED_KEYS = Arrays.asList(new String[]{
			//VariantRec.ARUP_HET_COUNT,
			//VariantRec.ARUP_SAMPLE_COUNT,
			//VariantRec.GERP_NR_SCORE,
			//VariantRec.RP_SCORE,
			//VariantRec.FS_SCORE,
			//VariantRec.VCF_FILTER,
			//VariantRec.VQSR
			});
	
	//A list of gene annotation keys that we WANT included 
	public final static List<String> geneKeys = new ArrayList<String>( Arrays.asList(new String[]{
			Gene.OMIM_DISEASES,
			Gene.OMIM_NUMBERS,
			Gene.OMIM_INHERITANCE,
			Gene.HGMD_INFO}));
	
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
		
		//Set the default keys to exclude
		List<String> allExKeys = new ArrayList<String>();
		allExKeys.addAll(excludedKeys);
		for (String key : DEFAULT_EXCLUDED_KEYS) {
			if (!allExKeys.contains(key)) {
				allExKeys.add(key);
			}
		}
		this.varConverter.setExcludeKeys(allExKeys);
		
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
	public static JSONObject convertPoolToObject(VariantPool vars) throws JSONException {
		JSONObject jsonResponse = new JSONObject();
		
		if(vars==null){
			vars = new VariantPool();
		}
		
		DEFAULT_CONVERTER.setExcludeKeys( DEFAULT_EXCLUDED_KEYS );
		
		JSONArray jsonVarList = new JSONArray();
		
		Set<String> keys = new HashSet<String>();
		
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
					keys.addAll(var.getAnnotationKeys());
					keys.addAll(var.getPropertyKeys());
			}
		}
		
		DEFAULT_CONVERTER.setKeys(new ArrayList<String>(keys));
		
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				JSONObject varObj = DEFAULT_CONVERTER.toJSON(var);
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
	 * Write all variants in JSON form to the given output stream, gzipping the string
	 * Also adds important gene annotations to the variants
	 * (Gene.OMIM_DISEASES,Gene.OMIM_NUMBERS,Gene.OMIM_INHERITANCE,Gene.HGMD_INFO)
	 * @param variants
	 * @param geneList
	 * @param outputStream
	 * @throws IOException
	 * @throws JSONException
	 */
	public static void createJSONVariantsGZIP(VariantPool variants, GeneList geneList, OutputStream outputStream) throws IOException, JSONException {
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
		
		for(String contig: variants.getContigs()) {
			for (VariantRec var: variants.getVariantsForContig(contig)) {	
				for(int i=0; i<geneKeys.size(); i++) {
					Gene g = var.getGene();
					if (g == null) {
						String geneName = var.getAnnotation(VariantRec.GENE_NAME);
						if (geneName != null && geneList != null)
							g = geneList.getGeneByName(geneName);
					}

					String val = "-";
					if (g != null) {
						val = g.getPropertyOrAnnotation(geneKeys.get(i)).trim();
					}

					//Special case, if HGMD_INFO, just emit "true" if there is anything
					if (geneKeys.get(i).equals(Gene.HGMD_INFO) && val.length() > 5) {
						val = "true";
					}

					var.addAnnotation(geneKeys.get(i), val);
				}
			}
		}

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
		//added to minimize ngsweb impact
		keys.add(VariantRec.INDEL_LENGTH);
		//remove excluded keys
		for (String ex : DEFAULT_EXCLUDED_KEYS) {
				keys.remove(ex);
		}
		for (String ex : excludedKeys) {
				keys.remove(ex);
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

	public static void createJSONVariantsGZIP(VariantPool variants, File dest) throws JSONException, IOException {
		FileOutputStream fs = new FileOutputStream(dest);
		JSONVarsGenerator.createJSONVariantsGZIP(variants, fs);
		fs.close();
	}

	public static void createJSONVariantsGZIP(VariantPool variants, File dest, List<String> excludeKeys) throws JSONException, IOException {
		excludedKeys = excludeKeys;
		FileOutputStream fs = new FileOutputStream(dest);
		JSONVarsGenerator.createJSONVariantsGZIP(variants, fs);
		fs.close();
	}

	public static void createJSONVariantsGZIP(VariantPool variants, GeneList geneList, File dest, List<String> excludeKeys) throws JSONException, IOException {
		excludedKeys = excludeKeys;
		FileOutputStream fs = new FileOutputStream(dest);
		JSONVarsGenerator.createJSONVariantsGZIP(variants, geneList, fs);
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
