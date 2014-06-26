package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.VCFFile;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantRec;

/**
 * This class provides a uniform interface for extracting values from a single line of a vcf file.
 * Right now this makes several assumptions regarding the format of the VCF which
 * work well with the GaTK's vcfs, but may break with other vcf types. Use
 * GenericVCFParser for a more flexible version.  
 * @author brendan
 *
 */
public class VCFLineParser_OLD extends PipelineObject implements VariantLineReader  {

		private BufferedReader reader;
		private int currentLineNumber = -1;
		private String currentLine = null;
		protected String[] lineToks = null;
		private String[] formatToks = null; //Tokenized format string, produced as needed
		protected int gtCol = -1; //Format column which contains genotype info
		protected int gqCol = -1; //Format column which contains genotype quality info 
		protected int adCol = -1; //Format column which contains allele depth info
		protected int aoCol = -1; //Format column which contains 'alternate allele' depth count
		protected int dpCol = -1; //Format column which contains depth info
		protected int fdpCol = -1; //Format column which contains depth info
		protected int faoCol = -1; //Format column which contains depth info
				
		private String sample = null; //Emit information for only this sample if specified (when not given, defaults to first sample)
		private int sampleColumn = -1; //Column that stores information for the given sample
		protected File sourceFile;
		
		private boolean stripInitialMatchingBases = true; //defaults to true (i.e. will trim)
		
		private String currentFormatStr = null;
		
		private boolean parseAllInfoTokens = false; //If true, we create annotations / properties for every token in the info field
		
		public VCFLineParser() {
			sourceFile = null;
			//No arg constructor, imput stream and sample must be set using setters
		}
		
		public VCFLineParser(File file, String sample) throws IOException {
			setInputStream(new FileInputStream(file));
			this.sourceFile = file;
			currentLine = reader.readLine();
			this.sample = sample; //Sample must be specified before header is read
			readHeader();
		}
		
		@Override
		public void setFile(File file) throws IOException {
			setInputStream(new FileInputStream(file));
			this.sourceFile = file;
			currentLine = reader.readLine();
			readHeader();
		}
		
		/**
		 * Create a VCFLineReader to read variants from the given input stream
		 * @param stream
		 * @throws IOException
		 */
		public VCFLineParser(InputStream stream) throws IOException {
			setInputStream(stream);
			sourceFile = null;
			primeForReading();
		}
		
		public VCFLineParser(File file) throws IOException {
			this(file, false);
		}
		
		public VCFLineParser(File file, boolean parseInfoToks) throws IOException {
			setInputStream(new FileInputStream(file));
			this.sourceFile = file;
			this.parseAllInfoTokens = parseInfoToks;
			primeForReading();
		}

		
		public VCFLineParser(VCFFile file) throws IOException {
			this(file.getFile());
		}
		
		public void setInputStream(InputStream stream) throws IOException {
			this.reader = new BufferedReader(new InputStreamReader(stream));
		}
		
		public void primeForReading() throws IOException {
			if (this.reader == null) {
				throw new IllegalArgumentException("No reader set");
			}
			
			currentLine = reader.readLine();
			sampleColumn = 9; //First column with info, this is the default when no sample is specified
			readHeader();
		}
		
		public boolean isPrimed() {
			return currentLine != null;
		}
		
		public int getSampleColumn() {
			return sampleColumn;
		}
		
		public String getHeader() throws IOException {
			if (sourceFile == null) {
				return null;
			}
			
			if (! isPrimed()) {
				primeForReading();
			}
			
			BufferedReader headReader = new BufferedReader(new FileReader(sourceFile));
			String line = headReader.readLine();
			StringBuilder strB = new StringBuilder();
			while(line != null && line.trim().startsWith("#")) {
				strB.append(line + "\n");
				line = headReader.readLine();
			}
			headReader.close();
			return strB.toString();
		}
		
