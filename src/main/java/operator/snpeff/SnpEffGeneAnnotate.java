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
	
	protected String snpEffDir = null;
	protected String snpEffGenome = null;
	protected boolean performMitoSub = false; //If true, do a substitution of mito chr name to 
	
	private Map<String, List<SnpEffInfo> > annos = null;
	private Map<String, String> nmMap = new HashMap<String, String>();
	
	public void prepare() {
		
		//First we have to build an input file
		File input = new File(this.getProjectHome() + "/snpeff.input");
		File outputFile = new File("snpeff.output");
		int varsWritten = 0;
		
		try {
			input.createNewFile();
			outputFile.createNewFile();

			BufferedWriter writer = new BufferedWriter(new FileWriter(input));
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
		String command = "java -Xmx8g -jar " + snpEffDir + "/snpEff.jar -c " + snpEffDir + "/snpEff.config " + snpEffGenome + " -hgvs -nostats -i txt -o txt " + input.getAbsolutePath(); 
		Logger.getLogger(Pipeline.primaryLoggerName).info("Executing command: " + command);
		try {
			
			executeCommandCaptureOutput(command, outputFile);
			
		} catch (OperationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Finally, parse the output file and read it into memory so we can quickly annotate the variants
		annos = new HashMap<String, List<SnpEffInfo>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(outputFile));
			String line = reader.readLine();
			while(line != null) {
				if (line.length() ==0 || line.startsWith("#")) {
					line = reader.readLine();
					continue;
				}
				SnpEffInfo info = parseOutputLine(line);
				String[] toks = line.split("\t");
				
				String varKey = convertVar(toks[0], Integer.parseInt(toks[1]),toks[2], toks[3]);
				List<SnpEffInfo> infos = annos.get(varKey);
				if (infos == null) {
					infos = new ArrayList<SnpEffInfo>();
					annos.put(varKey, infos);
				}
				infos.add(info);
				
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
		
		SnpEffInfo topHit = infoList.get(0);
		int topRank = calculateRank(topHit.changeType);
		for(SnpEffInfo info : infoList) {
			
			int infoRank= calculateRank(info.changeType);
			
			//First check to see if it's in the nmMap. If so, it gets a really high rank
			if (nmMap.containsKey(info.gene)) {
				String preferredNM = nmMap.get(info.gene);
				if (info.transcript.startsWith(preferredNM)) {
					infoRank = 1000;
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
	}
	
	private static void appendAnnotation(VariantRec var, String key, String value) {
		String existing = var.getAnnotation(key);
		if (existing == null) {
			var.addAnnotation(key, value);
		}
		else {
			var.addAnnotation(key, existing + "; " + value);
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
			String var1 = convertVar(chr, pos, ref, alt.split(",")[0]);
			String var2 = convertVar(chr, pos, ref, alt.split(",")[1]);
			return var1 + "\n" + var2;
		}
		
		if (cont.equals("M") || cont.equals("chrM") || cont.equals("MT") || cont.equals("chrMT")) {
			cont = "NC_012920";
		}

		if (alt.equals("-")) {
			return cont + "\t" + (pos-1) + "\t" + ref + "\tN";
		} else {
			if (ref.equals("-")) {
				return cont + "\t" + (pos-1) + "\tG\tG" + alt;
			} else {
				return cont + "\t" + pos + "\t" + ref + "\t" + alt;	
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
	
	private SnpEffInfo parseOutputLine(String line) {
		String[] toks = line.split("\t");
		SnpEffInfo info = new SnpEffInfo();
		if (toks.length < 2) {
			return info;
		}
		info.gene = toks[10];
		info.transcript = toks[12];
		info.exon = toks[14];
		info.changeType = toks[15];
		
		info.pDot = "";
		info.cDot = "";
		if (!info.changeType.equals("INTERGENIC") && toks.length>16) {
			if (toks[16].length()>0) {
				String[] cp = toks[16].split("/");
				if (cp.length > 1) {
					info.pDot = cp[0];
					info.cDot = cp[1];
				}
				else {
					info.cDot = toks[16];
				}
			}	
		}
		
		return info;
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);

		snpEffDir = this.getPipelineProperty(SNPEFF_DIR);
		if (snpEffDir == null) {
			throw new IllegalArgumentException("No path to snpEff dir specified, use " + SNPEFF_DIR);
		}
		
		snpEffGenome = this.getAttribute(SNPEFF_GENOME);
		if (snpEffGenome == null) {
			throw new IllegalArgumentException("No snpEff genome specified, use attribute " + SNPEFF_GENOME);
		}
		
		String performMitoSubStr = this.getAttribute(PERFORM_MITO_SUB);
		if (performMitoSubStr != null) {
			performMitoSub = Boolean.parseBoolean(performMitoSubStr);
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
