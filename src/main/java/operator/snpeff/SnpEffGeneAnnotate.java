package operator.snpeff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.variant.VariantRec;

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
	
	protected String snpEffDir = null;
	protected String snpEffGenome = null;
	protected int updownStreamLength = 1000;
	protected int spliceSiteSize = 10;
	protected boolean performMitoSub = false; //If true, do a substitution of mito chr name to 
	
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



		//Next, run snpeff using the input file we just made
		String command = "java -Xmx8g -jar " + snpEffDir + "/snpEff.jar -c " + snpEffDir + "/snpEff.config " + snpEffGenome + " -hgvs -nostats -ud " + updownStreamLength + " -spliceSiteSize " + spliceSiteSize + " " + input.getAbsolutePath(); 
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
					infoRank = 1000;
					isUsingPreferredNM = true;
				}
			}
			
			if (infoRank > topRank) {
				topHit = info;
				topRank = infoRank;
			}
		}
		
		appendAnnotation(var, VariantRec.CDOT, topHit.cDot);
		appendAnnotation(var, VariantRec.PDOT, topHit.pDot);
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
		if (changeType.equals("INTERGENIC") || changeType.contains("UPSTREAM") || changeType.contains("DOWNSTREAM")) {
			return 0;
		}
		//Splice is bad since there's no CDot or PDot associated with it. Prefer INTRON instead.
				if (changeType.startsWith("SPLICE")) {
					return 0;
				}
		if (changeType.contains("UTR")) {
			return 1;
		}
		if (changeType.equals("INTRON")) {
			return 1;
		}
		if (changeType.equals("SYNONYMOUS_CODING")) {
			return 2;
		}
		if (changeType.startsWith("NON_SYNONYMOUS")) {
			return 3;
		}
		
		
		if (changeType.startsWith("START") || changeType.startsWith("STOP")) {
			return 5;
		}
		
		return 6;
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
	
	private Map<String,String> readNMMap(File file) throws IOException{
		BufferedReader br;
			br = new BufferedReader(new FileReader(file));
			String line;
			HashMap<String,String> nms = new HashMap<String,String>();
			
			while((line = br.readLine()) != null){
				if (line.length()==0)
					continue;
				
				String[] values = line.split("\t");
				if (values.length != 2) {
					Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not parse preferred NM# from line: " + line);
					continue;
				}
				nms.put(values[0].toUpperCase().trim(), values[1].toUpperCase().trim());
			}
			br.close();
			return nms;
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
		if (nmDefs != null) {
			File nmFile = new File(nmDefs);
			try {
				nmMap = readNMMap(nmFile);
			} catch (IOException e) {
				throw new IllegalArgumentException("Could not parse NM Defs file: " + e.getLocalizedMessage());
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