		private void readHeader() throws IOException {
			while (currentLine != null && currentLine.startsWith("#")) {
				advanceLine();
				
				if (currentLine == null) {
					//no data in file
					return;
				}
				
				if (currentLine.startsWith("#CHROM")) {
					String[] toks = currentLine.split("\t");
					if (sample == null) {
						sampleColumn = 9;
						if (toks.length > 9)
							sample = toks[9];
						else 
							sample = "unknown";
					}
					else {
						for(int col = 0; col<toks.length; col++) {
							if (toks[col].equals(sample)) {
								sampleColumn = col;
							}
						}
					}
					if (sampleColumn < 0) {
						throw new IllegalArgumentException("Cannot find column for sample " + sample);
					}
				}
				
			}
		}
		
		public String getSampleName() {
			return sample;
		}
		
		/**
		 * Advance the current line until the contig found is the given contig. If
		 * already at the given contig, do nothing
		 * @param contig
		 * @throws IOException 
		 */
		public void advanceToContig(String contig) throws IOException {
			if (! isPrimed()) {
				primeForReading();
			}
			
			while (hasLine() && (!getContig().equals(contig))) {
				advanceLine();
			}
			if (! hasLine()) {
				throw new IllegalArgumentException("Could not find contig " + contig + " in vcf");
			}
		}
		
		/**
		 * Advance the current line until we reach a contig whose name matches the contig arg,
		 * and we find a variant whose position is equal to or greater than the given position
		 * @throws IOException 
		 */
		public void advanceTo(String contig, int pos) throws IOException {
			advanceToContig(contig);
			while(hasLine() && getPosition() < pos) {
				advanceLine();
				if (! hasLine()) {
					throw new IllegalArgumentException("Advanced beyond end file looking for pos: " + pos);
				}
				if (! getContig().equals(contig)) {
					throw new IllegalArgumentException("Could not find position " + pos + " in contig " + contig);
				}
			}
		}
		
		public boolean isPassing() {
			return currentLine.contains("PASS");
		}
		
		
		/**
		 * Converts the information in the current line to a VariantRec, by default this
		 * will strip 'chr' from all contig names
		 * @return
		 */
		public VariantRec toVariantRec() {
			return toVariantRec(true);
		}

		/**
		 * Calculates the number of shared bases between the ref sequence & string array of all alternate alleles
		 * @return
		 */
		public static int findNumberOfInitialMatchingBases(String ref, String alt) {
			String[] altToks = alt.split(",");
			int AltCount = altToks.length;
			String shortestAlt = altToks[0];
			// find shortest alt allele
			for(int j=0; j< AltCount; j++) {
				if (shortestAlt.length() > altToks[j].length()){
					shortestAlt = altToks[j];
				}
			}
			// find length of matching bases across all alleles
			int i;						
			for(i=0; i<Math.min(ref.length(), shortestAlt.length()); i++) {
				int validAlts = 0;
				char refchar = ref.charAt(i);
				for (int j=0; j< AltCount; j++) {
					String testAlt = altToks[j];					
					if (refchar == testAlt.charAt(i)) {
						validAlts++;
					}
				}
				if (validAlts - AltCount != 0) {
					return i;							
				}
			}
			return i; 
		}
		
