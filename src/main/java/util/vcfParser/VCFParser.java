package util.vcfParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import buffer.variant.VariantLineReader;
import buffer.variant.VariantRec;

/**
 * A replacement for the venerable VCFLineParser, this thing is in early dev but will ideally
 * have several handy features:
 * 	1. Reads single variants at a time, even when a single vcf line has multiple variants
 * 	2. Handles multi-sample VCF input easily
 * 	3. Provide access to VCF header information
 *  4. Automatically parse information from info and format fields correctly, even for multi-allelic sites
 *  5. Thorough testing and validation  
 * @author brendan + elainegee
 *
 */
public class VCFParser implements VariantLineReader {
	
	protected Map<String, HeaderEntry> headerItems = null; //Stores info about FORMAT and INFO fields from header
	protected Map<String, String> headerProperties = null; //Stores generic key=value pairs from header, not FORMAT or INFO
	protected Map<String, String> sampleMetrics = null; //Stores generic key=value pairs FORMAT or INFO, not header 
	
	
	protected File source = null;
	
	private BufferedReader reader = null;
	
	private String creator = null; //Tool that created this vcf, usually GATK / UG, FreeBayes, etc. 
	
	private int altIndex = 0; //Index of alt allele for the current line
	private String currentLine = null;
	private String[] currentLineToks = null; //current line split on tabs
	private int altsInCurrentLine = 1; //Total number of alts found on current line
	private int sampleIndex = 0; //Designates the index of the sample we want, defaulting to the first sample listed 
	private Map<String, Integer> sampleIndexes = null;
	
	public VCFParser(File source) throws IOException {
		setFile(source);

		reader = new BufferedReader(new FileReader(source));
		parseHeader();
	}
	
	/**
	 * Create a new vcf parser that only returns variants for the sample name provided
	 * @param source
	 * @param sampleName
	 * @throws IOException
	 */
	public VCFParser(File source, String sampleName) throws IOException {
		setFile(source);
		reader = new BufferedReader(new FileReader(source));
		parseHeader();
		
		if (! sampleIndexes.containsKey(sampleName)) {
			throw new IllegalArgumentException("Sample " + sampleName + " not found in this vcf.");
		}
		sampleIndex = sampleIndexes.get(sampleName);
	}
	
	/**
	 * Read the header of the file, including the list of samples, but do not parse any variants
	 * @throws IOException 
	 */
	public void parseHeader() throws IOException {
		if (source == null) {
			throw new IllegalStateException("Source file has not been set, cannot parse header");
		}
		headerItems = new HashMap<String, HeaderEntry>();
		headerProperties = new HashMap<String, String>();
		sampleIndexes = new HashMap<String, Integer>();
		String line = reader.readLine();
		while(line != null && line.startsWith("##")) {
			if (line.startsWith("##INFO") || line.startsWith("##FORMAT")) {
				HeaderEntry entry = parseHeaderItem(line);
				headerItems.put(entry.id, entry);
			}
			else {
				String[] keyval = line.replace("#", "").split("=", 2);
				if (keyval.length==2) {
					headerProperties.put(keyval[0], keyval[1]);
				}
			}
			
			line = reader.readLine();
		}
		
		//Line should now be at the line immediately prior to the actual variants list
		if (! line.toUpperCase().startsWith("#CHR")) {
			throw new IOException("Didn't find expected next line starting with #CHROM");
		}
		
		
		//Parse samples
		String[] toks = line.split("\t");
		for(int i=9; i<toks.length; i++) {
			sampleIndexes.put(toks[i].trim(), i-9);
		}
		
		//Infer creator. Freebayes & ion torrent define a source= field in the header
		//but GATK does not
		creator =  headerProperties.get("source");
		if (creator == null) {
			if (headerProperties.containsKey("UnifiedGenotyper")) {
				creator = "GATK / UnifiedGenotyper";
			}
		}
		
	}
	
	/**
	 * Names of all samples found in this VCF
	 * @return
	 */
	public Set<String> getSamples() {
		return sampleIndexes.keySet();
	}
	
	/**
	 * A string representing the creator of this VCF, usually "GATK / UnifiedGenotyper", "FreeBayes", etc. 
	 * @return
	 */
	public String getCreator() {
		return creator;
	}
	
	/**
	 * The reference used to call variants, from the reference=  header field
	 * @return
	 */
	public String getReference() {
		return headerProperties.get("reference");
	}
	
