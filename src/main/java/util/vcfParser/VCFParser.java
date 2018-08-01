package util.vcfParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

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
	
	public static final String NO_SOURCE_WARNING_MESSAGE = "Cannot determine variant caller that generated VCF. No '##source' header property found, and header does not contain '##GATKCommandLine'."; 
	
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
	
	//Annotators in  this list are used to grab additional pieces of info from the vcf line
	//and convert them into annotations. They can be set via addVCFMetricsAnnotator(..)
	private List<VCFMetricsAnnotator> annotators = new ArrayList<VCFMetricsAnnotator>();
	
	//If true, will die if source cannot be identified from header
	private boolean failIfNoSource = true;
	
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
	 * Add a new annotator to that will examine the vcf line and convert some data from it into
	 * an annotation.
	 * @param anno
	 */
	public void addVCFMetricsAnnotator(VCFMetricsAnnotator anno) {
		annotators.add(anno);
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
			if (line.startsWith("##ALT") || line.startsWith("##INFO") || line.startsWith("##FORMAT")) {
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
		
		//ion torrent (*Torrent*), Real Time Genomics (RTG*), Complete Genomics (CGAPipeline*),
		//Merged LoFreq/Scalpel/Manta output
		//EXCEPT for GATK, which looks for UnifiedGenotyper=, GATKCommandLine=, or GATKCommandLine.HaplotypeCaller= fields
		creator =  headerProperties.get("source");

		if (creator != null) {
			
			if (creator.startsWith("gatk_haplotype")) creator = "GATK / HaplotypeCaller";
			else if (creator.startsWith("CGAPipeline")) creator = "CompleteGenomics";	
			else if (creator.equals("lofreq_scalpel_manta")) creator = "lofreq_scalpel_manta";
			else if (creator.equals("lofreq_scalpel_USeqMerged")) creator = "lofreq_scalpel_USeqMerged";
			else if (!(creator.startsWith("freeBayes")) && !(creator.contains("Torrent")) && !(creator.startsWith("RTG")) && !(creator.startsWith("CGAPipeline"))) {
				if (failIfNoSource) {
					throw new IOException("Cannot determine which variant caller generated the VCF. Header property '##source' must be start with 'freeBayes' or 'CGAPipeline' or contain 'Torrent' or 'RTG' or 'gatk_haplotype' or 'lofreq_scalpel_USeqMerged' .");
				}
			}
		} else {
			if ((headerProperties.containsKey("UnifiedGenotyper")) || (headerProperties.containsKey("GATKCommandLine"))) creator = "GATK / UnifiedGenotyper";
			else if (headerProperties.containsKey("GATKCommandLine.HaplotypeCaller")) creator = "GATK / HaplotypeCaller";
			else if (headerProperties.containsKey("USeqMergedLofreqScalpel")) creator = "USeqMergedLofreqScalpel";
			else if (failIfNoSource) throw new IOException(NO_SOURCE_WARNING_MESSAGE);
		}
	}
	
	/**
	 * If set to false, won't die if source cannot be identified. 
	 * @param fail
	 */
	public void setFailIfNoSource(boolean fail) {
		this.failIfNoSource = fail;
	}
	
	/**
	 * Names of all samples found in this VCF
	 * @return
	 */
	public Set<String> getSamples() {
		return sampleIndexes.keySet();
	}
	
	/**
	 * A string representing the creator of this VCF, usually "GATK / UnifiedGenotyper", 
	 * "FreeBayes", etc. 
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
		if (line.startsWith("##ALT=")) {
			entry.entryType = EntryType.ALT;
			line = line.replace("##ALT=<", "ALT,").replace(">", "");
		}
		if (line.startsWith("##INFO=")) {
			entry.entryType = EntryType.INFO;
			line = line.replace("##INFO=<", "INFO,").replace(">", "");
		}
		if (line.startsWith("##FORMAT=")) {
			entry.entryType = EntryType.FORMAT;
			line = line.replace("##FORMAT=<", "FORMAT,").replace(">", "");
		}
		
		String[] toks = line.split(",");
		for(int i=1; i<toks.length; i++) {
			
			if (toks[i].startsWith("ID=")) {
				entry.id = toks[0] + "_" +  toks[i].replace("ID=", "");
			}
			if (toks[i].startsWith("Number=")) {
				entry.number =toks[i].replace("Number=", "");
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
		sampleMetrics = null;
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
	Normalizes the reference & alternate sequences
	 * @return 
	*/
	public static String[][] normalizeRefAlt(Integer pos, String ref, String[] alt, boolean stripInitial, boolean stripTrailing) {
		//Order important here: Remove trailing bases first! IN cases where there are starting and 
		//trailing matching bases we want to preserve the start position as much as possible, since 
		//that is what ends up getting used for future position comparisons. 
		boolean normalize = false;
		//Remove trailing characters if they are equal and subtract that many bases from end position
		if (stripTrailing) {
			int matches=0;
			//Normalize only if all alts are the same length
			Integer altLength = alt[0].length();
			if (alt.length == 1) {
				normalize = true;
			} else {
				normalize = false;
				//Normalize only if all alts are the same length
				for (int i=1; i<alt.length; i++) {
					if (altLength != alt[i].length()) {
						break;
					} else if (i==alt.length) {
						normalize = true; 
					}
					
				}
			}

			if (normalize == true) {
				Integer globalMatches = 0;
				globalMatches = findNumberOfTrailingMatchingBases(ref, alt[0]);
				if (alt.length > 1) {
					for (int i=1; i<alt.length; i++) {
						while (globalMatches > 0 && i < alt.length) {
							int currentMatch = findNumberOfTrailingMatchingBases(alt[0], alt[i]);	
							if (currentMatch > 0 && currentMatch < globalMatches) {
								globalMatches = currentMatch;
							}
						}
					}
				}
				matches = globalMatches;
			} else {
				matches = 0;
			}
										
			//Perform normalization
			if (matches > 0) {	
				// Trim Ref
				ref = ref.substring(0, ref.length() - matches); 
				if (ref.length()==0) {
					ref = "-";
				}
				// Trim Alts 	
				for (int idx=0; idx < alt.length; idx++) {
					alt[idx] = alt[idx].substring(0, alt[idx].length() - matches); 
					if (alt[idx].length() ==0){								
						alt[idx] = "-";
					} 
				}
			}
		}

		//Remove initial characters if they are equal and add that many bases to start position
		//Warning: Indels may no longer be left-aligned after this procedure
		if (stripInitial && normalize) {
			Integer globalStartMatches = findNumberOfInitialMatchingBases(ref, alt[0]);
			for (int n=1; n < alt.length; n++) {
				int currentStartMatch = findNumberOfInitialMatchingBases(ref, alt[n]);
				if (currentStartMatch < globalStartMatches) {
					globalStartMatches = currentStartMatch;
				}
			}
			Integer StartMatchs = globalStartMatches;						
			if (StartMatchs > 0) {	
				// Trim Ref
				ref = ref.substring(StartMatchs);
				if (ref.length()==0) {
					ref = "-";
				}
				// Trim Alt 			
				for (int x=0; x < alt.length; x++) {
					alt[x] = alt[x].substring(StartMatchs); 
					if (alt[x].length() == 0){								
						alt[x] = "-";
					} 
				}

				//Update start position
				pos+=StartMatchs;				
			}
		}
		
		//Update end position
		String[] end=new String[alt.length];
		for (int y=0; y < alt.length; y++) {
			if (alt.equals("-")) {
				end[y] = Integer.toString(pos);
			}
			else {
				end[y] = Integer.toString(pos + alt[y].length());
			}
		}
		String[] posResults = {Integer.toString(pos)};
		String[] refResults = {ref};
		String[][] normData = {posResults, end, refResults, alt};
		
		return normData;


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
		//Variant rec only holds one alt value
		String ref = var.getRef();
		String alt = var.getAlt();
		String[] altInput = {alt};
		Integer pos = var.getStart();
		//Normalize in context of all alts
		String[][] normData = normalizeRefAlt(pos, ref, altInput, stripInitial, stripTrailing);
		Integer normPos = Integer.parseInt(normData[0][0]);
		Integer normEnd = Integer.parseInt(normData[1][0]);
		String normRef = normData[2][0];
		String normAlt = normData[3][0];
		//Don't forget to copy over annotations and properties...
		// Create sampleMetrics dictionary containing INFO & FORMAT field data, keyed by annotation
		VariantRec normalizedVariant = new VariantRec(var.getContig(), normPos, normEnd, normRef, normAlt, var.getQuality(), var.getGenotype(), var.getZygosity());

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

		String chr = getContig();
		int pos = getPos(); 
		String ref = getRef();
		String alt = getAlt(); //pulls out current alt
			
		String qualStr = currentLineToks[5];
		double quality = -1;
		try {
			quality = Double.parseDouble(qualStr);
		}
		catch (NumberFormatException ex) {
			//we tolerate it if we can't parse quality...
		}
		

		//@author elxainegee start

		sampleMetrics = createSampleMetricsDict(); //Stores sample-specific key=value pairs from VCF entry from FORMAT & INFO, not header	
	
		//Create new variant record
		String genotype = getGT();
		GTType isHet = isHetero();
		VariantRec var = new VariantRec(chr, pos, pos + ref.length(), ref, alt, quality, genotype, isHet);
		var = normalizeVariant(var, stripInitialMatchingBases, stripTrailingMatchingBases);
		var.setQuality(quality);

		//Store original VCF position, ref, & alt (untrimmed direct from file) as an annotation
		var.addProperty(VariantRec.VCF_POS, (double) pos);
		var.addAnnotation(VariantRec.VCF_REF, ref);
		var.addAnnotation(VariantRec.VCF_ALT, alt);
		
		//if FILTER isn't (e.g. empty, pass, or .) add it
		String filter = getFilter();
		if (filter.length() > 1 && filter.toUpperCase().equals("PASS") == false) var.addAnnotation(VariantRec.VCF_FILTER, filter);
		 
		// Get certain values
		Integer depth = getDepth();
		if (depth != null) {
			var.addProperty(VariantRec.DEPTH, new Double(depth));
		}
		
		String varCaller = getVarCaller();
		if (varCaller != null){
			var.addAnnotation(VariantRec.VAR_CALLER, varCaller);
		}

		// If we have a somatic caller try and grab the AF info field.
		if (creator.equals("lofreq_scalpel_manta") && sampleMetrics.containsKey("AF")) {
			double alleleFrequency = convertStr2Double(sampleMetrics.get("AF"));
			var.addProperty("var.freq", alleleFrequency);
		}

		Integer altDepth = getVariantDepth();
		if (altDepth != null) {
			var.addProperty(VariantRec.VAR_DEPTH, new Double(altDepth));
		}

		//Only add END if it was present in VCF info field
		Integer infoEnd = getInfoEND();
		if (infoEnd != null && infoEnd !=-1){
			var.addPropertyInt(VariantRec.SV_END, infoEnd);
		}

		String alleles = getMNPAlleleComponents();
		if(alleles != null) {
			var.addAnnotation(VariantRec.MNP_ALLELES, alleles);
		}
		//If no SVLEN present in VCF field, calculate size of indels instead
		Integer svlen = getSVLEN();
		if (svlen != null && svlen !=-1){
			var.addPropertyInt(VariantRec.INDEL_LENGTH, Math.abs(svlen));
		}else if (svlen.equals(-1)){
			int indelsize = var.getIndelLength();//gets indelsize if ref or alt =="-"
			int reflength = var.getRef().length();
			int altlength = var.getAlt().length();
			
			if (var.getAlt().equals("-")){
				altlength = altlength - 1;
			}
			if (var.getRef().equals("-")){
				reflength = reflength - 1;
			}
			indelsize = Math.abs(reflength - altlength);
			var.addPropertyInt(VariantRec.INDEL_LENGTH, indelsize);
			
			if (indelsize == 0){
				var.addPropertyInt(VariantRec.INDEL_LENGTH, null);
			}
		}

		String pindelRef = getPindelRef();
		if (pindelRef != null) {
			var.addAnnotation(VariantRec.PINDEL_ORIG_REF, pindelRef);
		}

		String pindelAlt = getPindelAlt();
		if (pindelAlt != null) {
			var.addAnnotation(VariantRec.PINDEL_ORIG_ALT, pindelAlt);
		}

		String genotypeQuality = getGenotypeQuality();
		if (genotypeQuality != null) {
			var.addAnnotation(VariantRec.GENOTYPE_QUALITY, genotypeQuality);
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

		var.addAnnotation(VariantRec.RAW_GT, getSampleMetricsStr("GT"));

		var.addAnnotation(VariantRec.SV_IMPRECISE, getSVImpreciseFlag());

		//Iterator over all annotators and cause them to annotator if need be
		for(VCFMetricsAnnotator vcfAnnotator : annotators) {
			vcfAnnotator.addAnnotation(var, sampleMetrics);
		}
		
		return var;
		

	}
	
	public enum GTType {
		HET, HOM, HEMI, UNKNOWN
	}
	
	public enum EntryType {
		FORMAT, INFO, ALT
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
	public Map<String, String> createSampleMetricsDict(){
		// Create dictionaries from key-value pairs from INFO & FORMAT fields
		
		Map<String, String>  finalDict = createFORMATDict();
		finalDict = createINFODict(finalDict);
		return finalDict;
	}

	
	/**
	 *  Creates a dictionary of INFO key-value pairs by storing info from column 8 in VCF format 4.1
	 *  @author elainegee
	 */
	private Map<String, String> createINFODict(Map<String, String> existing){
		
		//Tokenize INFO keys & values
		//String[] infoToks = currentLineToks[7].split(";"); //INFO key-value pairs
		StringTokenizer tokener = new StringTokenizer(currentLineToks[7], ";");
		while(tokener.hasMoreElements()) {
			String tok = tokener.nextToken();
			int index = tok.indexOf("=");
			if (index < 0) {
				existing.put(tok, tok);
			} else {
				existing.put(tok.substring(0, index), tok.substring(index+1, tok.length()));
			}	
		}
		return existing;
	}
	
	/**
	 *  Creates a dictionary of FORMAT key-value pairs by storing FORMAT strings (column 10 in VCF format 4.1, sample-specific values) 
	 *  according to FORMAT key (column 9 in VCF format 4.1)
	 *  @author elainegee
	 */
	private Map<String, String> createFORMATDict(){
		HashMap<String, String> dict = new HashMap<String, String>(100, 0.5f);
		
		//Tokenize FORMAT keys & values
		
		//Abort if there's no format data
		if (currentLineToks.length < 9) {
			return dict;
		}
		
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
	private static Double convertStr2Double(String str){
		if (str == null || str.length()==0 || str.equals("-")) {
			return -1.0;
		}
		try {
			Double outDouble = Double.parseDouble(str);
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
	 * Variant filter, anything but "." or "PASS" indicates a problem with the variant
	 * @author nix
	 * @return
	 */
	public String getFilter() {
		if (currentLineToks != null) {
			return currentLineToks[6];
		} else {
			return "";
		}		
	}
	
	/**
	 * String array of reference & all alternate sequences for variant
	 * @author elainegee
	 * @return
	 */
	public String[] getRawSeqArray() {
		if (currentLineToks != null) {
			String[] alts = currentLineToks[4].split(",");
			String[] allseq = new String[alts.length + 1];
			//add ref
			allseq[0] = currentLineToks[3]; 
		
			//add alts
			for (int i=0; i< alts.length; i++) {
				String currentAlt = alts[i];
				allseq[i+1] = currentAlt;

			}
			return allseq; 
		} else {
			return new String[0];
		}
	}	
		
	/**
	 * Total read depth at locus from INFO column, identified by "DP" and specified for the particular ALT
	 * @author elainegee
	 * @return
	 */
	public Integer getDepth(){
		//Get DP from sampleMetrics dictionary
		int dp = -1;
		String AnnoStr = null;
		int[] AnnoIdx = null;
		if (creator.contains("Torrent")){
			AnnoStr = "FDP"; //Flow evaluator metrics reflect the corrected base calls based on model of ref, alt called by FreeBayes, & original base call	
			AnnoIdx = new int[]{0};
		} else if (creator.contains("lofreq_scalpel_manta")) {
			if (getSampleMetricsStr("set").equals("lofreq")) {
				AnnoStr = "DP4";
				AnnoIdx = new int[]{0,1,2,3};
			} else if (getSampleMetricsStr("set").equals("scalpel") || getSampleMetricsStr("set").equals("pindel")) {
				AnnoStr = "DP";
				AnnoIdx = new int[]{0};
			} else if (getSampleMetricsStr("set").equals("manta")) {
				String pairedStr = getSampleMetricsStr("PR");
				String splitStr = getSampleMetricsStr("SR");
				String[] pairedDepthToks = {"0","0"};
				String[] splitDepthToks = {"0","0"};

				if (pairedStr != null) {
					pairedDepthToks = pairedStr.split(",");
				}
				if (splitStr != null) {
					splitDepthToks = splitStr.split(",");
				}

				dp = convertStr2Int(pairedDepthToks[0]) + convertStr2Int(pairedDepthToks[1]) +
						convertStr2Int(splitDepthToks[0]) + convertStr2Int(splitDepthToks[1]);
				return dp;
			} else if (getSampleMetricsStr("set").equals("MNPoster")) {
				AnnoStr = "DP";
				AnnoIdx = new int[]{0};
			} else {
				throw new IllegalStateException("ERROR: VCF malformed! Merged Lofreq/Scalpel/Manta VCF contains a 'set' key of "
						+ getSampleMetricsStr("set") + ", which is not defined. 'set' must be 'lofreq', 'scalpel', 'pindel', 'manta' or 'MNPoster'.");
			}
		} else {
			//If creator is not 'Torrent' or 'lofreq_scalpel_manta'
			AnnoStr = "DP";
			AnnoIdx = new int[]{0};
		}
		String depthStr = getSampleMetricsStr(AnnoStr);
		if (depthStr == null) {
			return dp;
		}
		String[] DepthToks = depthStr.split(",");
		dp = 0;
		for (int i : AnnoIdx)
			dp += convertStr2Int(DepthToks[i]);
		return dp;
	}
	
	/**
	 * Alternate allele count, identified by "AD" (GATK) or "AO" (FreeBayes) or 
	 * "FAO" (IonTorrent), need to calulate it for lofreq_scalpel, specified for the particular ALT
	 * @author elainegee, nix
	 * @return
	 */
	public Integer getVariantDepth(){
		int vardp = -1;
		String annoStr = null;
		int[] annoIdx = null;

		if (creator.startsWith("freeBayes")){
			annoStr = "AO";
			annoIdx = new int[]{altIndex}; //AO doesn't contain depth for REF, which is stored in RO
		} else if (creator.startsWith("Torrent")){
			annoStr = "FAO";
			annoIdx = new int[]{altIndex}; //FAO only contains infor for alternate allele
		} else if (creator.startsWith("RTG") || creator.equals("CompleteGenomics")) { //RTG variant caller or Complete Genomics
			annoStr = "AD";
			annoIdx = new int[]{altIndex}; //AD does not contain depth for REF
		} else if (creator.equals("lofreq_scalpel_manta")){
				if (getSampleMetricsStr("set").equals("lofreq")) {
					annoStr = "DP4";
					annoIdx = new int[]{2,3};
				} else if (getSampleMetricsStr("set").equals("scalpel") || getSampleMetricsStr("set").equals("pindel")) {
					annoStr = "AD";
					annoIdx = new int[]{altIndex + 1};
				} else if (getSampleMetricsStr("set").equals("MNPoster")) {
					if (sampleMetrics.containsKey("DP") && sampleMetrics.containsKey("AF")) {
						int dp = convertStr2Int(sampleMetrics.get("DP"));
						double af = convertStr2Double(sampleMetrics.get("AF"));
						return new Integer( (int)Math.round(dp * af));
					} else {
						throw new IllegalStateException("Could not parse DP and AF fields for reconstructed MNP variant");
					}
				} else if (getSampleMetricsStr("set").equals("manta")) {
					String pairedStr = getSampleMetricsStr("PR");
					String splitStr = getSampleMetricsStr("SR");
					
					String[] pairedDepthToks = null;
					String[] splitDepthToks = null;
					
					if (pairedStr != null) {
						pairedDepthToks = pairedStr.split(",");
					}
					if (splitStr != null) {
						splitDepthToks = splitStr.split(",");
					} 
					
					if (pairedStr != null &&  splitStr != null) {
						vardp = convertStr2Int(pairedDepthToks[altIndex+1]) + 
								convertStr2Int(splitDepthToks[altIndex+1]);
					} else if (pairedStr != null &&  splitStr == null) {
						vardp = convertStr2Int(pairedDepthToks[altIndex+1]);
					} else if (pairedStr == null &&  splitStr != null) {
						vardp = convertStr2Int(splitDepthToks[altIndex+1]);
					}
					return vardp;
				} else  {
					throw new IllegalStateException("ERROR: VCF malformed! Merged Lofreq/Scalpel/Manta VCF contains a 'set' key of "
							+ getSampleMetricsStr("set") + ", which is not defined. 'set' must be 'lofreq', 'scalpel', 'pindel', 'manta' or 'MNPoster'.");
				}
		} else if (creator.equals("lofreq_scalpel_USeqMerged")){
			//get total depth
			Integer readDept = this.getDepth();
			double dp = readDept.doubleValue();
			//get allele freq
			String alleleFreq = getSampleMetricsStr("AF");
			double af = convertStr2Double(alleleFreq);
			double ad = af * dp;
			return new Integer(  (int)Math.round(ad)  );
		} else {
			annoStr = "AD";
			annoIdx = new int[]{altIndex + 1}; //AD contains depth for REF
		}
		//Get alternate allele count from sampleMetrics dictionary	
		String varDepthStr = getSampleMetricsStr(annoStr);
		
		//Added to handle cases where FAO is not present in the VCF file
		if (varDepthStr == null && annoStr.equals("FAO")) {
			annoStr="AO";
			varDepthStr = getSampleMetricsStr(annoStr);
		}
		if (varDepthStr == null) {
			return null;
		}
		String[] varDepthToks = varDepthStr.split(",");
		vardp = 0;
		for (int i : annoIdx)
			vardp += convertStr2Int(varDepthToks[i]);
		return vardp;				
	}
	
	/**
	 * Genotype quality, identified by "GQ"
	 * @author elainegee
	 * @return
	 */
	public String getGenotypeQuality() {
		return getSampleMetricsStr("GQ");
	}

	/**
	 * If creator is "lofreq_scalpel_manta", search for a field with key "IMPRECISE", if found return true
	 * otherwise false
	 * If creator is not "lofreq_scalpel_manta", return null
	 * @return
	 */
	public String getSVImpreciseFlag() {
		if (creator.equals("lofreq_scalpel_manta")) {
			if (sampleMetrics.containsKey("IMPRECISE")) {
				return "true";
			} else {
				return "false";
			}
		}
		return null;
	}

	/**
	 * For records that are reconstructed MNPs created by MNPoster, parse and return the ALLELES entry in the
	 * INFO dict
	 * @return String containing component alleles, or null if no ALLELES entry is found in the INFO dict
	 */
	public String getMNPAlleleComponents() {
		if (sampleMetrics.containsKey("ALLELES")) {
			return sampleMetrics.get("ALLELES");
		} else {
			return null;
		}
	}

	/**
	 * Return the bases associated with the 'pindel.orig.ref' field, or null if that key doesn't exist
	 */
	public String getPindelRef() {
		if (sampleMetrics.containsKey("orig_ref")) {
			return sampleMetrics.get("orig_ref");
		} else {
			return null;
		}
	}

	/**
	 * Return the bases associated with the 'pindel.orig.alt' field, or null if that key doesn't exist
	 */
	public String getPindelAlt() {
		if (sampleMetrics.containsKey("orig_alt")) {
			return sampleMetrics.get("orig_alt");
		} else {
			return null;
		}
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
		String annoStr = null;
		if (creator.contains("Torrent")){
			annoStr = "STB";
		} else if (creator.equals("lofreq_scalpel_manta")) { 
			annoStr = "SB";
		} else {
			annoStr = "FS";
		}
		//Get FS from sampleMetrics dictionary
		String sbStr = getSampleMetricsStr(annoStr);
		Double sb = convertStr2Double(sbStr);
		return sb;	
	}
	/**
	 * Grabs structural variant size from VCF, identified by SVLEN field. If not SVLEN (as would be the case for germline), return -1
	 * @return svlen (structural variant length)
	 * @author chrisk
	 */
	public int getSVLEN(){
		int svlen = -1;
		if (creator.equals("lofreq_scalpel_manta")){
			String strsvlen = getSampleMetricsStr("SVLEN");
			svlen = convertStr2Int(strsvlen);
		}
		return svlen;
	}
	
	/**
	 * Grabs the info field END annotation if it exists. If not infoEND (as would be the case for germline), return -1
	 * @return infoend (end position of structural variant)
	 * @author jacobd
	 */
	public int getInfoEND(){
		String strinfoend = getSampleMetricsStr("END");
		if (strinfoend != null) {
			return convertStr2Int(strinfoend);
		} else {
			return -1;
		}
	}
	
	/**
	 * Grabs set value from info field which indicates what variant caller
	 * @return setfield (which variant caller called variant)
	 * @author ashinib
	 */
	public String getVarCaller(){
		String setfield = "";
		if (creator.equals("lofreq_scalpel_manta") || creator.equals("GATK / HaplotypeCaller")){
			setfield = getSampleMetricsStr("set");
		}
		return setfield;
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
	 * Returns the genotype sequence alleles 
	 * @author elainegee
	 * @return
	 */
	public String getGT() throws IllegalStateException {
		//Get GT from sampleMetrics dictionary
		String genoQualStr = getSampleMetricsStr("GT");
		// Grab array of ref & alternates
		String[] sequences = getRawSeqArray();
		
		if (genoQualStr != null) {		
			//Grab genotype sequence alleles when there are 2 alleles		
			String delimRegex = getGTDelimitor();
			String[] GTToks;
			//Parse out sequences if available
			GTToks = genoQualStr.split(delimRegex);
			String delim;
			if (delimRegex == "\\|") {
					delim = "|";
			} else {
					delim = delimRegex;								
			}
			
			//Throw an error if genotype is for an alt that exceeds the number of alts in VCF line
			for (int idx=0; idx < GTToks.length; idx++) {
				if (!GTToks[idx].equals(".")) {
					if (Integer.parseInt(GTToks[idx]) >= sequences.length) {
						throw new IllegalStateException("ERROR: VCF malformed! Genotype given as '" + String.valueOf(idx) + 
								"', but there are only '" + String.valueOf(sequences.length - 1) + "' alternate(s) in the VCF (GT value: '" 
								+ genoQualStr + "') for chr/pos/ref/alt: " + getContig() + "/" + getPos() + "/" + getRef() + "/" + getAlt());
					}
				}
			}
			
			if (!delimRegex.equals("")) {
				//Normalize alleles (not tracking position, so placeholder value used)
				String[] InputAlt = Arrays.copyOfRange(sequences, 1, sequences.length);
				String[][] normData = normalizeRefAlt(0, sequences[0], InputAlt, stripInitialMatchingBases, stripTrailingMatchingBases);
				//Unpack ref & alt into new String array
				String[] normSequences = new String[sequences.length];
				normSequences[0] = normData[2][0];
				for (int idx=0; idx < sequences.length - 1; idx ++) {
					normSequences[idx+1] = normData[3][idx]; 
				}


			
				// Grab diploid alleles
				String gtAlleles = "";
				String[] alleles = new String[2];
				for (int i=0; i < 2; i++) {
					String currentIdxStr = GTToks[i];
					if (currentIdxStr.equals(".")) {
						gtAlleles +=  currentIdxStr;
					} else {
						int currentIdx = Integer.parseInt(GTToks[i]);
						gtAlleles += normSequences[currentIdx];
						
						if (i < 1) {
							gtAlleles += delim;
						}
					}							
				}
				return gtAlleles;
				
			} else {
				//Grab genotype sequence for haploid chromosomes
				if (!genoQualStr.equals(".")) {
					int currentIdx = Integer.parseInt(genoQualStr);
					//Throw an error if index exceeds the number of alts in VCF
					if (currentIdx >= sequences.length) {
						throw new IllegalStateException("ERROR: VCF malformed! Genotype given as '" + String.valueOf(currentIdx) + 
								"', but there are only '" + String.valueOf(sequences.length - 1) + "alternates in the VCF (GT value: '" 
								+ genoQualStr + "'");
					}
					String gtAlleles =sequences[currentIdx];
					return gtAlleles;
				} else {
					//GT undefined, i.e. "."
					return genoQualStr;
				}
			}
		} else {
			return ".";
		}
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
				return "\\|"; //Pipe character needs to be a regex, otherwise considered an empty string in split command
			} else if (genoQualStr.contains("/"))  {
				return "/";
			} else {
				//Handle halploid chromosomes (X & M)
				if ((genoQualStr.length() == 1 && getContig().equals("X")) || (genoQualStr.length() == 1 && getContig().equals("M"))) {
					return "";
				} else {
					throw new IllegalStateException("Genotype separator char does not seem to be normal (i.e. | or /). GT field in VCF given as '" + genoQualStr + "'.");
				}
			}
		} else {
			throw new IllegalStateException("No genotype ('GT') specified in VCF.");
		}
	}
	
	
	/**
	 * Determine if variant is heterozygous
	 * @author elainegee
	 * @return 
	 * @return
	 */
	public GTType isHetero() {
		//Get GT from sampleMetrics dictionary
		String genoQualStr = getSampleMetricsStr("GT");
		if (genoQualStr == null) {
			return GTType.UNKNOWN;
		}
		try {
			if (genoQualStr.length() == 1) {
				if(genoQualStr.equals(".")) {
					//Handle case when VCF is missing info
					return GTType.UNKNOWN;
				} else { 
					//Handle hemizygous cases (male X, M)
					return GTType.HEMI;
				}				
			} else {
				//Determine if HET or HOM
				String delim = getGTDelimitor();
				String[] gtToks = genoQualStr.trim().split(delim);

				String refGT = gtToks[0]; //Allele1 genotype
				String altGT = gtToks[1]; //Allele2 genotype
				if (refGT.equals(".") && altGT.equals(".")) {
					//missing genotype for allele
					return GTType.UNKNOWN;
				} else if (refGT.equals(altGT)) {
					return GTType.HOM;
				} else {
					return GTType.HET;
				}
			}
		} catch (IllegalStateException ise) {
			//This situation can arise sometimes even in normal operation, for instance where we attempt
			//to parse the zygosity of somatic mutations called with ploidylevel = 50 (which happens for
			//myeloid samples), in this case the 'genotype' is 1/1/1/1/1/1/1/1/1..., which produces an error
			System.err.println("Warning: Could not parse genotype from " + genoQualStr);
			return GTType.UNKNOWN;
		}
	}
		
	/**
	 * Determine if variant is homozygous
	 * @author elainegee
	 * @return
	 */
	public GTType isHomo() {
		GTType ans = isHetero();
		return ans;
	}
	
	/**
	 * Returns true if the phasing separator is "|" and not "/" 
	 * @author elainegee
	 * @return
	 */
	public boolean isPhased() {
		if (getSampleMetricsStr("GT") == null) {
			return false;
		}
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
	
	/**
	 * A single entry in a VCF INFO field
	 * @author brendan
	 *
	 */
	private class InfoEntry {
		final String key;
		final String value;
		
		public InfoEntry(String key, String val) {
			this.key = key;
			this.value = val;
		}
	}
					
}
