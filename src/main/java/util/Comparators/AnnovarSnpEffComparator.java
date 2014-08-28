/**
 * 
 */
package util.Comparators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import json.JSONException;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * @author markebbert
 *
 */
public class AnnovarSnpEffComparator extends Operator {
	
	private boolean printSnpEffNMList = false;
	private static final String PRINT_SNPEFF_NMLIST = "print.snpeff.nmlist";
	
	private String snpeffNMListOutFileName;
	private static final String SNPEFF_NMLIST_FILE_NAME = "snpeff.nmlist.file";

    private VariantPool snpEffVariants = null;
    private VariantPool annovarVariants = null;

	/**
	 * 
	 */
	public AnnovarSnpEffComparator() {
		return;
	}
	
	/**
	 * Loop over the VariantPool objects and compare the annotations. Track
	 * anything that doesn't match.
	 * @throws OperationFailedException 
	 */
	private void compareAnnovarSnpEff() throws OperationFailedException{
		
		PrintWriter pw = null;
		TreeMap<String, String> snpEffNMs = null;
		if(this.printSnpEffNMList()){
			snpEffNMs = new TreeMap<String, String>();
			try {
				pw = new PrintWriter(this.getSnpeffNMListOutFileName());
			} catch (FileNotFoundException e) {
				throw new OperationFailedException("Could not write to file: " + e.getMessage() , this);
			}
		}
		int snpEffSize = snpEffVariants.size();
		int annovarSize = annovarVariants.size();

		if(snpEffSize != annovarSize){
			throw new OperationFailedException("SnpEff and Annovar didn't even return the same number of variants."
					+ " SnpEff returned: " + snpEffSize + ", while Annovar returned: " + annovarSize, this);
		}
		
		AminoAcidTwoWayHashmap<String, String> aminoMap = getTwoWayAminoAcidAbbrevMap();

		VariantRec snpEffVar, annovarVar;
		List<VariantRec> snpEffVarList = snpEffVariants.toList();
		List<VariantRec> annovarVarList = annovarVariants.toList();
		ArrayList<String> varDiffs = new ArrayList<String>(); 
		ArrayList<String> nmDiffs = new ArrayList<String>(); 
		String snpEffVarNM, annovarVarNM, seCdot, annoCdot, sePdot, annoPdot,
				seExon, annoExon, seGene, annoGene;
		String[] seCdotVals, annoCdotVals, sePdotVals, annoPdotVals;
		int matches = 0, mismatches = 0, unparsed = 0, exonMatches = 0, exonMismatches = 0,
				geneMatches = 0, geneMismatches = 0, cdotMatches = 0, cdotMismatches = 0,
				pdotMatches = 0, pdotMismatches = 0, numNoAnnoNM = 0, numDiffNM = 0;
		boolean difference = false;
		for(int i = 0; i < snpEffVariants.size(); i++){
			snpEffVar = snpEffVarList.get(i);
			annovarVar = annovarVarList.get(i);
			
			snpEffVarNM = snpEffVar.getAnnotation("nm.number");
			annovarVarNM = annovarVar.getAnnotation("nm.number");
			
//			varDiffs = VariantRec.equals(snpEffVar, annovarVar);
			if(annovarVarNM == null){
				numNoAnnoNM++;
			}
			else if(snpEffVarNM.startsWith(annovarVarNM)){ /* Only comparing those where the same nm num is used */
				difference = false;
				seCdot = snpEffVar.getAnnotation("cdot");
				annoCdot = annovarVar.getAnnotation("cdot");
				sePdot = snpEffVar.getAnnotation("pdot");
				annoPdot = annovarVar.getAnnotation("pdot");
				seExon = snpEffVar.getAnnotation("exon.number");
				annoExon = annovarVar.getAnnotation("exon.number");
				seGene = snpEffVar.getAnnotation("gene");
				annoGene = annovarVar.getAnnotation("gene");
				
				seCdotVals = parseHGVSCdot(seCdot);
				annoCdotVals = parseAnnovarCdot(annoCdot);
				
                sePdotVals = parseHGVSPdot(sePdot);
                annoPdotVals = parseAnnovarPdot(annoPdot);
                
                /* Write the NM list from SNPEff so we can use it
                 * to specify which for Annovar to use. Only take
                 * the first one we encounter for each gene.
                 */
                if(this.printSnpEffNMList()){
                	if(!snpEffNMs.containsKey(seGene)){
                        snpEffNMs.put(seGene, snpEffVarNM);
                	}
                }
				
				if(seCdotVals == null || annoCdotVals == null){
					System.err.println("\n############");
					System.err.println("# Warning! #");
					System.err.println("############");
					System.err.println("Cannot parse cdot. Skipping. SNPEff cdot: " + seCdot + "\tAnnov. cdot: " + annoCdot);
					unparsed++;
					continue;
				}
				
				
				/* Arrays are in order of pos, ref, alt */
				if(!seCdotVals[0].equals(annoCdotVals[0]) || /* Check pos */
                    !seCdotVals[1].equals(annoCdotVals[1]) || /* check ref */
                    !seCdotVals[2].equals(annoCdotVals[2])){ /* check alt */

					varDiffs.add("Records do not have the same cdot values. SNPEff: " + seCdot 
							+ "\tAnnovar: " + annoCdot + ". Var: " + snpEffVar.getContig() + " " +
							snpEffVar.getStart() + " " + snpEffVar.getRef() + " " + snpEffVar.getAlt());
					cdotMismatches++;
					difference = true;
				}
				else{
					cdotMatches++;
				}
				
				/* Arrays are in order of pos, ref, alt */
				if((sePdotVals != null && annoPdotVals != null) &&
					(!sePdotVals[0].equals(annoPdotVals[0]) || /* Check pos */
					!sePdotVals[1].equals(aminoMap.getThreeLetterFromOne(annoPdotVals[1])) || /* Check ref */
					!sePdotVals[2].equals(aminoMap.getThreeLetterFromOne(annoPdotVals[2])))){ /* check alt */
					
					varDiffs.add("Records do not have the same pdot values. SNPEff: " + sePdot 
							+ "\tAnnovar: " + annoPdot + ". Var: " + snpEffVar.getContig() + " " +
							snpEffVar.getStart() + " " + snpEffVar.getRef() + " " + snpEffVar.getAlt());
					pdotMismatches++;
					difference = true;
				}
				else{
					pdotMatches++;
				}
				
				if(!annoExon.contains(seExon) || 
						!seExon.equals(annoExon.substring(annoExon.indexOf(seExon), annoExon.length()))){
					varDiffs.add("Records do not have the same exon values. SNPEff: " + seExon
							+ "\tAnnovar: " + annoExon + ". Var: " + snpEffVar.getContig() + " " +
							snpEffVar.getStart() + " " + snpEffVar.getRef() + " " + snpEffVar.getAlt());
					exonMismatches++;
					difference = true;
				}
				else{
					exonMatches++;
				}

				if(!seGene.equals(annoGene)){
					varDiffs.add("Records do not have the same gene values. SNPEff: " + seGene
							+ "\tAnnovar: " + annoGene + ". Var: " + snpEffVar.getContig() + " " +
							snpEffVar.getStart() + " " + snpEffVar.getRef() + " " + snpEffVar.getAlt());
					geneMismatches++;
					difference = true;
				}
				else{
					geneMatches++;
				}
				
				if(difference){
                    mismatches++;
				}
				else{
					matches++;
				}
			}
			else{
				numDiffNM++;
//				mismatches++;
				nmDiffs.add("\nSNPEff annos: " + snpEffVar.annotationsToString() + "\n" +
						"Annovar annos: " + annovarVar.annotationsToString());
			}
		}
		printResultsSummary(numDiffNM, numNoAnnoNM, matches, mismatches, unparsed, cdotMatches, cdotMismatches,
				pdotMatches, pdotMismatches, exonMatches, exonMismatches, geneMatches, geneMismatches, varDiffs, nmDiffs);
		if(this.printSnpEffNMList()){
			for(String key : snpEffNMs.keySet()){
            	pw.write(key + "\t" + snpEffNMs.get(key) + "\n");
			}
		}
	}
	
