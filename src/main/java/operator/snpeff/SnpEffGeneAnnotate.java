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

import json.JSONArray;
import json.JSONException;
import json.JSONObject;

import org.w3c.dom.NodeList;

import buffer.ArupBEDFile;
import buffer.ArupBEDFile.ARUPBedIntervalInfo;
import buffer.variant.VariantRec;
import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import util.Interval;


/**
 * Uses SnpEff to provide gene annotations for the variants given 
 * @author brendan, modified by chrisk
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
							String varStr = convertVarWithInfoEND(rec);		
							writer.write(varStr + "\n");
							varsWritten++;
						}
					}
				}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Next, run snpeff using the input file we just made (--formatEff tag used for v4.0 output standards while running v4.2)
		String command = javaHome + " -Xmx16g -jar " + snpEffDir + "/snpEff.jar -c " + snpEffDir + 
				"/snpEff.config " + snpEffGenome + " -hgvs -nostats -ud " + updownStreamLength + 
				" " + input.getAbsolutePath();
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Executing command: " + command);
		try {
			executeCommandCaptureOutput(command, outputFile);
		} catch (OperationFailedException e) {
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
				
				String varKey = convertVar(toks[0], Integer.parseInt(toks[1]),toks[3], toks[4], findInfoEndFromVCF(toks[7]));
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
			e.printStackTrace();
		}

	}
	
	/**
	 * InfoToks should be field 7 from a VCF record (the INFO column), we split this into tokens on ; and search for the END field,
	 * If found return it, otherwise return '.'
	 * In general, this must match the output format of convertVariantFromINFOEnd
	 * @return Either the END entry from the INFO field, or "." if no END found
	 */
	private String findInfoEndFromVCF(String infotoks) {
		for(String tok : infotoks.split(";")) {
			if (tok.startsWith("END=")) {
				return tok;
			}
		}
		return ".";
	}
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		
		if (annos == null) {
			throw new OperationFailedException("Map not initialized", this);
			
		}
		
		String varStr = convertVarWithInfoEND(var);
		String[] allAlts = varStr.split("\n");
		
		//crashes if one of the alts has no annotations
		for(String alt : allAlts) {
			List<SnpEffInfo> annoList = annos.get(alt);
			if (annoList == null) {
				throw new OperationFailedException("No annotation info found the alt " + alt + " for " + varStr, this);
			}
		
			//use annos from any of the alts to choose highest ranking
			try {
				annotateFromList(var, annoList);
			} catch (IOException e) {
				throw new OperationFailedException(e.getMessage(), this);
			}
		}
		
	}
	/**
	 * This builds the json object where each snpeff annotation is held, then adds all annotations to the Variant Rec as a string.
	 * @param var
	 * @param infoList
	 * @throws IOException
	 * @throws OperationFailedException
	 */
	private void annotateFromList(VariantRec var, List<SnpEffInfo> infoList) throws IOException, OperationFailedException {
		if (infoList == null || infoList.size() == 0) {
			return;
		}

		// First add the GENE_NAME annotation to the VariantRec var
		// This is needed for other variant annotations that are based on the gene
		// Only adds the top ranking gene from all snpeff annotations for the variant alts
		
		var.addAnnotation(VariantRec.GENE_NAME, infoList.get(0).gene);
		
		JSONArray masterlist = new JSONArray();

		String tempstring="";
		for (SnpEffInfo info : infoList) {
			JSONObject snpeffannos = new JSONObject();
			JSONObject mastersnpeffannos = new JSONObject();

			try {
				snpeffannos.put("cdot",info.cDot);
				snpeffannos.put("pdot",info.pDot);
				snpeffannos.put("exon.number",info.exon);
				snpeffannos.put("gene",info.gene);
				snpeffannos.put("variant.type", info.changeType);
				snpeffannos.put("warning",info.warning);
				snpeffannos.put("impact",info.impact);
				tempstring = info.transcript;
				if(!mastersnpeffannos.has(tempstring)){
					mastersnpeffannos.put(tempstring,snpeffannos);
				}

			} catch (JSONException e) {
				throw new OperationFailedException("JSONException adding variant " + var + " -> " + e.getLocalizedMessage(), this);
			}
			
			masterlist.put(mastersnpeffannos);//in case an array is needed instead of string rep of json			
		}
		
		appendAnnotationJSON(var, VariantRec.SNPEFF_ALL, masterlist);
	}

	
	private static String convertVar(String chr, int pos, String ref, String alt, String infoField) {
		String cont = chr;
		if (alt.contains(",")) {
			String var1 = convertVar(chr, pos, ref, alt.split(",", 2)[0], infoField);
			String var2 = convertVar(chr, pos, ref, alt.split(",", 2)[1], infoField);
			return var1 + "\n" + var2;
		}
		
		if (cont.equals("M") || cont.equals("chrM") || cont.equals("MT") || cont.equals("chrMT")) {
			//cont = "NC_012920";
			cont = "MT";
		}

		if (ref.equals("*")) {
			ref = "-";
		}
		alt = alt.replace("+", "");
		//alt = alt.replace("DEL", "-");
		

		
		
		
		if (alt.equals("-")) {
			ref = ref.replace("-", "");
			return cont + "\t" + (pos-1) + "\t.\tG" + ref + "\tG\t.\t.\t" + infoField;
		} else {
			if (ref.equals("-")) {
				return cont + "\t" + (pos-1) + "\t.\tG\tG" + alt + "\t.\t.\t" + infoField;
			} else {
				return cont + "\t" + pos + "\t.\t" + ref + "\t" + alt + "\t.\t.\t" + infoField;	
			}
		}		
	}
	
	
	
	/**
	 * Convert the variant info a form that works with SnpEff parsing
	 * @param rec
	 * @return
	 */
	private static String convertVar(VariantRec rec) {
		return convertVar(rec.getContig(), rec.getStart(), rec.getRef(), rec.getAlt(), ".");
	}
	
	/**
	 * Convert the variant info a form that works with SnpEff parsing
	 * @param rec
	 * @return
	 */
	private static String convertVarWithInfoEND(VariantRec rec) {

		//For some structural vars the END annotation in the INFO field is needed
		String infoField = ".";
		Integer infoEnd = rec.getPropertyInt(VariantRec.SV_END);
		if (infoEnd != null && infoEnd != -1) {
			infoField = "END=" + infoEnd;
		}
		return convertVar(rec.getContig(), rec.getStart(), rec.getRef(), rec.getAlt(), infoField);
	}
	
	/**
	 *ANN= ##INFO=<ID=ANN,Number=.,Type=String,Description="Functional annotations: 'Allele | Annotation | Annotation_Impact | Gene_Name | Gene_ID | Feature_Type | Feature_ID | Transcript_BioType | Rank | HGVS.c | HGVS.p | cDNA.pos / cDNA.length | CDS.pos / CDS.length | AA.pos / AA.length | Distance | ERRORS / WARNINGS / INFO' ">
	 *  0: alt
	 *  1: Effect
	 *  2: Effect_Impact
	 *  3: Gene name
	 *  4: Gene ID
	 *  5: feature type
	 *  6: feature ID
	 *  7: transcript biotype
	 *  8: rank
	 *  9: c.dot
	 *  10: p.dot
	 *  11: cDNA pos / cDNA length
	 *  12: cds pos / cds length
	 * line13: aa pos / aa length
	 * 14: Distance
	 * 	15: ERRORS / WARNINGS / INFO
	 * 
	 * @param 
	 * @return
	 * 
	 * 
	 * NOTE: If a variant is called as intergenic, the transcript will be returned as the GENE.
	 * 		This is SnpEff v4.3 behavior
	 * 
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
			if (effs[i].startsWith("ANN=")) {
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
		
		if (! token.startsWith("ANN=")) {
			throw new IllegalArgumentException("This doesn't look like a SnpEff info token.");
		}
		
		String[] toks = token.replace("ANN=", "").split(",");
		for(int i=0; i<toks.length; i++) {
			String tok = toks[i];	
			
			SnpEffInfo info = new SnpEffInfo();
			String[] bits = tok.split("\\|",-1);	//need to tokenize with "-1" because empty fields may exist
			String cdot = "";
			String pdot = "";
			String warning ="";
			String transcriptID = "";
			String exonNum = "";
			String effect = "";
			String gene = "";
			String impact = "";

			cdot = bits[9];
			gene = bits[3];
			exonNum = bits[8];
			transcriptID = bits[6];
			pdot = bits[10];
			effect = bits[1];
			warning = bits[15];
			impact = bits[2];
			info.cDot = cdot;
			info.pDot = pdot;
			info.changeType = effect;
			info.exon = exonNum;
			info.gene = gene;
			info.transcript = transcriptID;
			info.warning = warning;
			info.impact = impact;
			infos.add(info);
		}
		
		return infos;
	}

	
	@Override
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
		String warning;
		String impact;
	}
}


