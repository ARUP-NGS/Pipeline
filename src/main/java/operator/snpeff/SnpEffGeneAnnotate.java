package operator.snpeff;

//import htsjdk.tribble.readers.TabixReader;
//import htsjdk.tribble.readers.TabixReader.Iterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NodeList;

import buffer.ArupBEDFile;
import buffer.ArupBEDFile.ARUPBedInterval;
import buffer.variant.VariantRec;
import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import util.Interval;

//So far I have edited 
	//	SnpEffGeneAnnotate.java major changes: 
	//		uses attribute transcripts.from.arupbed = true to use transcripts from arupBedFile input rather than preferred nm list
	//		if var intersects with intervals, annotations with the transcripts from those intervals will be used
	//		force snpEff to only use transcripts from the ArupBedFile (faster, filters most annotations we don't want)
	//	ArupBEDFile.java new extends BEDFile, uses info property in Interval class to store transcripts
	//	Annotator.java now looks for ArupBEDFile as input and builds interval map if found
	//	Interval.java added getter for info property
	//	IntervalsFile.java added intersectsWhich and nearest methods for a query interval input
	//	VariantRec.java added annotations for 2nd and 3rd annotation
	//	VarViewerWriter.java added annotations for 2nd and 3rd annotation

//TODO in SnpEff annotator, if bed trs don't match any snpEff trs throw error
//TODO annotate with all transcripts available from ArupBedFile.nearest search. 
//TODO have nearest send back ranked list
//TODO test using nicu bed file to show all possibilities of bed file
//TODO add proper comments
//TODO add test mods?

//NOTE currently full ArupBEDFile loaded into an interval map
//		if this becomes too large (for exome/genome?) we could possibly load one chrom at a time

/**
 * Uses SnpEff to provide gene annotations for the variants given 
 * @author brendan
 *
 */
public class SnpEffGeneAnnotate extends Annotator {

	public static final String NM_DEFS = "nm.Definitions";
	public static final String SNPEFF_DIR = "snpeff.dir";
	public static final String SNPEFF_GENOME = "snpeff.genome";
	public static final String PERFORM_MITO_SUB = "perform.mito.sub";	
	public static final String UPDOWNSTREAM_LENGTH = "updownstream.length";
	public static final String SPLICESITE_SIZE = "spliceSite.size";
	public static final String ALT_JAVA_HOME = "alt.java.home";
	public static final String TRANSCRIPTS_FROM_ARUPBED = "transcripts.from.arupbed";


	protected String javaHome = null;
	protected String altJavaHome = null;
	protected String snpEffDir = null;
	protected String snpEffGenome = null;
	protected int updownStreamLength = 1000;
	protected int spliceSiteSize = 10;
	protected boolean performMitoSub = false; //If true, do a substitution of mito chr name to 
	protected boolean trsFromArupBed = false; //if true, expect ArupBEDFile input with transcripts in 4th column
	
	private Map<String, List<SnpEffInfo> > annos = null;
	private Map<String, String> nmMap = new HashMap<String, String>();
	
