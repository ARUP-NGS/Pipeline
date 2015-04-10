package operator.snpeff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import operator.snpeff.SnpEffGeneAnnotate.SnpEffInfo;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import util.Interval;
import buffer.variant.VariantRec;
import buffer.BEDFile;
import buffer.IntervalsFile;

/**
 * Uses SnpEff to provide gene annotations for the variants given
 * Breaks up annotation by chromosome
// * This should eventually be re-written to extend AbstractSerialAnnotator
 * @author daniel
 *
 */
public class SerialSnpEffGeneAnnotate extends Annotator {

	public static final String NM_DEFS = "nm.Definitions";
	public static final String SNPEFF_DIR = "snpeff.dir";
	public static final String SNPEFF_GENOME = "snpeff.genome";
	public static final String PERFORM_MITO_SUB = "perform.mito.sub";	
	public static final String UPDOWNSTREAM_LENGTH = "updownstream.length";
	public static final String SPLICESITE_SIZE = "spliceSite.size";
	public static final String MEMORY_STRING = "memory.string";
	
	protected String snpEffDir = null;
	protected String snpEffGenome = null;
	protected String memoryStr = "-Xmx8G";
	
	protected int updownStreamLength = 1000;
	protected int spliceSiteSize = 10;
	protected boolean performMitoSub = false; //If true, do a substitution of mito chr name to 
	
	private Map<String, List<SnpEffInfo> > annos = new HashMap<String, List<SnpEffInfo>>();
	private Map<String, String> nmMap = new HashMap<String, String>();
	Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
	
	public void prepare() {
		
		List<String> contigs = new ArrayList<String>(bedFile.getContigs());
		Collections.sort(contigs);
		File outputFile = new File(this.getProjectHome() + "/snpeff.output.vcf");
		
		ArrayList<File> inputs = new ArrayList<File>();
		ArrayList<File> outputs = new ArrayList<File>();
		for (String contig: contigs){
			ArrayList<File> inOut = prepareContig(contig);
			inputs.add(inOut.get(0));
			outputs.add(inOut.get(1));
		}
		
		//Run SnpEff and parse the output file and read it into memory so we can quickly annotate the variants
		//If there are tons and tons of variants, this might break.
		for(int i=0; i<inputs.size(); i++){
			runSnpEff(inputs.get(i), outputs.get(i));
			parseSnpEff(outputs.get(i));
		}

	}
	