	private void printDiffs(ArrayList<String> diffs){
		for(String diff : diffs){
			System.out.println(diff);
		}
	}
	
	private String[] parseHGVSCdot(String cdot){
		String cdotPattern = "c\\.(\\d+)(\\w+)>(\\w+)";
		Pattern pattern = Pattern.compile(cdotPattern);
		Matcher matcher = pattern.matcher(cdot);
		while(matcher.find()){

			/* Return in order of pos, ref, alt */
			return new String[]{matcher.group(1), matcher.group(2), matcher.group(3)};
		}

		/* if the first doesn't match, try intron cdot */
		String cdotPatternIntron = "c\\.(\\d+[+-]*\\d*)(\\w+)>(\\w+)";
		pattern = Pattern.compile(cdotPatternIntron);
		matcher = pattern.matcher(cdot);
		while(matcher.find()){
			return new String[]{matcher.group(1), matcher.group(2), matcher.group(3)};
		}
		return null;
	}
	
	private String[] parseHGVSPdot(String pdot){
		if(pdot != null){
            String pdotPattern = "p\\.([a-zA-Z]{3})(\\d+)([a-zA-Z]{3})";
            Pattern p = Pattern.compile(pdotPattern);
            Matcher m = p.matcher(pdot);
            while(m.find()){
                /* Return in order of pos, ref, alt */
                return new String[]{m.group(2), m.group(1), m.group(3)};
            }
		}
		return null;
	}
	