		/**
		 * Convert current line into a variant record
		 * @param stripChr If true, strip 'chr' from contig name, if false do not alter contig name
		 * @return A new variant record containing the information in this vcf line
		 */
		public VariantRec toVariantRec(boolean stripChr) {			
			if (! isPrimed()) {
				try {
					primeForReading();
				} catch (IOException e) {
					throw new RuntimeException(e.getMessage());
				}
			}
			
			if (currentLine == null || currentLine.trim().length()==0)
				return null;
			else {
				
				VariantRec rec = null;
				try {
					String contig = getContig();
					if (contig == null)
						return null;
					if (stripChr)
						contig = contig.replace("chr", "");
					//System.out.println(currentLine);
					String ref = getRef();
					String alt = getAlt();
					int start = getStart();
					int end = ref.length();
					

					//Remove initial characters if they are equal (across all alt alleles) and add that many bases to start position
					//Warning: Indels may no longer be left-aligned after this procedure
					if (stripInitialMatchingBases) {
						String[] altToks = alt.split(",");
						int altCount = altToks.length;
						int matches = findNumberOfInitialMatchingBases(ref, alt);						
						if (matches > 0) {	
							// Trim Ref
							ref = ref.substring(matches);
							if (ref.length()==0) {
								ref = "-";
							}
							// Trim Alt Alleles
							String TrimmedAlt = new String();
							for (int i=0; i< altCount; i++) {	
								altToks[i] = altToks[i].substring(matches); 
								if (i!=0){
									TrimmedAlt += ",";
								}
								if (altToks[i].length()==0){								
									TrimmedAlt += "-";
								} 
								else {
									TrimmedAlt += altToks[i];
								}								
							}
							alt = TrimmedAlt;
							
							//Update start position
							start+=matches;
							
							//Update end position
							if (ref.equals("-")) {
								end = start;
							}
							else {
								end = start + ref.length();
							}								
						}
					}

					rec = new VariantRec(contig, start, end,  ref, alt, getQuality(), isHetero() );
					Integer depth = getDepth();
					if (depth != null)
						rec.addProperty(VariantRec.DEPTH, new Double(depth));

					Integer altDepth = getVariantDepth();
					if (altDepth != null) {
						rec.addProperty(VariantRec.VAR_DEPTH, new Double(altDepth));
					}

					if (rec.isMultiAllelic()) {
						Integer altDepth2 = getVariantDepth(1);
						if (altDepth2 != null) {
							rec.addProperty(VariantRec.VAR2_DEPTH, new Double(altDepth2));
						}
					}
					
					Double genotypeQuality = getGenotypeQuality();
					if (genotypeQuality != null) 
						rec.addProperty(VariantRec.GENOTYPE_QUALITY, genotypeQuality);

					Double vqsrScore = getVQSR();
					if (vqsrScore != null)
						rec.addProperty(VariantRec.VQSR, vqsrScore);

					Double fsScore = getStrandBiasScore();
					if (fsScore != null)
						rec.addProperty(VariantRec.FS_SCORE, fsScore);
					
					Double rpScore = getRPScore();
					if (rpScore != null)
						rec.addProperty(VariantRec.RP_SCORE, rpScore);
					
					
					Double logFSScore = getLogFSScore();
					if (logFSScore != null)
						rec.addProperty(VariantRec.LOGFS_SCORE, logFSScore);
					
					if (parseAllInfoTokens) {
						addAllInfoTokens(rec);
					}
				}
				catch (Exception ex) {
					System.err.println("ERROR: could not parse variant from line : " + currentLine + "\n Exception: " + ex.getCause() + " " + ex.getMessage());
					
					return null;
				}
				return rec;
			}
		}
		

		
		/**
		 * Add all info items as properties / annotations. This is likely to be kinda slow. 
		 * @param rec
		 */
		private void addAllInfoTokens(VariantRec rec) {
			String[] infoToks = lineToks[7].split(";");
			for(int i=0; i<infoToks.length; i++) {
				String[] bits = infoToks[i].split("=");
				if (bits.length == 2) {
					
					try {
						double val = Double.parseDouble(bits[1]);
						rec.addProperty(bits[0], val);
						continue;
					}
					catch (NumberFormatException ex) {
						//no sweat
					}
					rec.addAnnotation(bits[0], bits[1]);
				}
			}
		}

		/**
		 * Read one more line of input, returns false if line cannot be read
		 * @return
		 * @throws IOException
		 */
		public boolean advanceLine() throws IOException {
			currentLine = reader.readLine();
			while(currentLine != null && currentLine.startsWith("#")) {
				currentLine = reader.readLine();
				currentLineNumber++;
			}
			
			if (currentLine == null)
				lineToks = null;
			else
				lineToks = currentLine.split("\\t");

			return currentLine != null;
		}

		/**
		 * Returns true if the current line is not null. 
		 * @return
		 */
		public boolean hasLine() {
			return currentLine != null;
		}
		
		public String getContig() {
			if (lineToks != null) {
				return lineToks[0];
			}
			else
				return null;
		}
		