	/**
	 * Return the header entry associated with the given id or null if there is no
	 * header entry with that id
	 * @param id
	 * @return
	 */
	public HeaderEntry getHeaderEntry(String id) {
		return headerItems.get(id);
	}
	
	private HeaderEntry parseHeaderItem(String line) {
		HeaderEntry entry = new HeaderEntry();
		if (line.startsWith("##INFO=")) {
			entry.entryType = EntryType.INFO;
			line = line.replace("##INFO=<", "").replace(">", "");
		}
		if (line.startsWith("##FORMAT=")) {
			entry.entryType = EntryType.FORMAT;
			line = line.replace("##FORMAT=<", "").replace(">", "");
		}
		
		String[] toks = line.split(",");
		for(int i=0; i<toks.length; i++) {
			
			if (toks[i].startsWith("ID=")) {
				entry.id = toks[i].replace("ID=", "");
			}
			if (toks[i].startsWith("Number=")) {
				entry.number = toks[i].replace("Number=", "");
			}
			if (toks[i].startsWith("Type=")) {
				entry.type = toks[i].replace("Type=", "");
			}
			if (toks[i].startsWith("Description=")) {
				entry.description = toks[i].replace("Description=\"", "").replace("\"", "");
			}
		}
			
		return entry;
	}
	
	@Override
	public void setFile(File file) throws IOException {
		headerItems = null;
		this.source = file;
	}

	/**
	 * This actually advances to the next variant to be read, which doesn't involve advancing to
	 * the next line if there are more alts to read on the current line
	 */
	@Override
	public boolean advanceLine() throws IOException {
		altIndex++;
		if (altIndex == altsInCurrentLine) {
			
			currentLine = reader.readLine();
			if (currentLine != null) {
				currentLineToks = currentLine.split("\t");
				if (currentLineToks.length < 7) {
					throw new IOException("Invalid number of tokens on line: " + currentLine);
				}
				
				altIndex = 0;
				altsInCurrentLine = currentLineToks[4].split(",").length;
			}
		}
		return currentLine != null;
	}

	/**
	 * Returns true if the currentLineToks represent a variant that is
	 * has a genotype that is not ./. or 0/0 
	 * @return
	 */
	private boolean isVariant() {
		
	}
	
	@Override
	public String getCurrentLine() throws IOException {
		return currentLine;
	}

	@Override
	public String getHeader() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VariantRec toVariantRec() {
		
		String chr = currentLineToks[0].toUpperCase().replace("CHR","");
		int pos = Integer.parseInt(currentLineToks[1]);
		String ref = currentLineToks[3];
		String alt = currentLineToks[4].split(",")[altIndex];
		
		String qualStr = currentLineToks[5];
		double quality = -1;
		try {
			quality = Double.parseDouble(qualStr);
		}
		catch (NumberFormatException ex) {
			//we tolerate it if we can't parse quality...
		}
		
		VariantRec var = new VariantRec(chr, pos, pos+alt.length(), ref, alt);
		var.setQuality(quality);
	
		//@elainegee
		// Create sampleMetrics dictionary containing INFO & FORMAT field data, keyed by annotation
		sampleMetrics = createSampleMetricsDict(); //Stores sample-specific key=value pairs from VCF entry from FORMAT & INFO, not header	

		// Get certain values
		Integer depth = getDepth();
		if (depth != null) {
			var.addProperty(VariantRec.DEPTH, new Double(depth));
		}
	
		Integer altDepth = getVariantDepth();
		if (altDepth != null) {
			var.addProperty(VariantRec.VAR_DEPTH, new Double(altDepth));
		}
		/**
		Double genotypeQuality = getGenotypeQuality();
		if (genotypeQuality != null) 
			var.addProperty(VariantRec.GENOTYPE_QUALITY, genotypeQuality);

		Double vqsrScore = getVQSR();
		if (vqsrScore != null)
			var.addProperty(VariantRec.VQSR, vqsrScore);

		Double fsScore = getStrandBiasScore();
		if (fsScore != null)
			var.addProperty(VariantRec.FS_SCORE, fsScore);
		
		Double rpScore = getRPScore();
		if (rpScore != null)
			var.addProperty(VariantRec.RP_SCORE, rpScore);
		
				Double logFSScore = getLogFSScore();
		if (logFSScore != null)
			var.addProperty(VariantRec.LOGFS_SCORE, logFSScore);
	
		*/
		//#############EG stop
		
		
		
		return var;
		

	}

	
	
	
	public enum EntryType {
		FORMAT, INFO
	}
	
