package operator.annovar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.variant.VariantRec;

/**
 * Adds gene / exon variant functions to variant records. This runs annovar -geneanno to generate the information,
 * then parses the resulting output text files to associate the gene/exon function with variants in the
 * variant pool 
 * @author brendan
 *
 */
public class GeneAnnotator extends AnnovarAnnotator {

	public static final String NM_DEFS = "nm.Definitions";
	protected int splicingThreshold = 10;
	
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);
		
		String command = "perl " + annovarPath + "annotate_variation.pl -geneanno --buildver " + buildVer + " --splicing_threshold " + splicingThreshold +  " " + annovarInputFile.getAbsolutePath() + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		executeCommand(command);
		
		String variantFuncFile =  annovarPrefix + ".variant_function";
		String exonFuncFile = annovarPrefix + ".exonic_variant_function";
		
		Map<String,String> nmMap = new HashMap<String, String>();
		String nmFile = this.getAttribute("nm.Definitions");
		if(nmFile != null) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Reading in defined NM #s from file: " + nmFile);
			nmMap = readNMMap(new File(nmFile));
		}
		
		try {
			addAnnotations(variantFuncFile, exonFuncFile, nmMap);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Error reading variant function files", this);
		}
		
		
		//Cleanup old annovar files
		File variantFunc = new File(variantFuncFile);
		variantFunc.deleteOnExit();
		File exonVariantFunc = new File(exonFuncFile);
		exonVariantFunc.deleteOnExit();
	}
	
	
	private void addAnnotations(String variantFilePath, String exonicFuncFilePath, Map<String, String> nmMap) throws IOException {
		//Add gene annotations
		int totalVars =0 ;
		int errorVars = 0;
		BufferedReader reader = new BufferedReader(new FileReader(variantFilePath));
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Reading annovar variant output from file " + variantFilePath);
		List<String> lastFewErrors = new ArrayList<String>(); //Stores info about variants not found in annotation file

		String line = reader.readLine();
		while (line != null) {
			String[] toks = line.split("\\t");
			String variantType = toks[0];
			String gene = toks[1];
			String contig = toks[2];
			int pos = Integer.parseInt(toks[3]);
			String ref = toks[5];
			String alt = toks[6];
			
			VariantRec rec = findVariant(contig, pos, ref, alt);  //Make sure we match alt
			if (rec == null) {
				errorVars++;
				if (lastFewErrors.size() < 10)
					lastFewErrors.add("Variant not found : " + line);
				line = reader.readLine();
				continue;
			}
			
			if (gene.contains(";")) {
				String before = gene;
				String[] genes = gene.split(";");
				gene = genes[0]; 
				if (genes.length > 0 && (!genes[0].equals(genes[1]))) {
					logger.info("Converting gene name : " + before + " to: " + gene);	
				}								
			}
			//Remove nm and cdot info from gene name
			if (gene.contains("(")) {
				gene = gene.substring(0, gene.indexOf("("));
			}
			
			//See if this is a splicing variant. so there may be c.dot information in the gene field (often there's not)
			//So try to parse it... keeping in mind that the preferred NM may have been provided in the nmMap object
			if (variantType.contains("splic")) {
				String info = toks[1];
				if (info.contains("(")) {
					info = info.substring(info.indexOf("(")+1);
					info = info.replace(")", "");
					String[] infoToks = info.split(","); //Each token will be a colon-separated NM#:exon#:cdot
					int nmRec = 0;
					for(int i=0; i<infoToks.length; i++) {
						String[] nmTok = infoToks[i].split(":");
						if (nmTok.length != 3) {
							//Skip it if it doesn't look right
							continue;
						}
						
						String nm = nmTok[0];
						
						if(nmMap.containsKey(gene)){ // if the user has specifed a specific nm #, get it
							if(nm.equals(nmMap.get(gene))){
								nmRec = i;
								Logger.getLogger(Pipeline.primaryLoggerName).info("Using transcript " + nm + " for gene " + gene);
							}
						}
					}
					
					String[] splicingDetails = infoToks[nmRec].split(":");
					String nm = splicingDetails[0];
					String exon = splicingDetails[1];
					String cdot = splicingDetails[2];
					rec.addAnnotation(VariantRec.EXON_NUMBER, exon);
					rec.addAnnotation(VariantRec.CDOT, cdot);
					rec.addAnnotation(VariantRec.NM_NUMBER, nm);
				}
			}
			

			rec.addAnnotation(VariantRec.GENE_NAME, gene);
			rec.addAnnotation(VariantRec.VARIANT_TYPE, variantType);
			
			totalVars++;
			line = reader.readLine();
		}
		
		if (totalVars > 200 && (errorVars > totalVars*0.05)) {
			for(String err : lastFewErrors) {
				System.err.println(err);
			}
			reader.close();
			throw new IOException("Too many variants not found in variant type file  "+ variantFilePath + ", errors: " + errorVars + " total variants: " + totalVars);
		}
		Logger.getLogger(Pipeline.primaryLoggerName).info(errorVars + " of " + totalVars + " could not be associated with a variant record");
		totalVars = 0;
		errorVars = 0;
		reader.close();
		
		
		
		//Add exonic variants functions to records where applicable
		reader = new BufferedReader(new FileReader(exonicFuncFilePath));
		logger.info("Reading annovar exonic variants from file " + exonicFuncFilePath);
		line = reader.readLine();
		lastFewErrors.clear();
		while(line != null) {
			if (line.length()>1) {
				String[] toks = line.split("\\t");
				String exonicFunc = toks[1];
				String ref = toks[6];
				String alt = toks[7];
				String NM = "NA";
				String exonNum = "NA";
				String cDot = "NA";
				String pDot = "NA";
	
				String contig = toks[3];
				int pos = Integer.parseInt( toks[4] );
				VariantRec rec = findVariant(contig, pos, ref, alt);
				
				// We should split by comma, find which has the NM # (string.contains)
				// and then split by colon
				String[] nms = toks[2].split(",");
				String gene = null;
				String tmpNM;
				int nmRec = 0; // iif the user hasn't specified an NM #, just take the first
				for(int i = 0; i < nms.length; i++){
					String[] details = nms[i].split(":");
					//Sometimes theres no gene, so don't try to parse any info
					if (details.length==1 && details[0].contains("UNKNOWN")) {
						break;
					}
					
					gene = details[0];
					tmpNM = details[1];
					if(nmMap.containsKey(gene)){ // if the user has specifed a specific nm #, get it
						if(tmpNM.equals(nmMap.get(gene))){
							nmRec = i;
							Logger.getLogger(Pipeline.primaryLoggerName).info("Using transcript " + tmpNM + " for gene " + gene);
						}
					}
				}
				
				
				String[] details = nms[nmRec].split(":");
				if (details.length>4) {
					NM = details[1];
					exonNum = details[2];
					cDot = details[3];
					pDot = details[4];
				}
				
				
				
				totalVars++;
				
				if (rec != null) {
					rec.addAnnotation(VariantRec.EXON_FUNCTION, exonicFunc);
					if (rec.getAnnotation(VariantRec.NM_NUMBER) == null)
						rec.addAnnotation(VariantRec.NM_NUMBER, NM);
					if (rec.getAnnotation(VariantRec.EXON_NUMBER) == null)
						rec.addAnnotation(VariantRec.EXON_NUMBER, exonNum);
					if (rec.getAnnotation(VariantRec.CDOT) == null)
						rec.addAnnotation(VariantRec.CDOT, cDot);
					rec.addAnnotation(VariantRec.PDOT, pDot);
				}
				else {
					errorVars++;
					if (lastFewErrors.size() < 10)
						lastFewErrors.add("Variant not found : " + line);
				}
			}
			line = reader.readLine();
		}
		reader.close();
		
		Logger.getLogger(Pipeline.primaryLoggerName).info(errorVars + " of " + totalVars + " could not be associated with a variant record");
		if (totalVars > 200 && errorVars > totalVars*0.01) {
			for(String err : lastFewErrors) {
				System.err.println(err);
			}
			reader.close();
			throw new IOException("Too many variants not found in exonic func file " + exonicFuncFilePath + ", errors: " + errorVars + " total variants: " + totalVars);
		}
	}

	private HashMap<String,String> readNMMap(File file){
		BufferedReader br;
		try {
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
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return null;
	}
}
