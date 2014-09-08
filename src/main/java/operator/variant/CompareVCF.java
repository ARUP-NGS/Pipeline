package operator.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;
import buffer.FileBuffer;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import pipeline.Pipeline;
import util.vcfParser.VCFParser;

public class CompareVCF extends IOOperator {

	protected VariantPool variantsA = new VariantPool();
	protected VariantPool variantsB = new VariantPool();
	
	public static Double parseValue(String line, String key) {
		if (! key.endsWith("="))
			key = key + "=";
		int index = line.indexOf(key);
		if (index < 0)
			return null;
		int startIndex = index + key.length();
		int i = startIndex;
		Character c = line.charAt(i);
		while ( Character.isDigit(c)) {
			i++;
			c = line.charAt(i);
		}
		String digStr = line.substring(startIndex, i);
		try {
			Double val = Double.parseDouble(digStr);
			return val;				
		}
		catch (NumberFormatException nfe) {
			System.err.println("Could not parse a value for key: " + key + ", got string: " + digStr);
			return null;
		}
	}
	
	private int buildVariantMap(VCFFile file, VariantPool vars) throws IOException {
		VCFParser vParser = new VCFParser(file.getFile());
		int totalVarsCounted = 0;
		
		while(vParser.advanceLine()) {
			vars.addRecord( vParser.toVariantRec() );
		}
		return totalVarsCounted;
	}



