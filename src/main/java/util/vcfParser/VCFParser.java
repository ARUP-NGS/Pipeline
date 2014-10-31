package util.vcfParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import buffer.VCFFile;
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
	
	private boolean stripInitialMatchingBases = true; //defaults to true (i.e. will trim)
	private boolean stripTrailingMatchingBases = true; //defaults to true (i.e. will trim)
	
	public VCFParser(File source) throws IOException {
		setFile(source); //Initializes reader and parses the header information
	}
	
	public VCFParser(VCFFile file) throws IOException {
		this(file.getFile());
	}
	
	/**
	 * Create a new vcf parser that only returns variants for the sample name provided
	 * @param source
	 * @param sampleName
	 * @throws IOException
	 */
	public VCFParser(File source, String sampleName) throws IOException {
		setFile(source);//Initializes reader and parses the header information
		
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
		while(line != null && (line.startsWith("##") || line.trim().length()==0)) {
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
			throw new IOException("Didn't find expected next line starting with #CHR (found " + line + ")" );
		}
		
		
		//Parse samples
		String[] toks = line.split("\t");
		for(int i=9; i<toks.length; i++) {
			sampleIndexes.put(toks[i].trim(), i-9);
		}
		
		//Infer creator from source= field in the header. Accepted creators: FreeBayes (freeBayes*), ion torrent (*Torrent*), Real Time Genomics (RTG*), Complete Genomics (CGAPipeline*) 
		//EXCEPT for GATK, which looks for UnifiedGenotyper= or GATKCommandLine= fields
		creator =  headerProperties.get("source");
		if (creator != null) {
			if (creator.startsWith("SelectVariants")) {
				creator = "GATK / UnifiedGenotyper";
			} else if (!(creator.startsWith("freeBayes")) && !(creator.contains("Torrent")) && !(creator.startsWith("RTG")) && !(creator.startsWith("CGAPipeline"))) {
				throw new IOException("Cannot determine variant caller that generated VCF. Header property '##source' must be start with 'freeBayes' or 'CGAPipeline' or contain 'Torrent' or 'RTG' or 'SelectVariants'.");
			}
		} else {
			if ((headerProperties.containsKey("UnifiedGenotyper")) || (headerProperties.containsKey("GATKCommandLine"))) {
				creator = "GATK / UnifiedGenotyper";
			} else {
				throw new IOException("Cannot determine variant caller that generated VCF. No '##source' header property found, and header does not contain '##GATKCommandLine'.");
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
		reader = new BufferedReader(new FileReader(source));
		parseHeader();
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
			while(currentLine != null && currentLine.trim().length()==0) {
				currentLine = reader.readLine();	
			}
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

	
	
	@Override
	public String getCurrentLine() throws IOException {
		return currentLine;
	}

	@Override
	public String getHeader() throws IOException {
		if (source == null) {
			return "No source file set";
		}
		StringBuilder strb = new StringBuilder();
		BufferedReader headerReader = new BufferedReader(new FileReader(source));
		String line = headerReader.readLine();
		while(line != null && line.startsWith("#")) {
			strb.append(line + "\n");
			line = headerReader.readLine();
		}
		return strb.toString();
	}
	
	
	/**
	 * Returns a new VariantRec that has the pos, ref, and alt converted to a normalized form. This
	 * involves removing any trailing matching bases, then removing all initial matching bases. 
	 * @param var
	 * @return
	 */
	public static VariantRec normalizeVariant(VariantRec var) {
		return VCFParser.normalizeVariant(var, true, true);
	}
	
	/**
	 * Returns a new VariantRec that has the pos, ref, and alt converted to a normalized form. This
	 * version gives control over whether initial or trailing matches are removed. If both are false
	 * no modifications are performed. 
	 * 
	 * @param var
	 * @return
	 */
	public static VariantRec normalizeVariant(VariantRec var, boolean stripInitial, boolean stripTrailing) {
		String ref = var.getRef();
		String alt = var.getAlt();
		int pos = var.getStart();

		//Order important here: Remove trailing bases first! IN cases where there are starting and 
		//trailing matching bases we want to preserve the start position as much as possible, since 
		//that is what ends up getting used for future position comparisons. 
 		// Create sampleMetrics dictionary containing INFO & FORMAT field data, keyed by annotation

		//Remove trailing characters if they are equal and subtract that many bases from end position
		if (stripTrailing) {
			int matches = findNumberOfTrailingMatchingBases(ref, alt);						
			if (matches > 0) {	
				// Trim Ref
				ref = ref.substring(0, ref.length() - matches); 
				if (ref.length()==0) {
					ref = "-";
				}
				// Trim Alt 			
				alt = alt.substring(0, alt.length() - matches); 
				if (alt.length()==0){								
					alt = "-";
				} 

			}
		}

		//Remove initial characters if they are equal and add that many bases to start position
		//Warning: Indels may no longer be left-aligned after this procedure
		if (stripInitial) {
			int matches = findNumberOfInitialMatchingBases(ref, alt);						
			if (matches > 0) {	
				// Trim Ref
				ref = ref.substring(matches);
				if (ref.length()==0) {
					ref = "-";
				}
				// Trim Alt 			
				alt = alt.substring(matches); 
				if (alt.length()==0){								
					alt = "-";
				} 

				//Update start position
				pos+=matches;				
			}
		}

		//Update end position
		Integer end=null;
		if (alt.equals("-")) {
			end = pos;
		}
		else {
			end = pos + ref.length();
		}


		VariantRec normalizedVariant = new VariantRec(var.getContig(), pos, end, ref, alt, var.getQuality(), var.isHetero());
		//Don't forget to copy over annotations and properties...
		for(String key : var.getAnnotationKeys()) {
			normalizedVariant.addAnnotation(key, var.getAnnotation(key));
		}
		for(String key : var.getPropertyKeys()) {
			normalizedVariant.addProperty(key, var.getProperty(key));
		}
		normalizedVariant.setGene(var.getGene());
		return normalizedVariant;
	}
	
	
	
	@Override
	public VariantRec toVariantRec() {
		if (currentLineToks == null) {
			return null;
		}
		if (headerItems == null || headerProperties == null) {
			throw new IllegalStateException("No header information, header probably not parsed correctly.");
		}

		//	String chr = currentLineToks[0].toUpperCase().replace("CHR","");
		String chr = getContig();
		int pos = getPos(); 
		String ref = getRef();
		String alt = getAlt(); 
		
		String qualStr = currentLineToks[5];
		double quality = -1;
		try {
			quality = Double.parseDouble(qualStr);
		}
		catch (NumberFormatException ex) {
			//we tolerate it if we can't parse quality...
		}
		

		//@author elainegee start

		sampleMetrics = createSampleMetricsDict(); //Stores sample-specific key=value pairs from VCF entry from FORMAT & INFO, not header	

		
		//Create new variant record
		boolean isHet = isHetero();
		VariantRec var = new VariantRec(chr, pos, pos + ref.length(), ref, alt, quality, isHet);
		var = normalizeVariant(var, stripInitialMatchingBases, stripTrailingMatchingBases);
		var.setQuality(quality);

		// Get certain values					
		Integer depth = getDepth();
		if (depth != null) {
			var.addProperty(VariantRec.DEPTH, new Double(depth));
		}
	
		Integer altDepth = getVariantDepth();
		if (altDepth != null) {
			var.addProperty(VariantRec.VAR_DEPTH, new Double(altDepth));
		}

		Double genotypeQuality = getGenotypeQuality();
		if (genotypeQuality != null) {
			var.addProperty(VariantRec.GENOTYPE_QUALITY, genotypeQuality);
		}
		
		Double vqsrScore = getVQSR();
		if (vqsrScore != null) {
			var.addProperty(VariantRec.VQSR, vqsrScore);
		}

		Double fsScore = getStrandBiasScore();
		if (fsScore != null) {
			var.addProperty(VariantRec.FS_SCORE, fsScore);
		}
		
		Double rpScore = getRPScore();
		if (rpScore != null){
			var.addProperty(VariantRec.RP_SCORE, rpScore);
		}
		
		//@author elainegee stop
		
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
	
	/**
	 * Returns whether initial matching bases between REF & ALT are stripped
	 */
	public boolean isStripInitialMatchingBases() {
		return stripInitialMatchingBases;
	}	
	
	/**
	 * Sets boolean for determining whether to strip initial matching bases between REF & ALT 
	 */
	public void setStripInitialMatchingBases(boolean stripInitialMatchingBases) {
		this.stripInitialMatchingBases = stripInitialMatchingBases;
	}
	
	/**
	 * Returns whether trailing matching bases between REF & ALT are stripped
	 */
	public boolean isStripTrailingMatchingBases() {
		return stripTrailingMatchingBases;
	}	
	
	/**
	 * Sets boolean for determining whether to strip trailing matching bases between REF & ALT 
	 */
	public void setStripTrailingMatchingBases(boolean stripTrailingMatchingBases) {
		this.stripTrailingMatchingBases = stripTrailingMatchingBases;
	}
	
	/**
	 * Calculates the number of shared bases between the ref sequence & alternate allele
	 * at the BEGINNING of the sequence (only takes one ref and one alt as input)
	 * @author elainegee 
	 * @return
	 */
	public static int findNumberOfInitialMatchingBases(String ref, String alt) {
		String[] altToks = alt.split(",");
		int AltCount = altToks.length;
		String shortestAlt = altToks[0];

		// find length of initial matching bases across all alleles
		int i;						
		for(i=0; i<Math.min(ref.length(), shortestAlt.length()); i++) {
			int validAlts = 0; // counts number of alts that match
			char refchar = ref.charAt(i);
			// loop through each alt
			for (int j=0; j< AltCount; j++) {
				String testAlt = altToks[j];					
				if (refchar == testAlt.charAt(i)) {
					validAlts++;
				}
			}
			// if not all alts match, then return last matching base index 
			if (validAlts - AltCount != 0) {
				return i;							
			}
		}
		return i; 
	}
	
	/**
	 * Calculates the number of shared bases between the ref sequence & alternate allele 
	 * at the END of the sequence
	 * @author elainegee 
	 * @return 
	 */
	public static int findNumberOfTrailingMatchingBases(String ref, String alt) {
		// find length of matching trailing bases 
		int i;		
		int matchBases = 0;
		for(i=0; i<Math.min(ref.length(), alt.length()); i++) {
			if (ref.charAt(ref.length()-i-1) == alt.charAt(alt.length()-i-1)) {
				matchBases++;
			} else {
				return matchBases; //stop loop if no matches
			}
		}
		return matchBases; 
	}
	
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
			//FreeBayes multisample VCFs have several fields that have the same key in both FORMAT and INFO, but different
			//values. In general, I think we want the sample-specific fields from the FORMAT to clobber the more general
			//ones from INFO if there is a conflict, so I'm removing the check here fow now. If there are cases
			//where we want the INFO, not the FORMAT field, we'll need to be smarter about this
			finalDict.put(key, valueStr);
//			if (finalDict.get(key) != null && !finalDict.get(key).equals(valueStr)) {
//				if (key.equals("DP")) {
//					//use the filtered read depth in the FORMAT field
//					finalDict.put(key, valueStr);
//				} else {
//					throw new IllegalStateException("Two different values for VCF field '" + key + "': " + finalDict.get(key) + ", " + valueStr + ".");
//				}
//				} else {
//					finalDict.put(key, valueStr);
//				}
//				}
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
			String valueStr = null;
			if (infoData.length == 1) {
				valueStr = infoData[0];
			} else {
				valueStr = infoData[1];
			}
			dict.put(infoData[0], valueStr);
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
		//It's actually OK (according to vcf 4.1 spec) to drop non-specified trailing fields from the formatData
		//so we try to read in only as many fields as there are formatData tokens
		for (int i=0; i < formatData.length; i++)  {
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
	 * Converts string (output of sampleMetrics dictionary) to double. 
	 * @author elainegee
	 * @return
	 */
	private static Double convertStr2Double(String AnnoOutStr){
		try {
			Double outDouble = Double.parseDouble(AnnoOutStr);
			return outDouble;
		} catch (NumberFormatException nfe) {
			return -1.0; //-1.0 indicates no data found
		} catch (NullPointerException npe) {
			return -1.0; //-1.0 indicates no data found
		}
	}
	
	/**
	 * Variant chromosome/contig
	 * @author elainegee
	 * @return
	 */
	public String getContig() {
		if (currentLineToks != null) {
			return currentLineToks[0].toUpperCase().replace("CHR","");
		} else {
			return "?";
		}		
	}
	
	/**
	 * Variant position
	 * @author elainegee
	 * @return
	 */
	public Integer getPos() {
		if (currentLineToks != null) {
			return Integer.parseInt(currentLineToks[1]);
		} else {
			return -1;
		}		
	}
	
	/**
	 * Reference sequence for variant
	 * @author elainegee
	 * @return
	 */
	public String getRef() {
		if (currentLineToks != null) {
			return currentLineToks[3];
		} else {
			return "?";
		}
	}		

	/**
	 * Alternate sequence for variant
	 * @author elainegee
	 * @return
	 */
	public String getAlt() {
		if (currentLineToks != null) {
			return currentLineToks[4].split(",")[altIndex];
		} else {
			return "?";
		}
	}	
	
	/**
	 * Total read depth at locus from INFO column, identified by "DP" and specified for the particular ALT
	 * @author elainegee
	 * @return
	 */
	public Integer getDepth(){
		//Get DP from sampleMetrics dictionary
		String AnnoStr = null;
		if (creator.contains("Torrent")){
			AnnoStr = "FDP"; //Flow evaluator metrics reflect the corrected base calls based on model of ref, alt called by FreeBayes, & original base call
		} else {
			AnnoStr = "DP";
		}
		String depthStr = getSampleMetricsStr(AnnoStr);
		Integer dp = convertStr2Int(depthStr);
		return dp;				
	}
	
	/**
	 * Alternate allele count, identified by "AD" (GATK) or "AO" (FreeBayes) or 
	 * "FAO" (IonTorrent), specified for the particular ALT
	 * @author elainegee
	 * @return
	 */
	public Integer getVariantDepth(){
		String annoStr = null;
		Integer annoIdx = null;
		if (creator.startsWith("freeBayes")){
			annoStr = "AO";
			annoIdx = altIndex; //AO doesn't contain depth for REF, which is stored in RO
		} else if (creator.startsWith("Torrent")){
			annoStr = "FAO";
			annoIdx = altIndex; //FAO only contains infor for alternate allele
		} else if (creator.startsWith("RTG") || creator.startsWith("CGAPipeline")) { //RTG variant caller or Complete Genomics
			annoStr = "AD";
			annoIdx = altIndex; //AD does not contain depth for REF
		} else {
			annoStr = "AD";
			annoIdx = altIndex + 1; //AD contains depth for REF
		}
		//Get alternate allele count from sampleMetrics dictionary	
		String varDepthStr = getSampleMetricsStr(annoStr);
		if (varDepthStr == null) {
			return null;
		}
		String[] varDepthToks = varDepthStr.split(",");
		Integer vardp = convertStr2Int(varDepthToks[annoIdx]);
		return vardp;				
	}
	
	/**
	 * Genotype quality, identified by "GQ"
	 * @author elainegee
	 * @return
	 */
	public Double getGenotypeQuality(){
		//Get GQ from sampleMetrics dictionary
		String genoQualStr = getSampleMetricsStr("GQ");
		Double gq = convertStr2Double(genoQualStr);
		return gq;	
	}

	/**
	 * Log odds ratio of being a true variant vs. being falsed under trained Gaussian 
	 * mixture model, identified by "VQSLOD"
	 * @author elainegee
	 * @return
	 */
	public Double getVQSR(){
		//Get VQSLOD from sampleMetrics dictionary
		String vqsrStr = getSampleMetricsStr("VQSLOD");
		Double vqsr = convertStr2Double(vqsrStr);
		return vqsr;	
	}
		
	/**
	 * Strand Bias detected by Fisher's exact test, identified by "FS" (GATK) or STB (IonTorrent)
	 * @author elainegee
	 * @return
	 */
	public Double getStrandBiasScore(){
		String AnnoStr = null;
		if (creator.contains("Torrent")){
			AnnoStr = "STB";
		} else {
			AnnoStr = "FS";
		}
		//Get FS from sampleMetrics dictionary
		String sbStr = getSampleMetricsStr(AnnoStr);
		Double sb = convertStr2Double(sbStr);
		return sb;	
	}
	
	/**
	 * Alt vs. Ref Read position bias Z-score from Wilcoxon rank sum test, identified by "ReadPosRankSum"
	 * @author elainegee
	 * @return
	 */
	public Double getRPScore(){
		//Get ReadPosRankSum from sampleMetrics dictionary
		String rpStr = getSampleMetricsStr("ReadPosRankSum");
		Double rp = convertStr2Double(rpStr);
		return rp;	
	}
	
	
	/**
	 * Returns the genotype delimiter, if properly separated
	 * @author elainegee
	 * @return
	 */
	public String getGTDelimitor() {
		//Get GT from sampleMetrics dictionary
		String genoQualStr = getSampleMetricsStr("GT");
		if (!genoQualStr.equals(null)) {
			if (genoQualStr.contains("|")) {
				return "|";
			} else if (genoQualStr.contains("/"))  {
				return "/";
			} else {
				throw new IllegalStateException("Genotype separator char does not seem to be normal (i.e. | or /).");
			}
		} else {
			throw new IllegalStateException("No genotype ('GT') specified in VCF.");
		}
	}
	
	/**
	 * Returns true if the genotype is not ./. or 0/0 
	 * @author elainegee
	 * @return
	 */
	public boolean isVariant() {
		//Determine if GT delimiter is valid
		String delim=null;
		try {
			delim = getGTDelimitor();			
		} catch (IllegalStateException ise) {
			throw new IllegalStateException ("Error processing request:", ise);
		}
		//Get GT from sampleMetrics dictionary
		String genoQualStr = getSampleMetricsStr("GT");
		
		String[] GQToks = genoQualStr.split(delim);
		if ((GQToks[0].equals(".") && GQToks[1].equals(".")) || (GQToks[0].equals("0") && GQToks[1].equals("0"))) {
			return false;
		} else {
			return true;
		}				
	}
	
	/**
	 * Determine if variant is heterozygous
	 * @author elainegee
	 * @return
	 */
	public boolean isHetero() {
		//Get GT from sampleMetrics dictionary
		String genoQualStr = getSampleMetricsStr("GT");
		try {
			String delim = getGTDelimitor();
			String[] gtToks = genoQualStr.split(delim);
		
			String refGT = gtToks[0]; //Allele1 genotype
			String altGT = gtToks[1]; //Allele2 genotype
			if (refGT.equals(altGT)) {
				return false;
			} else {
				return true;
			}
		} catch (IllegalStateException ise) {
			throw new IllegalStateException ("Error processing request:", ise);
		}
	}
		
	/**
	 * Determine if variant is homozygous
	 * @author elainegee
	 * @return
	 */
	public boolean isHomo() {
		return ! isHetero();
	}
	
	/**
	 * Returns true if the phasing separator is "|" and not "/" 
	 * @author elainegee
	 * @return
	 */
	public boolean isPhased() {
		//Get GT from sampleMetrics dictionary
		try {
			String delim = getGTDelimitor();
			if (delim.equals("|")) {
				return true;
			} else {
				return false;
			}
		} catch (IllegalStateException ise) {
			throw new IllegalStateException ("Error processing request:", ise);
		}
	}
					
}