	public void prepare() {
		
		//First we have to build an input file
		File input = new File(this.getProjectHome() + "/snpeff.input.vcf");
		File outputFile = new File(this.getProjectHome() + "/snpeff.output");
		int varsWritten = 0;
		
		try {
			input.createNewFile();
			outputFile.createNewFile();

			BufferedWriter writer = new BufferedWriter(new FileWriter(input));
			writer.write("##fileformat=VCFv4.1\n");
			writer.write("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	sample\n");
			
				for(String contig: variants.getContigs()) {
					for(VariantRec rec: variants.getVariantsForContig(contig)) {
						Integer recLength = rec.getRef().length() - rec.getAlt().length(); //if >0 Indicates a deletion
						if (recLength < 0) { //Indicates that it is an insertion
							recLength = 0; 
						} else if (recLength == 0) { //Indicates that it is an snv or mnv
							if (rec.getRef().length() == 1) { // Indicates that it is an snv
								recLength = 1;
							} else { // Indicates that it is an mnv
								recLength = rec.getRef().length();
							}
						}
						Integer recEnd = rec.getStart() - 1 + recLength;
						Interval recInterval = new Interval(rec.getStart() - 1, recEnd);
						if (bedFile == null || bedFile.intersects(rec.getContig(), recInterval)) {
							String varStr = convertVar(rec);		
							writer.write(varStr + "\n");
							varsWritten++;
						}
					}
				}

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Next, make file of unique set of transcripts. Only these transcripts will be used by snpEff (-onlyTr option)
		//(only if we are using nms from an ARUP BED file)
		String onlyTr = null;
		if (trsFromArupBed) {
			onlyTr = this.getProjectHome() + "/snpeff_onlyTr.txt";
			try {
				onlyTrBuild(onlyTr, arupBedFile);
			} catch (OperationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}

		//Next, run snpeff using the input file we just made
		String command = javaHome + " -Xmx16g -jar " + snpEffDir + "/snpEff.jar -c " + snpEffDir + 
				"/snpEff.config " + snpEffGenome + " -hgvs -nostats -ud " + updownStreamLength + 
				" -spliceSiteSize " + spliceSiteSize + " " + input.getAbsolutePath();
		// add additional -onlyTr option if we are using transcripts from an ARUP BED file
		if (trsFromArupBed) {
			command = command + " -onlyTr " + onlyTr;
		}
		Logger.getLogger(Pipeline.primaryLoggerName).info("Executing command: " + command);
		try {
			executeCommandCaptureOutput(command, outputFile);
		} catch (OperationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Finally, parse the output file and read it into memory so we can quickly annotate the variants
		//If there are tons and tons of variants, this might break. 
		annos = new HashMap<String, List<SnpEffInfo>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(outputFile));
			String line = reader.readLine();
			while(line != null) {
				if (line.length() ==0 || line.startsWith("#")) {
					line = reader.readLine();
					continue;
				}
				List<SnpEffInfo> newInfos = parseOutputLineVCF(line);
				String[] toks = line.split("\t");
				
				String varKey = convertVar(toks[0], Integer.parseInt(toks[1]),toks[3], toks[4]);
				List<SnpEffInfo> infos = annos.get(varKey);
				if (infos == null) {
					infos = new ArrayList<SnpEffInfo>();
					annos.put(varKey, newInfos);
				} else {
					infos.addAll(newInfos);
				}
				
				line = reader.readLine();
			}

			reader.close();
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		
		
		if (annos == null) {
			throw new OperationFailedException("Map not initialized", this);
			
		}
		
		String varStr = convertVar(var);
		
		String[] allAlts = varStr.split("\n");
		
		//crashes if one of the alts has no annotations
		for(String alt : allAlts) {
			List<SnpEffInfo> annoList = annos.get(alt);
			if (annoList == null) {
				throw new OperationFailedException("No annotation info found for " + varStr, this);
			}
		
			//use annos from any of the alts to choose highest ranking
			try {
				annotateFromList(var, annoList);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void annotateFromList(VariantRec var, List<SnpEffInfo> infoList) throws IOException, OperationFailedException {
	// inputs are a var and list of associated snpefff annotations
		
		if (infoList == null || infoList.size() == 0) {
			return;
		}
		
		boolean hasPreferredNM = false; //can use this to throw error if transcripts for this region not seen in infoList
		boolean isUsingPreferredNM = false;
		ArrayList<Integer> nearestIndexes = null;
		LinkedHashSet<String> varTrs = new LinkedHashSet<String>();
		

		if (trsFromArupBed) {
			//get var genomic position for query.
			String varContig = var.getContig();
			int varStart = var.getStart();
			int varRefLength = var.getRef().length();
			int varEnd;
			if (varRefLength == 0) varRefLength = 1;
			varEnd = varStart + varRefLength - 1;
			Interval varInterval = new Interval(varStart, varEnd);
			
			//get all transcripts from this var's position
			//make a "prioritized" transcript list (first in list takes priority)
			nearestIndexes = arupBedFile.nearest(varContig, varInterval);
			
			List<Interval> cInts = arupBedFile.getIntervalsForContig(varContig);
			for (Integer idx : nearestIndexes) {
				Interval idxInter = cInts.get(idx);
				String[] idxTrs = (String[]) idxInter.getInfo();
				for (String tr : idxTrs) {
						varTrs.add(tr);
				}
			}

			//make a prioritized list of infos to output as annotations
			ArrayList<SnpEffInfo> annoResults = new ArrayList<SnpEffInfo>(); //infos for output in decending order of importance

			//try to find top ranking snpEff annotation for each transcript	
			for (String tr : varTrs) { 
				int topRank = -1;
				SnpEffInfo topHit = null;
				
				for (SnpEffInfo infoi : infoList) {
					if (tr.equals(infoi.transcript)) {
						int ranki = calculateRank(infoi.changeType);
						if (ranki > topRank) {
							topRank = ranki;
							topHit = infoi;
						}
					}
				}
				//if we found an info for this transcript
				//flesh the info out and add it to the annoResults list
				//TODO do we want to keep this section below that looks at different cdots/pdots or accept the top ranking for given transcript
				if (topHit != null) {
					SnpEffInfo infoForAnno = new SnpEffInfo();
					infoForAnno.exon = topHit.exon;
					infoForAnno.transcript = topHit.transcript;
					infoForAnno.changeType = topHit.changeType;
					infoForAnno.gene = topHit.gene;
					loadBestCPDot(infoForAnno, infoList);
					annoResults.add(infoForAnno);
					annoResults.add(topHit);
				}

			}
			
			//TODO Currently only annotating with top 3 ranked info, add additional annotations for lower ranked annotations
			//if no matching infos found, throw error for now
			//(or just choose next best info?????)
			if (annoResults.size() > 0) {
				appendAnnotation(var, VariantRec.CDOT, annoResults.get(0).cDot);
				appendAnnotation(var, VariantRec.PDOT, annoResults.get(0).pDot);
				appendAnnotation(var, VariantRec.EXON_NUMBER, annoResults.get(0).exon);
				appendAnnotation(var, VariantRec.NM_NUMBER, annoResults.get(0).transcript);
				appendAnnotation(var, VariantRec.GENE_NAME, annoResults.get(0).gene);
				appendAnnotation(var, VariantRec.VARIANT_TYPE, annoResults.get(0).changeType.replace("_CODING", ""));
				//add second annotation if it exists
				if (annoResults.size() >= 2) {
					appendAnnotation(var, VariantRec.CDOT2, annoResults.get(1).cDot);
					appendAnnotation(var, VariantRec.PDOT2, annoResults.get(1).pDot);
					appendAnnotation(var, VariantRec.EXON_NUMBER2, annoResults.get(1).exon);
					appendAnnotation(var, VariantRec.NM_NUMBER2, annoResults.get(1).transcript);
					appendAnnotation(var, VariantRec.GENE_NAME2, annoResults.get(1).gene);
					appendAnnotation(var, VariantRec.VARIANT_TYPE2, annoResults.get(1).changeType.replace("_CODING", ""));
				}
				//add third annotation if it exists
				if (annoResults.size() >= 3) {
					appendAnnotation(var, VariantRec.CDOT3, annoResults.get(3).cDot);
					appendAnnotation(var, VariantRec.PDOT3, annoResults.get(3).pDot);
					appendAnnotation(var, VariantRec.EXON_NUMBER3, annoResults.get(3).exon);
					appendAnnotation(var, VariantRec.NM_NUMBER3, annoResults.get(3).transcript);
					appendAnnotation(var, VariantRec.GENE_NAME3, annoResults.get(3).gene);
					appendAnnotation(var, VariantRec.VARIANT_TYPE3, annoResults.get(3).changeType.replace("_CODING", ""));
				}

			} else {
				var.addAnnotation(VariantRec.NON_PREFERRED_TRANSCRIPT, "true");
				throw new OperationFailedException("No ArupBedFile transcript found in snpEff variant region: " + var.getContig() + ":" + var.getStart(), this);
				//System.out.println("No ArupBedFile transcript found in snpEff variant region");

				//System.out.println("no anno var-info............... " + var.getContig() + ":" + var.getStart() + ":" + var.getAlt() + " varTrs are:");
				//System.out.println("nearestIndexes........ ");
				//for (Integer ni : nearestIndexes) {
				//	System.out.println(ni + ", ");
				//}

				//System.out.println("no anno var-info............... " + var.getContig() + ":" + var.getStart() + ":" + var.getAlt() + " varTrs are:");
				//System.out.println(var.getContig() + ":" + var.getStart() + ":" + var.getAlt());
				//System.out.println("varTrs........ ");
				//for (String tr : varTrs) {
				//	System.out.println(tr + ", ");
				//}
				//System.out.println("infoList........ ");
				//for (SnpEffInfo infoi : infoList) {
				//	System.out.println(infoi.transcript + ", ");
				//}
				//System.out.println("nearestIndexes........ ");
				//for (Integer ni : nearestIndexes) {
				//	System.out.println(ni + ", ");
				//}

			}

		} else {
		//not trsFromArupBed so use the old method
			SnpEffInfo topHit = infoList.get(0);
			int topRank = calculateRank(topHit.changeType);
			for (SnpEffInfo info : infoList) {
				int infoRank = calculateRank(info.changeType);

				// First check to see if it's in the nmMap. If so, it gets a
				// really high rank
				if (nmMap.containsKey(info.gene)) {
					hasPreferredNM = true;
					String preferredNM = nmMap.get(info.gene);
					if (info.transcript.startsWith(preferredNM)) {
						infoRank += 1000;
						isUsingPreferredNM = true;
						try {
							String specificNMFile = this.getAttribute(NM_DEFS);
							if (specificNMFile!=null) {
								if (this.getUserPreferredNMs(this.getAttribute(NM_DEFS)).containsKey(info.gene)) {
									infoRank += 9000; // This transcript is in the user specified preferred NMs. It's over 9000!!!!!
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
							throw new IllegalArgumentException("Could not read NMs file:  " + this.getAttribute(NM_DEFS));
						}
					}
				}
				if (infoRank > topRank) {
					topHit = info;
					topRank = infoRank;
				}
			}

			// based on the top hit, find the appropriate cdot
			// make map of infolist index vs cdot value but only for the transcript of top hit
			// choose cdot based on:
			// something is better than nothing
			// intronic or UTR is better than coding (because these will be most likely to be based on the reference transcript and not the variant)
			String bestCdot = "";
			String bestPdot = topHit.pDot;
			Pattern p = Pattern.compile("[\\+\\-\\*]");
			for(SnpEffInfo info : infoList) {
				if (info.transcript.equals(topHit.transcript) && info.cDot != null && ! info.cDot.equals("")) {
					Matcher bestMatch = p.matcher(bestCdot);
					Matcher nextMatch = p.matcher(info.cDot);
					if (nextMatch.find() && ! bestMatch.find()) {
						bestCdot = info.cDot;
						if (info.pDot != null && ! info.pDot.equals("")){
							bestPdot = info.pDot;
						}
					} else if (bestCdot.equals("")) {
						bestCdot = info.cDot;
						if (info.pDot != null && ! info.pDot.equals("")){
							bestPdot = info.pDot;
						}
					}
					if (bestPdot == null || bestPdot.equals("") && (info.pDot != null && ! info.pDot.equals(""))){
						bestPdot = info.pDot;
					}
				}
			}

			appendAnnotation(var, VariantRec.CDOT, bestCdot);
			appendAnnotation(var, VariantRec.PDOT, bestPdot);
			appendAnnotation(var, VariantRec.EXON_NUMBER, topHit.exon);
			appendAnnotation(var, VariantRec.NM_NUMBER, topHit.transcript);
			appendAnnotation(var, VariantRec.GENE_NAME, topHit.gene);
			appendAnnotation(var, VariantRec.VARIANT_TYPE, topHit.changeType.replace("_CODING", ""));
			if (hasPreferredNM && (!isUsingPreferredNM)) {
				var.addAnnotation(VariantRec.NON_PREFERRED_TRANSCRIPT, "true");
			}
		}
	}

	
	
	private void loadBestCPDot(SnpEffInfo hitInfo, List<SnpEffInfo> infoList) {
		String bestCdot = "";
		String bestPdot = hitInfo.pDot;
		Pattern p = Pattern.compile("[\\+\\-\\*]");
		for(SnpEffInfo info : infoList) {
			if (info.transcript.equals(hitInfo.transcript) && info.cDot != null && ! info.cDot.equals("")) {
				Matcher bestMatch = p.matcher(bestCdot);
				Matcher nextMatch = p.matcher(info.cDot);
				if (nextMatch.find() && ! bestMatch.find()) {
					bestCdot = info.cDot;
					if (info.pDot != null && ! info.pDot.equals("")){
						bestPdot = info.pDot;
					}
				} else if (bestCdot.equals("")) {
					bestCdot = info.cDot;
					if (info.pDot != null && ! info.pDot.equals("")){
						bestPdot = info.pDot;
					}
				}
				if (bestPdot == null || bestPdot.equals("") && (info.pDot != null && ! info.pDot.equals(""))){
					bestPdot = info.pDot;
				}
			}
		}
		hitInfo.cDot = bestCdot;
		hitInfo.pDot = bestPdot;
	}
			
	
	private static int calculateRank(String changeType) {
		if (changeType == null || changeType.length()==0) {
			return 0;
		}
		
		if (VarEffects.effects.containsKey(changeType.trim())) {
			return VarEffects.effects.get(changeType.trim());
		} else {
			return 6;
		}
	}
	
	private static String convertVar(String chr, int pos, String ref, String alt) {
		String cont = chr;
		if (alt.contains(",")) {
			String var1 = convertVar(chr, pos, ref, alt.split(",", 2)[0]);
			String var2 = convertVar(chr, pos, ref, alt.split(",", 2)[1]);
			return var1 + "\n" + var2;
		}
		
		if (cont.equals("M") || cont.equals("chrM") || cont.equals("MT") || cont.equals("chrMT")) {
			cont = "NC_012920";
		}

		if (ref.equals("*")) {
			ref = "-";
		}
		alt = alt.replace("+", "");
		alt = alt.replace("DEL", "-");
		
		
		
		if (alt.equals("-")) {
			ref = ref.replace("-", "");
			return cont + "\t" + (pos-1) + "\t.\tG" + ref + "\tG\t.\t.\t.";
		} else {
			if (ref.equals("-")) {
				return cont + "\t" + (pos-1) + "\t.\tG\tG" + alt + "\t.\t.\t.";
			} else {
				return cont + "\t" + pos + "\t.\t" + ref + "\t" + alt + "\t.\t.\t.";	
			}
		}		
	}
	
	
	
	/**
	 * Convert the variant info a form that works with SnpEff parsing
	 * @param rec
	 * @return
	 */
	private static String convertVar(VariantRec rec) {
		return convertVar(rec.getContig(), rec.getStart(), rec.getRef(), rec.getAlt());
	}
	
	/**
	 * 
	 * ##INFO=<ID=EFF,Number=.,Type=String,Description="Predicted effects for this variant.Format: 'Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )' ">
	 *  0: Effect_Impact 
	 *  1: Functional_Class 
	 *  2: Codon_Change 
	 *  3: Amino_Acid_Change
	 *  4: Amino_Acid_length 
	 *  5: Gene_Name
	 *  6: Transcript_BioType 
	 *  7: Gene_Coding 
	 *  8: Transcript_ID 
	 *  9: Exon_Rank  
	 *  10: Genotype_Number 
	 *  11: ERRORS 
	 *  12: WARNINGS 
	 * @param line
	 * @return
	 */
	private List<SnpEffInfo> parseOutputLineVCF(String line) {
		String[] toks = line.split("\t");
		if (toks.length < 8) {
			return new ArrayList<SnpEffInfo>();
		}
		
		//SNPEFF stuff appears in the INFO field, that's column index 7
		String[] effs = toks[7].split(";");
		String eff = null;
		for(int i=0; i<effs.length; i++) {
			if (effs[i].startsWith("EFF=")) {
				eff = effs[i];
				break;
			}
		}
		
		
		List<SnpEffInfo> infoList;
		if (eff != null) {
			infoList = parseInfoFromToken(eff);
		}
		else {
			infoList = new ArrayList<SnpEffInfo>();
		}
		return infoList;	
	}
	
	private List<SnpEffInfo> parseInfoFromToken(String token) {
		List<SnpEffInfo> infos = new ArrayList<SnpEffInfo>();
		
		if (! token.startsWith("EFF=")) {
			throw new IllegalArgumentException("This doesn't look like a SnpEff info token.");
		}
		
		
		
		String[] toks = token.replace("EFF=", "").split(",");
		for(int i=0; i<toks.length; i++) {
			String tok = toks[i];
			int firstParenIndex = tok.indexOf("(");
			if (firstParenIndex <0) {
				continue;
			}
			
			SnpEffInfo info = new SnpEffInfo();
			String effect = tok.substring(0, firstParenIndex);
			tok = tok.replace("(", "").replace(")", "");
			String[] bits = tok.split("\\|");
			
			String cp = bits[3];
			String[] cpParts = cp.split("/");
			String cdot = "";
			String pdot = "";
			if (cp.length()>0) {
				if (cpParts.length == 1){
					cdot = cpParts[0];	
				}
				if (cpParts.length == 2){
					pdot = cpParts[0];
					cdot = cpParts[1];
				}
				
				
			}
			
			String gene = bits[5];
			String exonNum = bits[9];
			String transcriptID = bits[8];
			
			info.cDot = cdot;
			info.pDot = pdot;
			info.changeType = effect;
			info.exon = exonNum;
			info.gene = gene;
			info.transcript = transcriptID;
			infos.add(info);
		}
		
		return infos;
	}
	
	//build temp file of transcripts to be used by snpEff exclusively
	private void onlyTrBuild(String onlyTr, ArupBEDFile arupBedFile) throws OperationFailedException {

		//make unique list of transcripts from bed file by doing a line by line look at transcript column (4)
		LinkedHashSet<String> trs = new LinkedHashSet<String>();

		for (String contig : arupBedFile.getContigs()) {
			for (Interval inter : arupBedFile.getIntervalsForContig(contig)) {
				if (inter.getInfo() instanceof ARUPBedInterval) {
					for (String tr : ((ARUPBedInterval) inter.getInfo()).transcripts) {
						trs.add(tr);
						//System.out.println("another tr is: " + contig + ":" + inter.begin + "-" + tr);
					}
					
				} else {
					throw new OperationFailedException("ArupBEDFile Interval object has malformed info property", this);
				}
			}
		}

		File trFile = new File(onlyTr);
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(trFile));
			for (String tr : trs) {
				writer.write(tr + "\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Could not write temp trFile for SnpEff. " + e, this);
		}
	}

	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		altJavaHome = this.getAttribute(ALT_JAVA_HOME);
		if (altJavaHome == null) {
			javaHome = "java";
		}
		else {
			javaHome = altJavaHome;
		}
			
		snpEffDir = this.getPipelineProperty(SNPEFF_DIR);
		if (snpEffDir == null) {
			snpEffDir = this.getAttribute(SNPEFF_DIR);
			if (snpEffDir == null) {
				throw new IllegalArgumentException("No path to snpEff dir specified, use " + SNPEFF_DIR);
			}
		}
		
		snpEffGenome = this.getAttribute(SNPEFF_GENOME);
		if (snpEffGenome == null) {
			throw new IllegalArgumentException("No snpEff genome specified, use attribute " + SNPEFF_GENOME);
		}
		
		String performMitoSubStr = this.getAttribute(PERFORM_MITO_SUB);
		if (performMitoSubStr != null) {
			performMitoSub = Boolean.parseBoolean(performMitoSubStr);
		}
		
		String updownStreamStr = this.getAttribute(UPDOWNSTREAM_LENGTH);
		if (updownStreamStr != null) {
			updownStreamLength = Integer.parseInt(updownStreamStr);
		}

		String spliceSiteStr = this.getAttribute(SPLICESITE_SIZE);
		if (spliceSiteStr != null) {
			spliceSiteSize = Integer.parseInt(spliceSiteStr);
		}

		String useArupBed = this.getAttribute(TRANSCRIPTS_FROM_ARUPBED);
		if (useArupBed != null) {
			if (useArupBed.matches("TRUE|True|true")) {
				trsFromArupBed = true;
			} else if (! useArupBed.matches("FALSE|False|false")) {
				throw new IllegalArgumentException("Could not decipher "
						+ TRANSCRIPTS_FROM_ARUPBED + " value given: " + useArupBed);
			}
		}
		if (trsFromArupBed && arupBedFile == null) {
			throw new IllegalArgumentException(TRANSCRIPTS_FROM_ARUPBED
					+ " set true but no ArupBEDFile passed as input");
		}

		//only load and use preferred nms if not using nms from arupbed
		String nmDefs = this.getAttribute(NM_DEFS);
		if (trsFromArupBed && nmDefs != null) {
			throw new IllegalArgumentException("Can only choose one of " + TRANSCRIPTS_FROM_ARUPBED + ", and "  + NM_DEFS + " together");
		} else if (! trsFromArupBed ) {
			try {
				nmMap = loadPreferredNMs(nmDefs);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IllegalArgumentException("Could not read NMs file:  " +nmDefs);
			}
		} 
	}
	
	class SnpEffInfo {

		String changeType;
		String cDot;
		String pDot;
		String gene;
		String transcript;
		String exon;
	}
}