		/**
		 * Return the (starting) position item for current line
		 * @return
		 */
		public int getPosition() {
			if (lineToks != null) {
				return Integer.parseInt(lineToks[1]);
			}
			else
				return -1;
		}
		
		/**
		 * Read depth from INFO column, tries to identify depth by looking for a DP string, then reading
		 * the following number
		 * @return
		 */
		public Integer getDepth() {
			if (lineToks != null) {
				String info = lineToks[7];
			
				String target = "DP";
				int index = info.indexOf(target);
				if (index < 0) {
					//Attempt to get DP from INFO tokens...
					Integer dp = getDepthFromInfo();
					return dp;
				}
			
				//System.out.println( info.substring(index, index+10) + " ...... " + info.substring(index+target.length()+1, info.indexOf(';', index)));
				try {
					Integer value = Integer.parseInt(info.substring(index+target.length()+1, info.indexOf(';', index)));
					return value;
				}
				catch (NumberFormatException nfe) {
					//Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not parse depth from vcf line: " );
				}
				return null;
			}
			else {
				return -1;
			}
		}
		

		
		public int getStart() {
			return getPosition();
		}
		
		/**
		 * Return the end of this variant
		 * @return
		 */
		public int getEnd() {
			if (lineToks != null) {
				return Integer.parseInt(lineToks[2]);
			}
			else
				return -1;
		}
		
		public Double getQuality() {
			if (lineToks != null) {
				try {
					return Double.parseDouble(lineToks[5]);
				}
				catch (NumberFormatException nfe) {
					return -1.0;
				}
			}
			else
				return -1.0;
		}
		
		public String getRef() {
			if (lineToks != null) {
				return lineToks[3];
			}
			else
				return "?";
		}
		
		public String getAlt() {
			if (lineToks != null) {
				return lineToks[4];
			}
			else
				return "?";
		}
		
		public int getLineNumber() {
			return currentLineNumber;
		}
		
		protected void updateFormatIfNeeded() {
			if (lineToks.length > 7) {
				if (formatToks == null) {
					createFormatString(lineToks);
				}
				else {
					if (! currentFormatStr.equals(lineToks[8]))
						createFormatString(lineToks);
				}
			}
		}
		
		/**
		 * 
		 */
		public boolean isHetero() {
			if (lineToks != null) {
			
				updateFormatIfNeeded();
			
				if (formatToks == null)
					return false;
				if (gtCol < 0) {
					return false;
				}
			
				String[] formatValues = lineToks[sampleColumn].split(":");
				String GTStr = formatValues[gtCol];
			
				if (GTStr.length() != 3) {
					throw new IllegalStateException("Wrong number of characters in string for is hetero... (got " + GTStr + ", but length should be 3)");
				}
				
				if (GTStr.charAt(1) == '/' || GTStr.charAt(1) == '|') {
					if (GTStr.charAt(0) != GTStr.charAt(2))
						return true;
					else
						return false;
				}
				else {
					throw new IllegalStateException("Genotype separator char does not seem to be normal (found " + GTStr.charAt(1) + ")");
				}
			}
			else {
				return false;
			}
			
		}
		
		public boolean isHomo() {
			return ! isHetero();
		}

		public String getCurrentLine() {
			return currentLine;
		}
		