	private String [] parseAnnovarCdot(String cdot){
		String cdotPattern = "c\\.([a-zA-Z]+)(\\d+)([a-zA-Z]+)";
		Pattern pattern = Pattern.compile(cdotPattern);
		Matcher matcher = pattern.matcher(cdot);
		while(matcher.find()){
			
			/* Return in order of pos, ref, alt */
			return new String[]{matcher.group(2), matcher.group(1), matcher.group(3)};
		}

		/* if the first doesn't match, try intron cdot */
		String cdotPatternIntron = "c\\.(\\d+[+-]*\\d*)(\\w+)>(\\w+)";
		pattern = Pattern.compile(cdotPatternIntron);
		matcher = pattern.matcher(cdot);
		while(matcher.find()){
			return new String[]{matcher.group(1), matcher.group(2), matcher.group(3)};
		}
		return null;
	}
	
	private String[] parseAnnovarPdot(String pdot){
		if(pdot != null){
            String pdotPattern = "p\\.([a-zA-Z]{1})(\\d+)([a-zA-Z]{1})";
            Pattern p = Pattern.compile(pdotPattern);
            Matcher m = p.matcher(pdot);
            while(m.find()){
                /* Return in order of pos, ref, alt */
                return new String[]{m.group(2), m.group(1), m.group(3)};
            }
		}
		return null;
	}
	