	/**
	 * Just a container for a single entry in the vcf header (either INFO or FORMAT)
	 * @author brendan
	 *
	 */
	public class HeaderEntry {
		public EntryType entryType;
		public String id;
		public String number;
		public String description;
		public String type;
		
		public String toString() {
			return entryType + ": ID=" + id + " " + description; 
		}
	}
	
	//###########EG start2
	/**
	 *  Return key-value pairs in final sampleMetrics dictionary containing metrics from both INFO & FORMAT fields
	 *  @author elainegee
	 */
	public HashMap<String, String> createSampleMetricsDict(){
		// Create dictionaries from key-value pairs from INFO & FORMAT fields
		HashMap<String, String> finalDict = createINFODict();
		HashMap<String, String>  formatDict = createFORMATDict();

		// Add information from FORMAT dictionary into final dictionary to return
		for (Map.Entry<String, String> entry: formatDict.entrySet()){
			String key = entry.getKey();
			String valueStr=entry.getValue();

			//Add value if key not already in dictionary. otherwise check if values are the same
			if (finalDict.get(key) != null && !finalDict.get(key).equals(valueStr)) {
				throw new IllegalStateException("Two different values for VCF field '" + key + "'.");
			} else {
				finalDict.put(key, valueStr);
			}
		}
		return finalDict;

	}

	
	/**
	 *  Creates a dictionary of INFO key-value pairs by storing info from column 8 in VCF format 4.1
	 *  @author elainegee
	 */
	public HashMap<String, String> createINFODict(){
		HashMap<String, String> dict = new HashMap<String, String>();
		//Tokenize INFO keys & values
		String[] infoToks = currentLineToks[7].split(";"); //INFO key-value pairs

		//Add data to dictionary
		for (int i=0; i < infoToks.length; i++)  {
			String[] infoData = infoToks[i].split("=");	
			dict.put(infoData[0], infoData[1]);
		}
		//Return INFO-only dictionary
		return dict;
	}
	
	/**
	 *  Creates a dictionary of FORMAT key-value pairs by storing FORMAT strings (column 10 in VCF format 4.1, sample-specific values) 
	 *  according to FORMAT key (column 9 in VCF format 4.1)
	 *  @author elainegee
	 */
	public HashMap<String, String> createFORMATDict(){
		HashMap<String, String> dict = new HashMap<String, String>();
		//Tokenize FORMAT keys & values
		String[] formatKeys = currentLineToks[8].split(":"); //FORMAT keys
		String[] formatData = currentLineToks[9].split(":"); //sample-specific FORMAT values

		//Add data to dictionary		
		for (int i=0; i < formatKeys.length; i++)  {
			dict.put(formatKeys[i], formatData[i]);
		} 
		//Return FORMAT-only dictionary
		return dict;
	}
	
	/**
	 * Looks up annotation specified by annoStr in sampleMetrics dictionary.
	 * @author elainegee
	 * @return
	 */
	private String getSampleMetricsStr(String annoStr){
		String outStr = sampleMetrics.get(annoStr);
		return outStr;
	}
	
	/** 
	 * Converts string (output of sampleMetrics dictionary) to integer. 
	 * @author elainegee
	 * @return
	 */
	private static Integer convertStr2Int(String AnnoOutStr){
		try {
			Integer outInt = Integer.parseInt(AnnoOutStr);
			return outInt;
		} catch (NumberFormatException nfe) {
			return -1; //-1 indicates no data found
		}	

	}
	
	/**
	 * Total read depth at locus from INFO column, identified by "DP" and specified for the particular ALT
	 * @author elainegee
	 * @return
	 */
	public Integer getDepth(){
		//Get DP from sampleMetrics dictionary
		String depthStr = getSampleMetricsStr("DP");
		Integer dp = convertStr2Int(depthStr);
		return dp;				
	}
	
	/**
	 * Alternate allelet count, identified by "AD" or "AO", specified for the particular ALT
	 * @author elainegee
	 * @return
	 */
	public Integer getVariantDepth(){
		String AnnoStr = null;
		if (creator == "FreeBayes"){
			AnnoStr = "AO";
		} else {
			AnnoStr = "AD";
		}

		//Get alternate allele count from sampleMetrics dictionary
		String varDepthStr = getSampleMetricsStr(AnnoStr);
		String[] varDepthToks = varDepthStr.split(",");
		System.out.println(varDepthToks[altIndex]);
		Integer vardp = convertStr2Int(varDepthToks[altIndex]);
		return vardp;				
	}
	
	//###########EG stop2
}