	/**
	 * Returns average of quality scores across all variants in set
	 * @param vars
	 * @return
	 */
	public static double meanQuality(VariantPool vars) {
		double sum = 0;
		double count = 0;
		for(String contig : vars.getContigs()) {
			for(VariantRec rec : vars.getVariantsForContig(contig)) {
				sum += rec.getQuality();
				count++;
			}
		}
		
		return sum/count;
	}

	
	/**
	 * Use a VCFParser to count the number of heterozygotes in this VCF file
	 * @param file
	 * @return
	 */
	private int countHets(File file) {
		int count = 0;
		try {
			VCFParser vp = new VCFParser(file);
			while(vp.advanceLine()) {
				if (vp.isHetero()) 
					count++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return count;
	}

	public static void compareVars(VariantPool varsA, VariantPool varsB, PrintStream output) {
		List<VarPair> perfectMatch = new ArrayList<VarPair>();
		List<VarPair> difZygote = new ArrayList<VarPair>();
		List<VarPair> difAlt = new ArrayList<VarPair>();
		List<VarPair> missingProps = new ArrayList<VarPair>();
		List<VarPair> diffProps = new ArrayList<VarPair>();
		List<VarPair> adtlProps = new ArrayList<VarPair>();
		List<VarPair> missingAnn = new ArrayList<VarPair>();
		List<VarPair> diffAnn = new ArrayList<VarPair>();
		List<VarPair> adtlAnn = new ArrayList<VarPair>();
		double cumulPropDiffs = 0;
		double cumulAnnDiffs = 0;
		double cumulPropAdtns = 0;
		double cumulAnnAdtns = 0;
		double cumulPropMissing = 0;
		double cumulAnnMissing = 0;
		
		for(String contig : varsA.getContigs()) {
			List<VariantRec> listA = varsA.getVariantsForContig(contig);
			for(VariantRec rec : listA) {
				VariantRec match = varsB.findRecordNoWarn(contig, rec.getStart());
				double propDiffs = 0;
				double propMissing = 0;
				double annDiffs = 0;
				double annMissing = 0;
				if (match != null) {
					VarPair pair = new VarPair();
					pair.a = rec;
					pair.b = match;
					
					if (rec.getAlt().equals(match.getAlt())) {
						if (rec.isHetero() == match.isHetero()) {
							perfectMatch.add(pair);							
						}
						else {
							difZygote.add(pair); //Alt allele matches, but zygosity is different
						}
					}
					else {
						difAlt.add(pair); //Alt allele does not match
					}
					//Compare property entries for shared keys
					Collection<String> recProps = rec.getPropertyKeys();
					for(String prop : recProps) {
						Double recProp = rec.getProperty(prop);
						if(match.getProperty(prop)!=null){
							if(recProp != match.getProperty(prop)) {
								propDiffs += 1;
							}
						}
						else {
							propMissing += 1;
						}
					}
					if(propDiffs >= 1){
						output.println("Warning: properties do not match at contig: " + rec.getContig() + " and position: " + rec.getStart()  +  ". Number of differences: " + String.valueOf(propDiffs));
						diffProps.add(pair);
						cumulPropDiffs+=propDiffs;
					}
					if(propMissing >= 1){
						output.println("Warning: property missing from second VCFRecord at contig: " + rec.getContig() + " and position: " + rec.getStart()  +  ". Number missing: " + String.valueOf(propMissing));
						missingProps.add(pair);
						cumulPropMissing+=propMissing;
					}
					
					//Count number of properties in match not in rec
					double propAdtns = 0;
					for(String matchProp : match.getPropertyKeys()) {
						boolean present = false;
						for(String recProp : recProps){
							if(recProp == matchProp)
								present = true;
						}
						if(present == false) {
							propAdtns+=1;
						}
					}
					if(propAdtns > 0) {
						output.println("Warning: additional property in match not present in rec at contig: " + rec.getContig() + " and position: " + rec.getStart() + ". Number missing: " + String.valueOf(propAdtns));
						cumulPropAdtns += propAdtns;
						adtlProps.add(pair);
					}
					
					//Compare annotation entries for shared keys
					Collection<String> recAnn = rec.getAnnotationKeys();
					for(String ann : recAnn) {
						Double recAnnotation = rec.getProperty(ann);
						if(match.getProperty(ann)!=null){
							if(recAnnotation != match.getProperty(ann)) {
								ann += 1;
							}
						}
						else {
							annMissing += 1;
						}
					}
					if(annDiffs >= 1){
						output.println("Warning: annotations do not match at contig: " + rec.getContig() + " and position: " + rec.getStart()  +  ". Number of differences: " + String.valueOf(annDiffs));
						cumulAnnDiffs+=annDiffs;
						diffAnn.add(pair);
					}

					if(annMissing >= 1){
						output.println("Warning: annotations not found in second rec at contig: " + rec.getContig() + " and position: " + rec.getStart()  +  ". Number missing: " + String.valueOf(annMissing));
						missingAnn.add(pair);
						cumulAnnDiffs+=annDiffs;
					}
					
					double annAdtns = 0;
					for(String matchAnn : match.getAnnotationKeys()) {
						boolean present = false;
						for(String recAnnot : recAnn){
							if(recAnnot == matchAnn)
								present = true;
						}
						if(present == false) {
							annAdtns+=1;
						}
					}
					if(annAdtns > 0) {
						output.println("Warning: additional annotation in match not present in rec at contig: " + rec.getContig() + " and position: " + rec.getStart() + ". Number missing: " + String.valueOf(annAdtns));
						cumulAnnAdtns += annAdtns;
						adtlAnn.add(pair);
					}
				}
			}
		}
		
		DecimalFormat formatter = new DecimalFormat("0.000");
		double overlapA = (double)perfectMatch.size() / (double)varsA.size();
		double overlapB = (double)perfectMatch.size() / (double)varsB.size();
		
		output.println("Total number of perfect matches: " + perfectMatch.size());
		output.println("\tFraction of perfect matches from A : " + formatter.format(overlapA));
		output.println("\tFraction of perfect matches from B : " + formatter.format(overlapB));

		
		overlapA = (double)difZygote.size() / (double)varsA.size();
		overlapB = (double)difZygote.size() / (double)varsB.size();

		output.println("Same alt allele, but different zygosity : " + difZygote.size());
		output.println("\tFraction of dif zygotes from A : " + formatter.format(overlapA));
		output.println("\tFraction of dif zygotes from B : " + formatter.format(overlapB));

		overlapA = (double)difAlt.size() / (double)varsA.size();
		overlapB = (double)difAlt.size() / (double)varsB.size();
		output.println("Different alt allele: " + difAlt.size());
		output.println("\tFraction of dif alts from A : " + formatter.format(overlapA));
		output.println("\tFraction of dif alts from B : " + formatter.format(overlapB));

		output.println("Differences between properties of overlapping entries.");
		output.println("\tNumber of VCF records with different values for shared property keys:" + formatter.format(cumulPropDiffs));
		output.println("\tNumber of VCF records missing property keys from the first pool:" + formatter.format(cumulPropMissing));
		output.println("\tNumber of VCF records missing property keys from the second pool:" + formatter.format(cumulPropAdtns));
		
		output.println("Differences between annotations of overlapping entries.");
		output.println("\tNumber of VCF records with different values for shared property keys:" + formatter.format(cumulAnnDiffs));
		output.println("\tNumber of VCF records missing property keys from the first pool:" + formatter.format(cumulAnnMissing));
		output.println("\tNumber of VCF records missing property keys from the second pool:" + formatter.format(cumulAnnAdtns));
		
	}
	
	/**
	 * Returns average variant quality of first item in pair
	 * @param recs
	 * @return
	 */
	public static double meanQualityA(List<VarPair> recs) {
		double sum =0;
		for(VarPair pair : recs) {
			sum += pair.a.getQuality();
		}
		return sum / (double)recs.size();
	}
	
	public static double meanQualityB(List<VarPair> recs) {
		double sum =0;
		for(VarPair pair : recs) {
			sum += pair.b.getQuality();
		}
		return sum / (double)recs.size();
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		FileBuffer fileA = inputBuffers.get(0);
		FileBuffer fileB = inputBuffers.get(1);
		DecimalFormat formatter = new DecimalFormat("#0.00");
		
		try {
			variantsA = new VariantPool( (VCFFile)fileA );
			variantsB = new VariantPool( (VCFFile)fileB );
			
			
			System.out.println("Total variants in " + fileA.getFile().getName() + " : " + variantsA.size());
			System.out.println("Total variants in " + fileB.getFile().getName() + " : " + variantsB.size());
			
			compareVars(variantsA, variantsB, System.out);
			
			VariantPool intersection = (VariantPool) variantsA.intersect(variantsB);
	
			
			VariantPool uniqA = new VariantPool(variantsA);
			uniqA.removeVariants(intersection);
			VariantPool uniqB = new VariantPool(variantsB);
			uniqB.removeVariants(intersection);
			
			
			int hetsA = variantsA.countHeteros();
			int hetsB = variantsB.countHeteros();
			System.out.println("Heterozyotes in " + fileA.getFilename() + " : " + hetsA + " ( " + formatter.format(100.0*(double)hetsA/(double)variantsA.size()) + " % )");
			System.out.println("Heterozyotes in " + fileB.getFilename() + " : " + hetsB +  " ( " + formatter.format(100.0*(double)hetsB/(double)variantsB.size()) + " % )");
			

			System.out.println("Total intersection size: " + intersection.size());
			System.out.println("%Intersection in " + fileA.getFile().getName() + " : " + formatter.format( (double)intersection.size() / (double)variantsA.size()));
			System.out.println("%Intersection in " + fileB.getFile().getName() + " : " + formatter.format( (double)intersection.size() / (double)variantsB.size()));
			
			
			System.out.println("Mean quality of sites in intersection: " + formatter.format(meanQuality(intersection)));
			System.out.println("Mean quality of sites in A but not in intersection: " + formatter.format(meanQuality(uniqA)));
			System.out.println("Mean quality of sites in B but not in intersection: " + formatter.format(meanQuality(uniqB)));
			

			int uniqAHets = uniqA.countHeteros();
			int uniqBHets = uniqB.countHeteros();
			System.out.println("Number of hets in discordant A sites: " + uniqAHets +  " ( " + formatter.format(100.0*(double)uniqAHets/(double)uniqA.size()) + " % )");
			System.out.println("Number of hets in discordant A sites: " + uniqBHets +  " ( " + formatter.format(100.0*(double)uniqBHets/(double)uniqB.size()) + " % )");

		//	System.out.println("\n\n Sites unique to " + fileA.getFilename());
			uniqA.listAll(new PrintStream(new FileOutputStream("unique_to_" + fileA.getFilename())));
		//	System.out.println("\n\nSites unique to " + fileB.getFilename());
			uniqB.listAll(new PrintStream(new FileOutputStream("unique_to_" + fileB.getFilename())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	static class VarPair {
		VariantRec a;
		VariantRec b;
	}
	
}