	private void printResultsSummary(int numDiffNM, int numNoAnnoNM, int matches, int mismatches, int unparsed, int cdotMatches,
			int cdotMismatches, int pdotMatches, int pdotMismatches, int exonMatches, int exonMismatches,
			int geneMatches, int geneMismatches, ArrayList<String> varDiffs, ArrayList<String> nmDiffs){
		
		double percMismatch = round(100*(double)mismatches/(mismatches+matches), 2);
		double percCdotMismatches = round(100*(double)cdotMismatches/(cdotMismatches+cdotMatches), 2);
		double percPdotMismatches = round(100*(double)pdotMismatches/(pdotMismatches+pdotMatches), 2);
		double percExonMismatches = round(100*(double)exonMismatches/(exonMismatches+exonMatches), 2);
		double percGeneMismatches = round(100*(double)geneMismatches/(geneMismatches+geneMatches), 2);
		double percNMMismatches = round(100*(double)numDiffNM/(mismatches+matches+numDiffNM), 2);
		
        String newLine = System.getProperty("line.separator");
        String compSumAlignFormat = "| %12d | %17d | %9d | %12.2f | %10d | %9d | %20d |" + newLine;
        String mismatchSumAlignFormat = "| %3s | %-8s | %12d | %9d | %7d | %12.2f |" + newLine;
        String mismatchSumAlignNoLeftPipeFormat = "  %3s | %-8s | %12d | %9d | %7d | %12.2f |" + newLine;
        String mismatchSumAlignNullFormat = "| %3s | %-8s | %12s | %9s | %7s | %12.2s |" + newLine;
        // String centerAlignFormat = | %14s | %10s | %13.2f%% | %8.0f |" +
        // newLine;
        System.out.format("\n==================================================" + newLine);
        System.out.format("                                                  " + newLine);
        System.out.format("                Comparison Summary                " + newLine);
        System.out.format("                                                  " + newLine);
        System.out.format("==================================================" + newLine + newLine);

        System.out.format("+--------------+-------------------+--------------+------------------------+" + newLine);
        System.out.format("|                           Within Matching NMs                            |" + newLine);
        System.out.format("+--------------+-------------------+-----------+--------------+------------+-----------+----------------------+" + newLine);
        System.out.format("| n Mismatches | n Perfect Matches |   Total   | %% Mismatches | n Unparsed | n Diff NM | n Annovar missing NM |" + newLine);
        System.out.format("+--------------+-------------------+-----------+--------------+------------+-----------+----------------------+" + newLine);
        
        System.out.format(compSumAlignFormat, mismatches, matches, mismatches+matches, percMismatch, unparsed, numDiffNM, numNoAnnoNM);
        System.out.format("+--------------+-------------------+-----------+--------------+------------+-----------+----------------------+" + newLine);
        
        System.out.format("\n\n================================================" + newLine);
        System.out.format("                Mismatch Summary                " + newLine);
        System.out.format("================================================" + newLine + newLine);
        System.out.format("      +----------+--------------+-----------+---------+--------------+" + newLine);
        System.out.format("      | Category | n Mismatches | n Matches |  Total  | %% Mismatches |" + newLine);
        System.out.format("+-----+----------+--------------+-----------+---------+--------------+" + newLine);
        
        System.out.format(mismatchSumAlignFormat, "  M", "Cdot", cdotMismatches, cdotMatches, cdotMismatches+cdotMatches, percCdotMismatches);
        System.out.format(mismatchSumAlignFormat, "N a", "Pdot", pdotMismatches, pdotMatches, pdotMismatches+pdotMatches, percPdotMismatches);
        System.out.format(mismatchSumAlignFormat, "M t", "Exon", exonMismatches, exonMatches, exonMismatches+exonMatches, percExonMismatches);
        System.out.format(mismatchSumAlignFormat, "  c", "Gene", geneMismatches, geneMatches, geneMismatches+geneMatches, percGeneMismatches);
        System.out.format(mismatchSumAlignNullFormat, "  h", "", "", "", "", "");
        System.out.format("+-----+----------+--------------+-----------+---------+--------------+" + newLine);
        System.out.format(mismatchSumAlignNoLeftPipeFormat, "   ", "NM", numDiffNM, matches+mismatches, numDiffNM+matches+mismatches, percNMMismatches);
        System.out.format("      +----------+--------------+-----------+---------+--------------+" + newLine);

        
        
        System.out.format("\n\n====================================================" + newLine);
        System.out.format("                Specific Differences                " + newLine);
        System.out.format("====================================================" + newLine + newLine);
        printDiffs(varDiffs);
        
        
        System.out.format("\n\n===============================================" + newLine);
        System.out.format("                NM Differences                " + newLine);
        System.out.format("==============================================" + newLine);
        printDiffs(nmDiffs);
	}
	