	private ArrayList<File> prepareContig(String contig){
		int varsWritten = 0;
		File outputFile = new File(this.getProjectHome() + "/snpeff.output." + contig + ".vcf");
		File input = new File(this.getProjectHome() + "/snpeff.input." + contig + ".vcf");

		try {
			
			input.createNewFile();
			outputFile.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(input));
			writer.write("##fileformat=VCFv4.1\n");
			writer.write("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	sample\n");
			
					for(VariantRec rec: variants.getVariantsForContig(contig)) {
						System.out.println("variant ref = " + rec.getRef() + "  | variant alt = " + rec.getAlt());
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
		
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ArrayList<File> inOut = new ArrayList<File>();
			inOut.add(input);
			inOut.add(outputFile);
			return inOut;
		}
	
	private void runSnpEff(File input, File outputFile){
		//Next, run snpeff using the input file we just made
		String threadStr = " -t ";
		String command = "java " + memoryStr + " -jar " + snpEffDir + "/snpEff.jar -c " + snpEffDir + "/snpEff.config " + snpEffGenome + " -hgvs -nostats " + threadStr + "-ud " + updownStreamLength + " -spliceSiteSize " + spliceSiteSize + " " + input.getAbsolutePath(); 
		Logger.getLogger(Pipeline.primaryLoggerName).info("Executing command: " + command);
		try {
			
			executeCommandCaptureOutput(command, outputFile);
			
		} catch (OperationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void parseSnpEff(File outputFile){
		//Finally, parse the output file and read it into memory so we can quickly annotate the variants
		//If there are tons and tons of variants, this might break. 
		int annosPlaced = 0;
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
					annosPlaced+=1;
				} else {
					infos.addAll(newInfos);
					annosPlaced+=1;
				}
				
				line = reader.readLine();
			}
			logger.info("Total annotations added from this outputFile: " + String.valueOf(annosPlaced));

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
		
		for(String alt : allAlts) {
			List<SnpEffInfo> annoList = annos.get(alt);
			if (annoList == null) {
				throw new OperationFailedException("No annotation info found for " + varStr, this);
			}
		
			annotateFromList(var, annoList);
		}
		
	}
	
	private void annotateFromList(VariantRec var, List<SnpEffInfo> infoList) {
		
		if (infoList == null || infoList.size() == 0) {
			return;
		}
		
		boolean hasPreferredNM = false;
		boolean isUsingPreferredNM = false;
		
		SnpEffInfo topHit = infoList.get(0);
		int topRank = calculateRank(topHit.changeType);
		for(SnpEffInfo info : infoList) {
			
			int infoRank= calculateRank(info.changeType);
			
			//First check to see if it's in the nmMap. If so, it gets a really high rank
			if (nmMap.containsKey(info.gene)) {
				hasPreferredNM = true;
				String preferredNM = nmMap.get(info.gene);
				if (info.transcript.startsWith(preferredNM)) {
					infoRank += 1000;
					isUsingPreferredNM = true;
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
	
	
	private static int calculateRank(String changeType) {
		if (changeType == null || changeType.length()==0) {
			return 0;
		}
		
		if (VarEffects.effects.containsKey(changeType.trim())) {
			return VarEffects.effects.get(changeType.trim());
		} else {
			return 6;
		}
		
		// Previous code
		//if (changeType.contains("intergenic") || changeType.contains("upstream") || changeType.contains("downstream") || changeType.equals("INTERGENIC") || changeType.contains("UPSTREAM") || changeType.contains("DOWNSTREAM")) {
		//	return 0;
		//}
		//Splice is bad since there's no CDot or PDot associated with it. Prefer INTRON instead.
		//		if (changeType.startsWith("SPLICE") || changeType.startsWith("splice")) {
		//			return 3; // changed from 0 
		//		}
		//if (changeType.contains("UTR")) {   // UTR is capitalized in both snpEff 3.
		//	return 1;
		//}
		//if (changeType.equals("INTRON")) {
		//	return 1;
		//}
		//if (changeType.equals("SYNONYMOUS_CODING")) {
		//	return 2;
		//}
		//if (changeType.startsWith("NON_SYNONYMOUS")) {
		//	return 4; // changed from 3
		//}
		//
		//if (changeType.startsWith("START") || changeType.startsWith("STOP")) {
		//	return 5;
		//}
		//
		//return 6;
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
	
//	private SnpEffInfo parseOutputLineTXT(String line) {
//		String[] toks = line.split("\t");
//		SnpEffInfo info = new SnpEffInfo();
//		if (toks.length < 2) {
//			return info;
//		}
//		info.gene = toks[10];
//		info.transcript = toks[12];
//		info.exon = toks[14];
//		info.changeType = toks[15];
//		
//		info.pDot = "";
//		info.cDot = "";
//		if (!info.changeType.equals("INTERGENIC") && toks.length>16) {
//			if (toks[16].length()>0) {
//				String[] cp = toks[16].split("/");
//				if (cp.length > 1) {
//					info.pDot = cp[0];
//					info.cDot = cp[1];
//				}
//				else {
//					info.cDot = toks[16];
//				}
//			}	
//		}
//		
//		
//		return info;
//	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
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
		
		String nmDefs = this.getAttribute(NM_DEFS);
		try {
			nmMap = loadPreferredNMs(nmDefs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalArgumentException("Could not read NMs file:  " +nmDefs);
		}
		
		String memoryAttr = this.getAttribute(MEMORY_STRING);
		if(memoryAttr == null)
			memoryAttr = this.getPipelineProperty(MEMORY_STRING);
		if(memoryAttr != null)
			memoryStr = memoryAttr;
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