		/**
		 * Returns true if the phasing separator is "|" and not "/" 
		 * @return
		 */
		public boolean isPhased() {
			if (lineToks != null) {
				updateFormatIfNeeded();
			
				if (formatToks == null)
					return false;
			
				String[] formatValues = lineToks[sampleColumn].split(":");
				String GTStr = formatValues[gtCol];
				if (GTStr.charAt(1) == '|') {
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		
		/**
		 * True if the first item in the genotype string indicates an 'alt' allele
		 * @return
		 */
		public boolean firstIsAlt() {
			updateFormatIfNeeded();
			if (formatToks == null)
				return false;
			
			String[] formatValues = lineToks[sampleColumn].split("\t");
			String GTStr = formatValues[gtCol];
			if (GTStr.charAt(0) == '1') {
				return true;
			}
			else {
				return false;
			}
		}
		
		/**
		 * True if the second item in the genotype string indicates an 'alt' allele
		 * @return
		 */
		public boolean secondIsAlt() {
			updateFormatIfNeeded();
			
			if (formatToks == null)
				return false;
			
			String[] formatValues = lineToks[sampleColumn].split(":");
			String GTStr = formatValues[gtCol];
			if (GTStr.charAt(2) == '1') {
				return true;
			}
			else {
				return false;
			}
		}
		
		/**
		 * Obtain the genotype quality score for this variant
		 * @return
		 */
		public Double getGenotypeQuality() {
			if (lineToks != null) {
			
				updateFormatIfNeeded();
			
				if (formatToks == null || gqCol < 0)
					return -1.0;
			
				String[] formatValues = lineToks[sampleColumn].split(":");
				String GQStr = formatValues[gqCol];
				try {
					Double gq = Double.parseDouble(GQStr);
					return gq;
				}
				catch (NumberFormatException ex) {
					System.err.println("Could not parse genotype quality from " + GQStr);
					return null;
				}
			}
			else {
				return -1.0;
			}			
		}
		
		private Double getVQSR() {
			String[] infoToks = lineToks[7].split(";");
			for(int i=0; i<infoToks.length; i++) {
				String tok = infoToks[i];
				if (tok.startsWith("VQSLOD=")) {
					Double val = Double.parseDouble(tok.replace("VQSLOD=", ""));
					return val;
				}
			}
					
			return null;
		}
		
		private Double getLogFSScore() {
			String[] infoToks = lineToks[7].split(";");
			for(int i=0; i<infoToks.length; i++) {
				String tok = infoToks[i];
				if (tok.startsWith("LOGFS=")) {
					Double val = Double.parseDouble(tok.replace("LOGFS=", ""));
					return val;
				}
			}
			return null;
		}

		private Double getRPScore() {
			String[] infoToks = lineToks[7].split(";");
			for(int i=0; i<infoToks.length; i++) {
				String tok = infoToks[i];
				if (tok.startsWith("ReadPosRankSum=")) {
					Double val = Double.parseDouble(tok.replace("ReadPosRankSum=", ""));
					return val;
				}
			}
			return null;
		}
		
		private Double getTauFPScore() {
			String[] infoToks = lineToks[7].split(";");
			for(int i=0; i<infoToks.length; i++) {
				String tok = infoToks[i];
				if (tok.startsWith("TAUFP=")) {
					Double val = Double.parseDouble(tok.replace("TAUFP=", ""));
					return val;
				}
			}
					
			return null;
		}
		
		
		private Double getStrandBiasScore() {
			String[] infoToks = lineToks[7].split(";");
			for(int i=0; i<infoToks.length; i++) {
				String tok = infoToks[i];
				if (tok.startsWith("FS=")) {
					Double val = Double.parseDouble(tok.replace("FS=", ""));
					return val;
				}
			}
					
			return null;

		}

		/**
		 * Depth may appear in format OR INFO fields, this searches the latter for depth
		 * @return
		 */
		public Integer getDepthFromInfo() {
			updateFormatIfNeeded();
			
			if (formatToks == null)
				return 1;
			
			if (dpCol < 0)
				return null;
			
			String[] formatValues = lineToks[sampleColumn].split(":");
			String dpStr = formatValues[dpCol];
			return Integer.parseInt(dpStr);
		}
		
		
		/**
		 * Returns the depth of the first variant allele, as parsed from the INFO string for this sample
		 * @return
		 */
		public Integer getVariantDepth() {
			if (lineToks != null) {
				return getVariantDepth(0);
			} 
			else {
				return -1;
			}
			
		}
		
		/**
		 * Returns the depth of the which variant allele, as parsed from the INFO string for this sample
		 * @return
		 */
		public Integer getVariantDepth(int which) {
			if (lineToks != null) {
				updateFormatIfNeeded();
			
				if (formatToks == null)
					return -1; 
			
				//if adCol specified (from GATK), use it. Otherwise, try aoCol (from IonTorrent or FreeBayes). If there's not that either, return null;
				boolean depthFromAD = true;
				int colIndex = adCol;
				if (colIndex < 0) {
					depthFromAD = false;
					colIndex = aoCol;
				}
				if (colIndex < 0)
					return null;
				
				//Confusing logic below to parse var depth (alt depth) from both GATK and IonTorrent-style vcfs...
				String[] formatValues = lineToks[sampleColumn].split(":");
				String adStr = formatValues[colIndex];
				try {
					String[] depths = adStr.split(",");
					if (depthFromAD) {
						if (depths.length==1) 
							return 0;
						else 
							return Integer.parseInt(depths[which+1]); 
					}
					else {
						Integer altReadDepth = Integer.parseInt(depths[which]);
						return altReadDepth;
					}
				
				}
				catch (NumberFormatException ex) {
					System.err.println("Could not parse alt depth from " + adStr);
					return null;
				}
			}
			else {
				return -1;
			}										
		}
		
		/**
		 * Create the string array representing elements in the 'format' column, which
		 * we assume is always column 8. Right now we use this info to figure out which portion
		 * of the format string correspond to the genotype, genotype quality, depth, alt depth, etc. parts.
		 * Probably switching to a map implementation would be better someday. 
		 */
		private void createFormatString(String[] toks) {
			if (toks.length <= 8) {
				formatToks = null;
				return;
			}
			
			String formatStr = toks[8];
			
			formatToks = formatStr.split(":");
			for(int i=0; i<formatToks.length; i++) {
				if (formatToks[i].equals("GT")) {
					gtCol = i;
				}
				if (formatToks[i].equals("GQ")) {
					gqCol = i;
				}
				
				if (formatToks[i].equals("AD")) {
					adCol = i;
				}
				if (formatToks[i].equals("DP")) {
					dpCol = i;
				}
				if (formatToks[i].equals("AO")) {
					aoCol = i;
				}
				if (formatToks[i].equals("FAO")) {
					faoCol = i;
				}
				if (formatToks[i].equals("FDP")) {
					fdpCol = i;
				}

			}
			
			currentFormatStr = formatStr;
		}

		public boolean isStripInitialMatchingBases() {
			return stripInitialMatchingBases;
		}

		public void setStripInitialMatchingBases(boolean stripInitialMatchingBases) {
			this.stripInitialMatchingBases = stripInitialMatchingBases;
		}
		
		//Pipeline Object implementation : Currently, don't do anything
		
		@Override
		public void setAttribute(String key, String value) {
			attributes.put(key, value);
		}

		@Override
		public String getAttribute(String key) {
			return attributes.get(key);
		}

		@Override
		public Collection<String> getAttributeKeys() {
			return attributes.keySet();
		}

		@Override
		public void initialize(NodeList children) {
			
			for(int i=0; i<children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					Element el = (Element)child;
					PipelineObject obj = getObjectFromHandler(el.getNodeName());
					if (obj instanceof VCFFile) {
						VCFFile inputVCF = (VCFFile)obj;
						try {
							setInputStream(new FileInputStream(inputVCF.getFile()));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				}
			}
			
			
			//Get name of input file...
			String fileName = this.getAttribute("filename");

			if (fileName != null && reader != null) {
				//hmm reader initialized but user has also specified filename...
				throw new IllegalArgumentException("Cannot specify a filename and an input file object at the same time");
			}
			
			if (reader == null && fileName != null) {

				//TODO support for sample, codec, etc. 

				File inputFile = null;
				if (fileName.startsWith("/")) {
					inputFile = new File(fileName);
				}
				else {
					inputFile = new File(getProjectHome() + "/" + fileName);
				}

				if (! inputFile.exists()) {
					throw new IllegalArgumentException("Input file " + inputFile.getAbsolutePath() + " does not exist");
				}

				try {
					this.setInputStream(new FileInputStream(inputFile));
				} catch (IOException e) {
					throw new IllegalArgumentException("Error opening input file " + inputFile.getAbsolutePath() + ": " + e.getMessage());
				}
			}
		}
		
		private Map<String, String> attributes = new HashMap<String, String>();




}