	private AminoAcidTwoWayHashmap<String, String> getTwoWayAminoAcidAbbrevMap(){
		
		AminoAcidTwoWayHashmap<String, String> aminoMap = new AminoAcidTwoWayHashmap<String, String>();
		
		/* SNPEff uses three-letter abbreviations and annovar uses one letter. */
		aminoMap.add("Ala", "A");
		aminoMap.add("Arg", "R");
		aminoMap.add("Asn", "N");
		aminoMap.add("Asp", "D");
		aminoMap.add("Cys", "C");
		aminoMap.add("Glu", "E");
		aminoMap.add("Gln", "Q");
		aminoMap.add("Gly", "G");
		aminoMap.add("His", "H");
		aminoMap.add("Ile", "I");
		aminoMap.add("Leu", "L");
		aminoMap.add("Lys", "K");
		aminoMap.add("Met", "M");
		aminoMap.add("Phe", "F");
		aminoMap.add("Pro", "P");
		aminoMap.add("Ser", "S");
		aminoMap.add("Thr", "T");
		aminoMap.add("Trp", "W");
		aminoMap.add("Tyr", "Y");
		aminoMap.add("Val", "V");
		aminoMap.add("*", "X"); // SNPEff uses '*' for missing and Annovar uses 'X'
		
		return aminoMap;
	}
	
	private static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		this.compareAnnovarSnpEff();
	}

	@Override
	public void initialize(NodeList children) {
		if (children == null) {
			return;
		}
		VariantPool tmpVars;
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VariantPool) {
					tmpVars = (VariantPool)obj;
					if(tmpVars.getObjectLabel().equals("SnpEffVariantPool")){
						snpEffVariants = (VariantPool)obj;
					}
					else if(tmpVars.getObjectLabel().equals("AnnovarVariantPool")){
						annovarVariants = (VariantPool)obj;
					}
				}

			}
		}
		
		String printNMs = this.getAttribute(PRINT_SNPEFF_NMLIST);
		if (printNMs != null) {
			if(printNMs.equalsIgnoreCase("true")){
				this.setPrintSnpEffNMList(true);
			}
			else if(printNMs.equalsIgnoreCase("false")){
				this.setPrintSnpEffNMList(false);
			}
			else{
				throw new IllegalArgumentException("Invalid parameter to " + PRINT_SNPEFF_NMLIST + ": " + printNMs +
						". Expecting 'true' or 'false'.");
			}
		}
		
		String nmOutFile = this.getAttribute(SNPEFF_NMLIST_FILE_NAME);
		if(nmOutFile != null){
			if(printSnpEffNMList() == false){
				System.err.println("WARNING: NM list file name specified without specifying " + PRINT_SNPEFF_NMLIST + "=\"true\"");
			}
			else{
				this.setSnpeffNMListOutFileName(nmOutFile);
			}
		}
	}
	
	private void setPrintSnpEffNMList(boolean printNMs){
		this.printSnpEffNMList = printNMs;
	}

	private boolean printSnpEffNMList(){
		return this.printSnpEffNMList;
	}
	
	private void setSnpeffNMListOutFileName(String outfile){
		this.snpeffNMListOutFileName = outfile;
	}
	
	private String getSnpeffNMListOutFileName(){
		return this.snpeffNMListOutFileName;
	}

	public class AminoAcidTwoWayHashmap<K extends Object, V extends Object> {

		  private Map<K,V> threeLetter = new Hashtable<K, V>();
		  private Map<V,K> oneLetter = new Hashtable<V, K>();

		  public synchronized void add(K key, V value) {
		    threeLetter.put(key, value);
		    oneLetter.put(value, key);
		  }

		  public synchronized K getThreeLetterFromOne(K key) {
		    return oneLetter.get(key);
		  }

		  public synchronized V getOneLetterFromThree(V key) {
		    return threeLetter.get(key);
		  }
		}

}
