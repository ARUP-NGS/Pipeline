package util;

import gene.Gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import math.Histogram;
import math.Integration;
import math.LazyHistogram;
import operator.qc.BamMetrics;
import operator.variant.CompareVCF;
import operator.variant.CompoundHetFinder;
import operator.variant.FPComputer;
import operator.variant.MedDirWriter;
import util.flatFilesReader.DBNSFPReader;
import buffer.BAMFile;
import buffer.BAMMetrics;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.VCFFile;
import buffer.variant.CSVLineReader;
import buffer.variant.GenePool;
import buffer.variant.SimpleLineReader;
import buffer.variant.VarFilterUtils;
import buffer.variant.VariantFilter;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import buffer.variant.VariantRec.PositionComparator;

public class VarUtils {

	public static final int A = 0;
	public static final int C = 1;
	public static final int G = 2;
	public static final int T = 3;
	
	public static void emitUsage() {
		System.out.println("\tVariant Utils, v0.01 \n\t Brendan O'Fallon \n\t ARUP Labs");

		System.out.println(" java -jar varUtils.jar compare varsa.vcf varsb.vcf ");
		System.out.println("			Perform simple comparison of variants in both files");
		
		System.out.println(" java -jar varUtils.jar intersect varsa.vcf varsb.vcf [outputfile]");
		System.out.println("			Find intersection of two variant files");
		
		System.out.println(" java -jar varUtils.jar subtract varsa.vcf varsb.vcf [outputfile]");
		System.out.println("			Subtract variants in second list from first list");

		System.out.println(" java -jar varUtils.jar homFilter vars.vcf [outputfile]");
		System.out.println("			Write only homozygotes from given variant file");

		System.out.println(" java -jar varUtils.jar bedFilter vars.vcf bedFile.bed [outputFile]");
		System.out.println("			Write only variants that are in regions defined by given .bed file");

		System.out.println(" java -jar varUtils.jar hetsByContig vars.vcf");
		System.out.println("			Emit list of how many heterozygotes are in each contig");

		System.out.println(" java -jar varUtils.jar compoundHet kidVars.csv parent1.csv parent2.csv");
		System.out.println("			Write genes which have two heterozygotes in kid, with one from each parent");
		
		System.out.println(" java -jar varUtils.jar geneComp vars1.csv vars2.csv ...");
		System.out.println("			Perform gene-intersection and emit genes with multiple nonsynonymous hits.");
		System.out.println("			** VARIANTS MUST HAVE GENE ANNOTATIONS **");

		System.out.println(" java -jar varUtils.jar summary vars.csv");
		System.out.println("			Emit summary information for the variants");
		
		System.out.println(" java -jar varUtils.jar filter [property] [value] vars.csv");
		System.out.println("			Emit all variants with the given property GREATER than the given value (e.g. pop.freq 0.025)");

		System.out.println(" java -jar varUtils.jar ufilter [property] [value] vars.csv");
		System.out.println("			Emit all variants with the given property LESS than the given value (e.g. pop.freq 0.025)");

		System.out.println(" java -jar varUtils.jar extract [property|annotation] vars.csv");
		System.out.println("			Emit only the value of the property or annotation for all variants in pool");

		System.out.println(" java -jar varUtils.jar histogram [property] vars.csv");
		System.out.println("			Emit a histogram of the values associated with the given property ");		
		
		System.out.println(" java -jar varUtils.jar filterGene vars.csv genelistfile.txt");
		System.out.println("			Emit only those variants in genes given in genelistfile.txt");
		System.out.println("			** VARIANTS MUST HAVE GENE ANNOTATIONS **");

	}
	
	/**
	 * Generate a histogram of the variant depths in the given pool
	 * @param vars
	 * @return
	 */
	public static void computeVarDepthHisto(VariantPool vars, Histogram hist) {
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				Double totDepth = var.getProperty(VariantRec.DEPTH);
				Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
				if (totDepth == null || varDepth == null)
					continue;
				//System.out.println(var.getContig() + "\t" + var.getStart() + "\t" + totDepth + "\t" + varDepth);
				double ratio = 0.99999*(varDepth / totDepth);
				hist.addValue(ratio);
			}
		}	
	}
	
	
	private static void computeReadDepthHisto(VariantPool vars,	Histogram hist) {
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				Double totDepth = var.getProperty(VariantRec.DEPTH);
				//Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
				//System.out.println(var.getContig() + "\t" + var.getStart() + "\t" + totDepth + "\t" + varDepth);
				double ratio = 0.99999*( totDepth);
				hist.addValue(ratio);
			}
		}
	}
	
	public static void handleCompoundHet(String[] args) {
		if (! args[0].equals("compoundHet")) {
			throw new IllegalArgumentException("First arg must be compoundHet");
		}
		
		if (args.length != 4) {
			System.err.println("Please enter the names of variant files for kid, parent1, and parent2, in that order");
			return;
		}
		
		File kidVars = new File(args[1]);
		File par1Vars = new File(args[2]);
		File par2Vars = new File(args[3]);
		
		try {
			VariantPool kidPool  = getPool(kidVars);
			VariantPool par1Pool = getPool(par1Vars);
			VariantPool par2Pool = getPool(par2Vars);
			
			CompoundHetFinder.computeCompoundHets(kidPool, par1Pool, par2Pool, null);
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	/**
	 * Emit only those variants that have a GENE_NAME annotation that matches
	 * a gene name in the given gene pool
	 * @param vars
	 * @param genes
	 * @return
	 */
	public static VariantPool filterByGene(VariantPool vars, GenePool genes, boolean reverse) {
		VariantPool geneVars = new VariantPool();
		int total = 0;
		int noAnno = 0;
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				total++;
				String geneName = var.getAnnotation(VariantRec.GENE_NAME);
				if (geneName == null) {
					//System.err.println("Variant " + var + " does not have gene name annotation!");
					noAnno++;
				}
				
				if (reverse) {
					if (! genes.containsGene(geneName)) {
						geneVars.addRecord(var);
					}
				}
				else {
					if (genes.containsGene(geneName)) {
						geneVars.addRecord(var);
					}
				}
			}
		}
		
//		if (noAnno > 0)
//			System.err.println("No gene annotation found for " + noAnno + " of " + total + " variants examined");
		return geneVars;
	}

	
	
	/**
	 * Identify genes which have common mutations among the given input variant files.
	 * Synonymous and intergenic variants are ignored, as well as those with a population frequency
	 * greater than popFreqCutoff
	 * @param pools
	 * @throws IOException 
	 */
	public static void compareGeneSets(int cutoff, List<File> variantFiles) throws IOException {
		
		GenePool genePool = new GenePool();
		for(File file : variantFiles) {
			VariantPool vPool = new VariantPool(getPool(file));

//			VariantPool lowFreqVars = new VariantPool(vPool.filterPool(VarFilterUtils.getPopFreqFilter(popFreqCutoff)));
//			VariantPool interestingVars = new VariantPool( lowFreqVars.filterPool(VarFilterUtils.getNonSynFilter()));
			int found = 0;
			for(String contig : vPool.getContigs()) {
				for(VariantRec var : vPool.getVariantsForContig(contig)) {
					Double prod = var.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
					boolean passes = true;
					passes = var.getQuality() > 20.0;
					
					String func = var.getAnnotation(VariantRec.EXON_FUNCTION);					
					String type = var.getAnnotation(VariantRec.VARIANT_TYPE);
//					if (type == null || (!type.contains("UTR3"))) {
//						passes = false;
//					}
					
//					Double freq = var.getProperty(VariantRec.POP_FREQUENCY);
//					if (freq != null && freq > 0.05)
//						passes = false;
					
					if (type == null || (!type.contains("exonic"))) {
						passes = false;
					}
					
//					String gene = var.getAnnotation(VariantRec.GENE_NAME);
//					if (gene == null || (! gene.contains("MIR"))) {
//						passes = false;
//					}
					
					
					
					if (func != null && (func.contains("nonsyn") 
							|| func.contains("splic")
							|| func.contains("stopgain")
							|| func.contains("stoploss")
							|| func.contains("frameshift"))) {
						
						Double freq = var.getProperty(VariantRec.POP_FREQUENCY);
						if (freq != null && freq > 0.05)
							passes = false;
						
//						
//						
//						
					}
					else {
						passes = false;
					}
					
					if (passes) {
						genePool.addRecordNoWarn(var);
						found++;
					}
				}
			}
			
			if (found == 0) {
				System.err.println("WARNING : Found 0 variants with correct annotations for file : " + file.getName());
			}
			System.err.println("WARNING : Found " + found + " variants with correct annotations for file : " + file.getName());

		}
		
		List<String> annoKeys = new ArrayList<String>();		
		annoKeys.add(VariantRec.GENE_NAME);
		annoKeys.add(VariantRec.VARIANT_TYPE);
		annoKeys.add(VariantRec.EXON_FUNCTION);
		annoKeys.add(VariantRec.NM_NUMBER);
		annoKeys.add(VariantRec.CDOT);
		annoKeys.add(VariantRec.PDOT);
		annoKeys.add(VariantRec.RSNUM);
		annoKeys.add(VariantRec.POP_FREQUENCY);
		annoKeys.add(Gene.GENE_RELEVANCE);
//		annoKeys.add(VariantRec.SUMMARY_SCORE);
		annoKeys.add(VariantRec.HGMD_INFO);
		annoKeys.add(VariantRec.HGMD_HIT);
		annoKeys.add(VariantRec.RSNUM);
		annoKeys.add(VariantRec.GO_EFFECT_PROD);
		annoKeys.add(VariantRec.SVM_EFFECT);
		annoKeys.add(VariantRec.SIFT_SCORE);
		annoKeys.add(VariantRec.POLYPHEN_SCORE);
		annoKeys.add(VariantRec.MT_SCORE);
		annoKeys.add(VariantRec.PHYLOP_SCORE);
		annoKeys.add(VariantRec.GERP_SCORE);
		annoKeys.add(VariantRec.LRT_SCORE);
		annoKeys.add(VariantRec.SIPHY_SCORE);
		
		genePool.listGenesWithMultipleVars(System.out, cutoff, new MedDirWriter());
		
	}


	private static VariantLineReader getReader(String filename) throws IOException {
		VariantLineReader reader = null;
		if (filename.endsWith("vcf")) {
			reader = new VCFLineParser(new File(filename));
		}
		if (filename.endsWith("csv")) {
			reader = new CSVLineReader(new File(filename));
		}
		return reader;
	}
	
	/**
	 * Compare the variants in the given files and emit a bunch of info describing their 
	 * differences
	 * @param trueMutsFile
	 * @param inferredMutsFile
	 * @throws IOException
	 */
	public static void compareAndEmitVars(File trueMutsFile, File inferredMutsFile) throws IOException {
		SimpleLineReader tParser = new SimpleLineReader(trueMutsFile);
		VCFLineParser vParser = new VCFLineParser(new VCFFile(inferredMutsFile));
		
		VariantRec trueVar = tParser.toVariantRec();
		VariantRec qVar = vParser.toVariantRec();
		int totalTrueVars = 0;
		int totalFoundVars = 0;
		
		VariantPool perfSNPs = new VariantPool();
		VariantPool falsePosSNPs = new VariantPool();
		VariantPool falseNegSNPs = new VariantPool();
		VariantPool falsePosIndels = new VariantPool();
		VariantPool falseNegIndels = new VariantPool();
		VariantPool perfIndels = new VariantPool();
		VariantPool closeIndels = new VariantPool();
		VariantPool notSoCloseIndels = new VariantPool();
		
		Histogram trueIndelHisto = new Histogram(0, 100, 100);
		Histogram foundIndelHisto = new Histogram(0, 100, 100);
		Histogram notFoundIndelHisto = new Histogram(0, 100, 100);
		
		//WHAT THE CODES MEAN:
		//0 : A perfect match
		//1 : Variant at same position, but different alt allele
		//-1: True variant not found, a false negative
		//-2: Query variant not in reference, a false positive
		//-3: Indels in different spots but exact same sequences
		//-4: Indels in different spots with different sequences, but same length
		//-5: Indels in different spots with different sequences of different lengths
		PositionComparator pComp = VariantRec.getPositionComparator();
		while (trueVar != null && qVar != null) {		
			int dif = pComp.compare(trueVar, qVar);
			
			//Variants are at same position
			if (dif == 0) {
				int result;
				if (trueVar.getRef().equals(qVar.getRef()) && trueVar.getAlt().equals(qVar.getAlt())) {
					result = 0; //Perfect match
					
				}
				else {
					result = 1;
				}
				
				if (trueVar.isIndel()) {
					trueIndelHisto.addValue(trueVar.getIndelLength());
				}
				if (qVar.isIndel()) {
					foundIndelHisto.addValue(qVar.getIndelLength());
				}
				
				System.out.println(trueVar.getContig() + "\t" + trueVar.getStart() + "\t" + trueVar.getRef() + "\t" + trueVar.getAlt() + "\t " + result);
				tParser.advanceLine();
				vParser.advanceLine();
				
				
				if (qVar.isIndel()) {
					perfIndels.addRecord(qVar);
				}
				else
					perfSNPs.addRecord(qVar);
				totalTrueVars++;
				totalFoundVars++;
			}

			//A bit of fuzzy-matching for insertions and deletions...
			if (dif != 0 && ((qVar.isInsertion() && trueVar.isInsertion()) || (qVar.isDeletion() && trueVar.isDeletion()))) {

				if ( qVar.getAlt().equals(trueVar.getAlt()) && qVar.getRef().equals(trueVar.getRef())) {
					System.out.println(qVar.getContig() + "\t" + qVar.getStart() + "\t" + qVar.getRef() + "\t" + qVar.getAlt() + "\t " + -3);
					closeIndels.addRecordNoSort(trueVar);
				} else {
					if (qVar.getIndelLength() == trueVar.getIndelLength()) {
						System.out.println(qVar.getContig() + "\t" + qVar.getStart() + "\t" + qVar.getRef() + "\t" + qVar.getAlt() + "\t " + -4);
						closeIndels.addRecordNoSort(trueVar);
					}
					else {
						System.out.println(trueVar.getContig() + "\t" + trueVar.getStart() + "\t" + trueVar.getRef() + "\t" + trueVar.getAlt() + "\t " + -5);
						System.out.println(qVar.getContig() + "\t" + qVar.getStart() + "\t" + qVar.getRef() + "\t" + qVar.getAlt() + "\t " + -5);
						notSoCloseIndels.addRecordNoSort(trueVar);
					}
				}
				
				if (trueVar.isIndel()) {
					trueIndelHisto.addValue(trueVar.getIndelLength());			
				}
				if (qVar.isIndel()) {
					foundIndelHisto.addValue(qVar.getIndelLength());
				}
				
				tParser.advanceLine();
				vParser.advanceLine();
				totalTrueVars++;
				totalFoundVars++;
			}
			else {
				if (dif < 0) {
					//TrueVar exists, but no matching qvar
					System.out.println(trueVar.getContig() + "\t" + trueVar.getStart() + "\t" + trueVar.getRef() + "\t" + trueVar.getAlt() + "\t -1");
					tParser.advanceLine();
					totalTrueVars++;
					if (trueVar.isIndel()) {
						falseNegIndels.addRecordNoSort(trueVar);
						trueIndelHisto.addValue(trueVar.getIndelLength());
					}
					else {
						falseNegSNPs.addRecordNoSort(trueVar);
					}
				}
				if (dif > 0) {
					//QVar exists, no matching true var
					System.out.println(qVar.getContig() + "\t" + qVar.getStart() + "\t" + qVar.getRef() + "\t" + qVar.getAlt() + "\t -2");
					vParser.advanceLine();
					totalFoundVars++;
					if (qVar.isIndel()) {
						falsePosIndels.addRecordNoSort(qVar);
						foundIndelHisto.addValue(qVar.getIndelLength());
					}
					else
						falsePosSNPs.addRecordNoSort(qVar);
				}
			}
			
			trueVar = tParser.toVariantRec();
			qVar = vParser.toVariantRec();
		}
		
		while(trueVar != null) {
			System.out.println(trueVar.getContig() + "\t" + trueVar.getStart() + "\t" + trueVar.getRef() + "\t" + trueVar.getAlt() + "\t -1");
			tParser.advanceLine();
			totalTrueVars++;
			if (trueVar.isIndel()) {
				falseNegIndels.addRecordNoSort(trueVar);
				trueIndelHisto.addValue(trueVar.getIndelLength());
			}
			else {
				falseNegSNPs.addRecordNoSort(trueVar);
			}
			
			tParser.advanceLine();
			trueVar = tParser.toVariantRec();
		}
		
		while (qVar != null) {
			totalFoundVars++;
			if (qVar.isIndel()) {
				falsePosIndels.addRecordNoSort(qVar);
				foundIndelHisto.addValue(qVar.getIndelLength());
			}
			else
				falsePosSNPs.addRecordNoSort(qVar);
			
			vParser.advanceLine();
			qVar = vParser.toVariantRec();
		}
		
		System.err.println("Total true variants: " + totalTrueVars);
		System.err.println("Total found variants: " + totalFoundVars);
		System.err.println("Total perfect matches: " + (perfSNPs.size() + perfIndels.size()) );
		System.err.println("Total perfect SNP matches: " + perfSNPs.size() );
		System.err.println("Total SNP false positives : "+ falsePosSNPs.size());
		System.err.println("Total SNP false negatives : "+ falseNegSNPs.size());
		
		System.err.println("Total perfect indel matches: " + perfIndels.size() );
		System.err.println("Total indel false positives : "+ falsePosIndels.size());
		System.err.println("Total indel false negatives : "+ falseNegIndels.size());
		
		System.err.println("Total indel near-misses : "+ closeIndels.size());
		System.err.println("Total indel sort-of-near-misses : "+ notSoCloseIndels.size());
		

		System.err.println("Histogram of true indel sizes: ");
		System.err.println(trueIndelHisto.toString());
		System.err.println("Histo size: " + trueIndelHisto.getCount());
		
		for(String contig : falseNegIndels.getContigs()) {
			List<VariantRec> recs = falseNegIndels.getVariantsForContig(contig);
			for(VariantRec rec : recs) 
				notFoundIndelHisto.addValue(rec.getIndelLength());
		}

		System.err.println("Histogram of not found (false neg) indels sizes: ");
		System.err.println(notFoundIndelHisto.toString());
		System.err.println("Histo size: " + notFoundIndelHisto.getCount());
		
		Histogram falsePosIndelHisto = new Histogram(0, 100, 100);
		for(String contig : falsePosIndels.getContigs()) {
			List<VariantRec> recs = falsePosIndels.getVariantsForContig(contig);
			for(VariantRec rec : recs) 
				falsePosIndelHisto.addValue(rec.getIndelLength());
		}

		System.err.println("Histogram of false pos indel sizes: ");
		System.err.println(falsePosIndelHisto.toString());
		System.err.println("Histo size: " + falsePosIndelHisto.getCount());
				
		
		System.err.println("Histogram of all indels found (regardless of correctness): ");
		System.err.println(foundIndelHisto.toString());
		System.err.println("Histo size: " + foundIndelHisto.getCount());
	}
	
	/**
	 * Obtain an AbstractVariantPool from a file
	 * @param inputFile
	 * @return
	 * @throws IOException
	 */
	public static VariantPool getPool(File inputFile) throws IOException {
		VariantPool variants = null;
		if (inputFile.getName().endsWith(".csv")) {
			variants = new VariantPool(new CSVFile(inputFile));	
			//variants = new VariantPool(new SimpleLineReader(inputFile));
		} else {
			if (inputFile.getName().endsWith(".vcf")) {
				variants = new VariantPool(new VCFFile(inputFile));	
			}
			else {
				throw new IllegalArgumentException("Unrecognized file suffix for input file: " + inputFile.getName());
			}
		}
		return variants;
	}
	
	/**
	 * Obtain an VariantPool from a CSV using a 'SimpleLineReader', which only
	 * 
	 * @param inputFile
	 * @return
	 * @throws IOException
	 */
	public static VariantPool getSPool(File inputFile) throws IOException {
		VariantPool variants = new VariantPool(new SimpleLineReader(inputFile));	
		return variants;
	}
	
	public static void main(String[] args) {
		
		//args = new String[]{"subtract", "/media/DATA/whitney_genome/52Kid2/contig_22.realigned.sorted.recal.vcf", "/media/DATA/whitney_genome/52Kid3/contig_22.realigned.sorted.recal.vcf"};
		
		if (args.length==0) {
			emitUsage();
			return;
		}
		
		String firstArg = args[0];
		
		if (firstArg.equals("geneFilter")) {
			performGeneFilter(args);
			return;
		}
		
		if (firstArg.equals("hhtGeneComp")) {
			performHHTGeneComp(args);
			return;
		}
		
		if (firstArg.equals("tMatrix")) {
			performComputeTMatrix(args);
			return;
		}
		
		if (firstArg.equals("hhtComp")) {
			performHHTComp(args);
			return;
		}
		
		if (firstArg.equals("tkgComp") || firstArg.equals("tgkComp")) {
			performTKGComp(args);
			return;
		}
		
		if (firstArg.equals("snpFilter")) {
			performSNPFilter(args);
			return;
		}
		
		if (firstArg.equals("sample")) {
			performSample(args);
			return;
		}
		
		if (firstArg.equals("qualityTT")) {
			try {
				performEmitQualityTiTv(args);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		
		
		
		//Compare 'truemuts'-style csv to a vcf 
		if (firstArg.equals("scompare")) {
			if (args.length != 3) {
				System.out.println("Enter the names of the truemuts.csv file and a vcf file to compare");
				return;
			}
			
			try {
				File fileA = new File(args[1]);
				File fileB = new File(args[2]);
				DecimalFormat formatter = new DecimalFormat("#0.000");
				
				VariantPool trueVars = getSPool(new File(args[1]));
				VariantPool queryVars = getPool(new File(args[2]));
				
				System.out.println("Total true variants " + fileA.getName() + " : " + trueVars.size() );
				System.out.println("Total inferred variants " + fileB.getName() + " : " + queryVars.size() + " mean quality: " + formatter.format(queryVars.meanQuality()));
				
				CompareVCF.compareVars(trueVars, queryVars, System.out);		
								
				VariantPool trueSNPs = new VariantPool(trueVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isSNP();
					}
				}));
				
				VariantPool qSNPs = new VariantPool(queryVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isSNP();
					}
				}));
				
				System.out.println("*** SNPs Only Comparison ********");
				System.out.println("True number of SNPs : " + trueSNPs.size());
				System.out.println("Inferred number of SNPs : " + qSNPs.size());
				CompareVCF.compareVars(trueSNPs, qSNPs, System.out);
				
				VariantPool trueInsertions = new VariantPool(trueVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isInsertion();
					}
				}));
				
				VariantPool qInserts = new VariantPool(queryVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isInsertion();
					}
				}));
				
				System.out.println("*** Insertions only Comparison ********");
				System.out.println("True number of insertions : " + trueInsertions.size());
				System.out.println("Inferred number of insertions : " + qInserts.size());
				CompareVCF.compareVars(trueInsertions, qInserts, System.out);
				
				
				VariantPool trueDeletions = new VariantPool(trueVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isDeletion();
					}
				}));
				
				VariantPool qDeletes = new VariantPool(queryVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isDeletion();
					}
				}));
				
				System.out.println("*** Deletions only Comparison ********");
				System.out.println("True number of deletions : " + trueDeletions.size());
				System.out.println("Inferred number of deletions : " + qDeletes.size());
				
				CompareVCF.compareVars(trueDeletions, qDeletes, System.out);
				
				
				System.out.println(" (finding intersection, this may take a minute..)");
				VariantPool intersection = (VariantPool) trueVars.intersect(queryVars);
	
				VariantPool uniqA = new VariantPool(trueVars);
				uniqA.removeVariants(intersection);
				VariantPool uniqB = new VariantPool(queryVars);
				uniqB.removeVariants(intersection);
				
				System.out.println("Total mumber true vars not found (false negatives) : " + uniqA.size());
				System.out.println("Total number false positives : " + uniqB.size());
				
				int hetsA = trueVars.countHeteros();
				int hetsB = queryVars.countHeteros();
				System.out.println("Heterozyotes in " + fileA.getName() + " : " + hetsA + " ( " + formatter.format(100.0*(double)hetsA/(double)trueVars.size()) + " % )");
				System.out.println("Heterozyotes in " + fileB.getName() + " : " + hetsB +  " ( " + formatter.format(100.0*(double)hetsB/(double)queryVars.size()) + " % )");
				

				System.out.println("Total intersection size: " + intersection.size());
				System.out.println("%Intersection in " + fileA.getName() + " : " + formatter.format( (double)intersection.size() / (double)trueVars.size()));
				System.out.println("%Intersection in " + fileB.getName() + " : " + formatter.format( (double)intersection.size() / (double)queryVars.size()));
				
				
				System.out.println("Mean quality of sites in intersection: " + formatter.format(intersection.meanQuality()));
				System.out.println("Mean quality of sites in A but not in intersection: " + formatter.format(uniqA.meanQuality()));
				System.out.println("Mean quality of sites in B but not in intersection: " + formatter.format(uniqB.meanQuality()));

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return;
		}

		if (firstArg.equals("mergeBed") || firstArg.equals("mergeBED")) {
			performMergeBED(args);
			return;
		}
		
		if (firstArg.equals("hapCompare")) {
			performHapCompare(args);
			return;
		}
		
		if (firstArg.equals("collate")) {
			performCollate(args);
			return;
		}
		
		if (firstArg.equals("compare")) {
			performCompare(args);
			return;
		}
		
		if (firstArg.equals("sort")) {
			performSort(args);
			return;
		}
		
		if (firstArg.equals("propGeneComp")) {
			performPropGeneComp(args);
			return;
		}
		
		if (firstArg.equals("freqComp")) {
			performFreqComp(args);
			return;
		}
		
		if (firstArg.equals("computeFP")) {
			performComputeFP(args);
			return;
		}
		
		if (firstArg.equals("intersect")) {
			performIntersection(args);
			return;
		}
		
		if (firstArg.equals("subtract")) {
			performSubtraction(args);
			return;
		}


		if (firstArg.equals("homFilter")) {
			performHomFilter(args);
			return;
		}

		if (firstArg.equals("buildroc")) {
			performBuildROC(args, false);
			return;
		}
		
		if (firstArg.equals("buildrocVQSR")) {
			performBuildROC(args, true);
			return;
		}
		
		
		if (firstArg.equals("geneExtract")) {
			performGenePropExtract(args, false);
			return;
		}
		
		if (firstArg.equals("tkgDelHits")) {
			performTKGGenePropExtract(args);
			return;
		}
		
		if (firstArg.equals("revGeneExtract")) {
			performGenePropExtract(args, true);
			return;
		}
		
		if (firstArg.equals("bedFilter")) {
			performBedFilter(args);
			return;
		}

		if (firstArg.equals("hetFilter")) {
			performHetFilter(args);
			return;
		}

		
		if (firstArg.equals("compoundHet")) {
			handleCompoundHet(args);
			return;
		}

		
		if (firstArg.equals("geneComp")) {
			performGeneComp(args);
			return;
		}
			
	
		if (firstArg.equals("novelFilter")) {
			try {
				performNovelFilter(args);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
			
		
		//Extract property or annotation and emit to stdout
		if (firstArg.equals("ex") || firstArg.equals("extract")) {
			performExtract(args);
			return;
		}
		
		//Just like filter, but lists all matching records to stdout
		if (firstArg.equals("emit")) {
			performEmit(args);
			return;
		}
			
		
		//General filter, second arg must match a column header, third arg is value for filter
		if (firstArg.equals("filter") || firstArg.equals("ufilter")) {
			performFilter(args);
			return;
		}
		
		if (firstArg.equals("annofilter") || firstArg.equals("afilter")) {
			performAnnoFilter(args);
			return;
		}
		
		
		if (firstArg.equals("sampleCount") ) {
			performSampleCount(args);
			return;
		}
		
		if (firstArg.equals("combine") || firstArg.equals("union")) {
			performCombine(args);
			return;
		}

		if (firstArg.startsWith("removeDup")) {
			performRemoveDuplicates(args);
			return;
		}

		
		if (firstArg.equals("summary") || firstArg.equals("sum")) {
			performSummary(args);
			return;
		}
		
		if (firstArg.equals("buildvcfdb")) {
			buildVCFDB(args);
			return;
		}
		
		if (firstArg.equals("multiqualhist")) {
			performMultiQualHist(args);
			return;
		}
		
		
		if (firstArg.equals("varFreq")) {
			performVarFreq(args);
			return;
		}
		
	
		
		
		if (firstArg.equals("bamMetrics")) {
			
			for(int i=1; i<args.length; i++ ) {
				BAMFile bamFile = new BAMFile(new File(args[i]));
				BAMMetrics metrics = BamMetrics.computeBAMMetrics(bamFile);
			
				System.out.println("Summary for " + args[i] + "\n" + BamMetrics.getBAMMetricsSummary(metrics));
			}
				
			
			return;
		}
		
		
		if (firstArg.startsWith("hist")) {
			performHistogram(args);
			return;
		}
		
			
		
		System.out.println("Unrecognized command : " + args[0]);
		emitUsage();
	}

	private static void performTKGComp(String[] args) {
		if (args.length != 6) {
			System.out.println("Please enter the gene pool, the score, the threshold, the population (e.g. afr.freq, amr.freq, etc.) and the tkg data file");
			return;
		}
		try {
			GenePool genePool = new GenePool(new File(args[1]));
			String score = args[2];
			Double threshold = Double.parseDouble(args[3]);
			String populationType = args[4];
			
			double totTarget = 0;
			double totNonTarget = 0;
			double delTarget = 0;
			double delNonTarget = 0;
			
			VariantLineReader reader = new CSVLineReader(new File(args[5]));
			VariantRec var = reader.toVariantRec();
			while(var != null) {
				Double val = var.getProperty(score);
				Double freq = var.getProperty(populationType);
				
				if (val != null & freq != null) {
					String gene = var.getAnnotation(VariantRec.GENE_NAME);
					if (genePool.containsGene(gene)) {
						if (val > threshold) {
							delTarget += freq;
						}
						totTarget += freq;
					}
					else {
						if (val > threshold) {
							delNonTarget += freq;
						}
						totNonTarget += freq;
					}
						
				}
				
				
				reader.advanceLine();
				var = reader.toVariantRec();
			}
			System.out.println(args[4] + "\t" + totTarget + "\t" + delTarget + "\t" + totNonTarget + "\t" + delNonTarget + "\t" + (double)delTarget / (double)delNonTarget);
		}
		catch (IOException ex) {
			
		}
	}
	
	private static void performVarFreq(String[] args) {
		String gene = args[1];
		String pDot = args[2];
		
		int alleleCount = 0;
		int total = 0;
		Double popFreq = null;
		Double eurFreq = null;
		for(int i=3; i<args.length; i++) {
			VariantLineReader reader;
			try {
				reader = getReader(args[i]);
				VariantRec var = reader.toVariantRec();
				while(var != null) {
					if (gene.equals( var.getAnnotation(VariantRec.GENE_NAME)) && pDot.equals( var.getAnnotation(VariantRec.PDOT) )) {
						popFreq = var.getProperty(VariantRec.POP_FREQUENCY);
						eurFreq = var.getProperty(VariantRec.EUR_FREQUENCY);
						int alleles = 1;
						if (! (var.isHetero())) {
							alleles = 2;
						}
						alleleCount += alleles;
						break;
					}
					reader.advanceLine();
					var = reader.toVariantRec();
				}
				total += 2;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//System.out.println(" Total samples: " + total/2);
		double freq = (double)alleleCount / (double)total;
		System.out.println(" Frequency : " + freq + "\t" + popFreq + "\t" + eurFreq);
		
	}
	
	private static void performHHTGeneComp(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the list of genes file, then the annotation, then the threshold, then the 1000 Genomes data file, then one or more annotated HHT files");
			return;
		}
		
		String anno = args[2];
		Double threshold = Double.parseDouble(args[3]);
		
		
		
		try {
			List<String> geneList = new ArrayList<String>();
			BufferedReader geneReader = new BufferedReader(new FileReader(args[1]));
			String line = geneReader.readLine();
			while(line != null) {
				if (line.length()>0 && (!line.startsWith("#"))) {
					geneList.add(line.trim());
				}
				line = geneReader.readLine();
			}
			geneReader.close();
			
			for(String gene : geneList) {
				double tkgTotTarget = 0;
				double tkgTotNonTarget = 0;
				double tkgDelTarget = 0;
				double tkgDelNonTarget = 0;
				
				VariantLineReader reader = new CSVLineReader(new File(args[4]));
				VariantRec var = reader.toVariantRec();
				while(var != null) {
					Double val = var.getProperty(anno);
					Double freq = var.getProperty("eur.freq");
					
					if (val != null & freq != null) {
						String varGene = var.getAnnotation(VariantRec.GENE_NAME);
						if (varGene.equals(gene)) {
							if (val > threshold) {
								tkgDelTarget += freq;
							}
							tkgTotTarget += freq;
						}
						else {
							if (val > threshold) {
								tkgDelNonTarget += freq;
							}
							tkgTotNonTarget += freq;
						}
							
					}
					
					
					reader.advanceLine();
					var = reader.toVariantRec();
				}
				
				//System.out.println(gene + "\t" + totTarget + "\t" + delTarget + "\t" + totNonTarget + "\t" + delNonTarget + "\t" + (double)delTarget / (double)delNonTarget);

				//			

				double hhtTotTarget = 0;
				double hhtTotNonTarget = 0;
				double hhtDelTarget = 0;
				double hhtDelNonTarget = 0;
				for(int i=5; i<args.length; i++) {

					reader = getReader(args[i]);
					do {
						var = reader.toVariantRec();
						if (var != null && var.isSNP()) {
							String varGene = var.getAnnotation(VariantRec.GENE_NAME);
							Double val = var.getProperty(anno);
							int count = 1;
							if (! var.isHetero()) {
								count = 2;
							}
							if (varGene != null && val != null) {
								if (varGene.equals(gene)) {
									if (val > threshold)
										hhtDelTarget += count;
									hhtTotTarget += count;
								}
								else {
									if (val > threshold)
										hhtDelNonTarget += count;
									hhtTotNonTarget += count;
								}
							}
						}
					} while(reader.advanceLine());

				}
				
				
				System.out.println(gene + "\t" + tkgDelTarget + "\t" + hhtDelTarget + "\t" + (double)tkgDelTarget / (double)tkgDelNonTarget + "\t" + (double)hhtDelTarget / (double)hhtDelNonTarget + "\t" + (((double)hhtDelTarget / (double)hhtDelNonTarget)-((double)tkgDelTarget / (double)tkgDelNonTarget)));	
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private static void performHHTComp(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the list of genes file, then the annotation, then the threshold, then one or more annotated files");
			return;
		}
		
		try {
			GenePool genePool = new GenePool(new File(args[1]));
			String anno = args[2];
			Double threshold = Double.parseDouble(args[3]);
			
			int allTotTarget = 0;
			int allTotNonTarget = 0;
			int allDelTarget = 0;
			int allDelNonTarget = 0;
			
			for(int i=4; i<args.length; i++) {
				int totTarget = 0;
				int totNonTarget = 0;
				int delTarget = 0;
				int delNonTarget = 0;
				int skipped = 0;
				
				VariantLineReader reader = getReader(args[i]);
				do {
					VariantRec var = reader.toVariantRec();
					if (var != null && var.isSNP()) {
						String gene = var.getAnnotation(VariantRec.GENE_NAME);
						Double val = var.getProperty(anno);
						int count = 1;
						if (! var.isHetero()) {
							count = 2;
						}
						if (gene != null && val != null) {
							if (genePool.containsGene(gene)) {
								if (val > threshold)
									delTarget += count;
								totTarget += count;
							}
							else {
								if (val > threshold)
									delNonTarget += count;
								totNonTarget += count;
							}
						}
						else {
							skipped++;
						}
					}
				} while(reader.advanceLine());
				
				//System.out.println("File : " + args[i]);
				//System.out.println("Total target snps:" + totTarget);
				//System.out.println("Total non-target snps:" + totNonTarget);
				//System.out.println("Del. target snps:" + delTarget + "\t" + ((double)delTarget/(double)totTarget));
				//System.out.println("Del. non-target snps:" + delNonTarget + "\t" + ((double)delNonTarget)/(double)totNonTarget);
				System.out.println(args[i] + "\t" + totTarget + "\t" + delTarget + "\t" + totNonTarget + "\t" + delNonTarget + "\t" + (double)delTarget / (double)delNonTarget);
				allTotTarget += totTarget;
				allTotNonTarget += totNonTarget;
				allDelTarget += delTarget;
				allDelNonTarget += delNonTarget;
			}
		
			if (args.length > 5) {
				System.out.println("\nOverall counts:");
				System.out.println("all:\t" + allTotTarget + "\t" + allDelTarget + "\t" + allTotNonTarget + "\t" + allDelNonTarget + "\t" + (double)allDelTarget / (double)allDelNonTarget);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	private static void performSNPFilter(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the name of the file to filter for snps");
			return;
		}
		
		try {
			VariantLineReader reader = getReader(args[1]);
			System.out.println(reader.getHeader());
			do {
				VariantRec var = reader.toVariantRec();
				if (var != null && var.isSNP())
					System.out.println( reader.getCurrentLine() );
			}while(reader.advanceLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performAnnoFilter(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the property to filter by and the cutoff value");
			return;
		}
		
		String key = args[1];
		String val = args[2];
		try {
			for(int i=3; i<args.length; i++) {
				VariantPool pool = getPool(new File(args[i]));
				VariantPool filteredVars = new VariantPool();
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						String anno = var.getAnnotation(key);
						if (anno != null && anno.contains(val)) {				
							filteredVars.addRecord(var);
						}
												
					}
				}

				List<String> annoKeys = new ArrayList<String>();
				annoKeys.add(VariantRec.RSNUM);
				annoKeys.add(VariantRec.POP_FREQUENCY);
				annoKeys.add(VariantRec.GENE_NAME);
				annoKeys.add(VariantRec.VARIANT_TYPE);
				annoKeys.add(VariantRec.EXON_FUNCTION);
				annoKeys.add(VariantRec.CDOT);
				annoKeys.add(VariantRec.PDOT);
				annoKeys.add(VariantRec.VQSR);
				annoKeys.add(VariantRec.DEPTH);
				annoKeys.add(VariantRec.EFFECT_PREDICTION2);
				//annoKeys.add(VariantRec.PUBMED_SCORE);
				annoKeys.add(VariantRec.FALSEPOS_PROB);
				annoKeys.add(VariantRec.TAUFP_SCORE);
				annoKeys.add(VariantRec.FS_SCORE);
				annoKeys.add(VariantRec.MT_SCORE);
				annoKeys.add(VariantRec.POLYPHEN_SCORE);
				annoKeys.add(VariantRec.SIFT_SCORE);
				annoKeys.add(VariantRec.GERP_SCORE);
				annoKeys.add(VariantRec.PHYLOP_SCORE);
				annoKeys.add(key);
				
				
				if (args.length==4) {
					filteredVars.listAll(System.out, annoKeys);
					System.err.println("Found " + filteredVars.size() + " vars with " + key + " containing " + val);
				}
				else {
					//Write results to a file
					String outfilename = args[i].replace(".vcf", "").replace(".csv", "") + ".flt-" + val + ".csv";
					PrintStream out = new PrintStream(new FileOutputStream(new File(outfilename)));
					System.err.println("Emitting vars from file " + args[i] + " to : " + outfilename);
					filteredVars.listAll(out, annoKeys);
					out.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void buildVCFDB(String[] args)  {

		VariantPool hugePool = new VariantPool();
		Set<String> analysisTypes = new HashSet<String>();
		
		for(int i=1; i<args.length; i++) {
			VariantPool tmpPool = new VariantPool(); //Temp storage for variants we will add to huge pool
			
			//Trim analysis string from filename
			int endPos = args[i].indexOf("_");
			if (endPos == -1) {
				endPos = args[i].indexOf(".");
			}
			String analysisTypeStr = args[i].substring(0, endPos);
			analysisTypes.add(analysisTypeStr);
			try {
				VariantLineReader reader = getReader(args[i]);
				System.err.println("Adding variants from " + args[i] + " current pool size is: " + hugePool.size());
				do {
					VariantRec var = reader.toVariantRec();
					VariantRec tableVar = hugePool.findRecordNoWarn(var.getContig(), var.getStart());
					if (tableVar == null) {
						var.addProperty(VariantRec.SAMPLE_COUNT, 1.0);
						var.addProperty(analysisTypeStr, 1.0);
						tmpPool.addRecordNoSort(var);
					}
					else {
						Double count = tableVar.getProperty(VariantRec.SAMPLE_COUNT);
						tableVar.addProperty(VariantRec.SAMPLE_COUNT, count+1);
						
						Double analTypeCount = tableVar.getProperty(analysisTypeStr);
						if (analTypeCount == null) {
							tableVar.addProperty(analysisTypeStr, 1.0);
						}
						else {
							tableVar.addProperty(analysisTypeStr, analTypeCount+1);
						}
						
						if (! var.getAlt().equals(tableVar.getAlt())) {
							tableVar.setAlt(tableVar.getAlt() + "," + var.getAlt());
						}
					}


				} while(reader.advanceLine());
				
				tmpPool.sortAllContigs();
				hugePool.addAll(tmpPool);
			}
			catch(IOException ex) {
				System.err.println("Warning, could not import variants from file " + args[i] + ":" + ex.getMessage());
			}
		}
		
		//Emit pool to system.out
		System.out.print("#contig\tpos\tref\talt\ttot.samples");
		for(String analysisType : analysisTypes) {
			System.out.print("\t" + analysisType);
		}
		System.out.println();
		
		for(String contig : hugePool.getContigs()) {
			for(VariantRec var : hugePool.getVariantsForContig(contig)) {
				System.out.print(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt() + "\t" + var.getProperty(VariantRec.SAMPLE_COUNT));
				for(String analysisType : analysisTypes) {
					Double ac = var.getProperty(analysisType);
					if (ac == null)
						ac = 0.0;
					System.out.print("\t" + ac);
				}
				System.out.println();
			}
		}
	}
	
	private static void performMultiQualHist(String[] args)  {

		VariantPool hugePool = new VariantPool();
		Set<String> analysisTypes = new HashSet<String>();
		
		for(int i=1; i<args.length; i++) {
			VariantPool tmpPool = new VariantPool(); //Temp storage for variants we will add to huge pool
			
			//Trim analysis string from filename
			int endPos = args[i].indexOf("_");
			if (endPos == -1) {
				endPos = args[i].indexOf(".");
			}
			String analysisTypeStr = args[i].substring(0, endPos);
			analysisTypes.add(analysisTypeStr);
			try {
				VariantLineReader reader = getReader(args[i]);
				System.err.println("Adding variants from " + args[i] + " current pool size is: " + hugePool.size());
				do {
					VariantRec var = reader.toVariantRec();
					VariantRec tableVar = hugePool.findRecordNoWarn(var.getContig(), var.getStart());
					if (tableVar == null) {
						var.addProperty(VariantRec.SAMPLE_COUNT, 1.0);
						var.addProperty(analysisTypeStr, 1.0);
						var.addAnnotation("qualities", "" + var.getQuality());
						tmpPool.addRecordNoSort(var);
					}
					else {
						Double count = tableVar.getProperty(VariantRec.SAMPLE_COUNT);
						tableVar.addProperty(VariantRec.SAMPLE_COUNT, count+1);
						
						String currentQuals = tableVar.getAnnotation("qualities");
						String newQuals = currentQuals + "," + var.getQuality();
						tableVar.addAnnotation("qualities", newQuals);
						
						Double analTypeCount = tableVar.getProperty(analysisTypeStr);
						if (analTypeCount == null) {
							tableVar.addProperty(analysisTypeStr, 1.0);
						}
						else {
							tableVar.addProperty(analysisTypeStr, analTypeCount+1);
						}
						
						if (! var.getAlt().equals(tableVar.getAlt())) {
							tableVar.setAlt(tableVar.getAlt() + "," + var.getAlt());
						}
					}


				} while(reader.advanceLine());
				
				tmpPool.sortAllContigs();
				hugePool.addAll(tmpPool);
			}
			catch(IOException ex) {
				System.err.println("Warning, could not import variants from file " + args[i] + ":" + ex.getMessage());
			}
		}
		
		//Emit pool to system.out
		System.out.print("#contig\tpos\tref\talt\ttot.samples\tmean.quality\tstdev.quality");
		for(String analysisType : analysisTypes) {
			System.out.print("\t" + analysisType);
		}
		System.out.println();
		
		VariantPool finalPool = new VariantPool();
		
		for(String contig : hugePool.getContigs()) {
			for(VariantRec var : hugePool.getVariantsForContig(contig)) {
				double meanQual = calcMeanQual(var);
				double stdevQual = calcStdevQual(var, meanQual);
				if (var.getProperty(VariantRec.SAMPLE_COUNT) > 5.0 && stdevQual / meanQual > 0.5) {
					finalPool.addRecordNoSort(var);
					System.out.print(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt() + "\t" + var.getProperty(VariantRec.SAMPLE_COUNT) + "\t" + meanQual + "\t" + stdevQual);

					for(String analysisType : analysisTypes) {
						Double ac = var.getProperty(analysisType);
						if (ac == null)
							ac = 0.0;
						System.out.print("\t" + ac);
					}
					
					System.out.println();
				}
			}
		}
		
		finalPool.sortAllContigs();
		System.out.println("Size of final pool: " + finalPool.size());
		System.out.println("TT ratio: " + finalPool.computeTTRatio());
	}
	
	private static double calcStdevQual(VariantRec var, double mean) {
		String[] qStrs = var.getAnnotation("qualities").split(",");
		
		double sumSqrs = 0;
		double count = 0;
		for(int i=0; i<qStrs.length; i++) {
			try {
				double val = Double.parseDouble(qStrs[i]);
				sumSqrs += (val-mean)*(val-mean);
				count++;
			} catch (NumberFormatException nfe) {
				//
			}
		}
		if (count == 0) {
			return Double.NaN;
		}
		else {
			return Math.sqrt(sumSqrs) / count;
		}
	}

	private static double calcMeanQual(VariantRec var) {
		String[] qStrs = var.getAnnotation("qualities").split(",");
		
		double sum = 0;
		double count = 0;
		for(int i=0; i<qStrs.length; i++) {
			try {
				double val = Double.parseDouble(qStrs[i]);
				sum += val;
				count++;
			} catch (NumberFormatException nfe) {
				//
			}
		}
		if (count == 0) {
			return Double.NaN;
		}
		else {
			return sum / count;
		}
	}
	
	private static void performGeneFilter(String[] args) {
		if (args.length < 3) {
			System.out.println("Enter the name of the file containing gene names, then one or more variant files");
			return;
		}
		try {
			GenePool genes = new GenePool(new File(args[1]));
			VariantLineReader reader = getReader(args[2]);
			System.out.println(reader.getHeader().trim());
			do {
				VariantRec var = reader.toVariantRec();
				String gene = var.getAnnotation(VariantRec.GENE_NAME);
				if (gene != null && genes.containsGene(gene))
					System.out.println( reader.getCurrentLine() );
			}while(reader.advanceLine());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performPropGeneComp(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the name of the file containing gene names, then one property to extract, then the variant files");
			return;
		}
		try {
			String key = args[2];
			GenePool genes = new GenePool(new File(args[1]));
			for(int i=3; i<args.length; i++) {
				VariantPool vars = getPool(new File(args[i]));
				VariantPool geneVars = filterByGene(vars, genes, false);
				VariantPool nonGeneVars = filterByGene(vars, genes, true);
				
				
				for(String contig : geneVars.getContigs()) {
					for(VariantRec var : geneVars.getVariantsForContig(contig)) {
						if (var.getAnnotation(VariantRec.EXON_FUNCTION) != null && var.getAnnotation(VariantRec.EXON_FUNCTION).contains("nonsynon"))
							try {
								Double val = Double.parseDouble( var.getPropertyOrAnnotation(key) );
								System.out.println(val);
							}
							catch (NumberFormatException nfe) {
								//Do nothing
							}
					}
				}
				
				for(String contig : nonGeneVars.getContigs()) {
					for(VariantRec var : nonGeneVars.getVariantsForContig(contig)) {
						if (var.getAnnotation(VariantRec.EXON_FUNCTION) != null && var.getAnnotation(VariantRec.EXON_FUNCTION).contains("nonsynon")) {
							try {
								Double val = Double.parseDouble( var.getPropertyOrAnnotation(key) );
								System.err.println(val);
							}
							catch (NumberFormatException nfe) {
								//Do nothing
							}
							
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Emit only the property or annotation given as arg #2 for variants present in the gene list for all input files
	 * Works just like extract, but only emits for the desired genes
	 * @param args
	 */
	private static void performGenePropExtract(String[] args, boolean reverse) {
		if (args.length < 4) {
			System.out.println("Enter the name of the file containing gene names, then one property to extract, then the variant files");
			return;
		}
		DecimalFormat formatter = new DecimalFormat("0.00000");
		try {
			String key = args[2];
			GenePool genes = new GenePool(new File(args[1]));
			int hitsInTarget = 0;
			int hitsNonTarget = 0;
			double cutoff = Double.parseDouble(args[3]);
			double sampleCount = 0.0;
			for(int i=4; i<args.length; i++) {
				VariantLineReader reader = getReader(args[i]);
				sampleCount++;
				do {
					VariantRec var = reader.toVariantRec();
					Double prop = var.getProperty(key);
					String gene = var.getAnnotation(VariantRec.GENE_NAME);
					int alleles = 1;
					if (! var.isHetero())
						alleles = 2;
					
					if (gene != null && prop != null) {
						if (genes.containsGene(gene)) {
							if (prop > cutoff)
								hitsInTarget += alleles;
						}
						else {
							if (prop > cutoff)
								hitsNonTarget += alleles;
						}	
					}
						
				} while(reader.advanceLine());
				
//				VariantPool vars = getPool(new File(args[i]));
//				VariantPool geneVars = filterByGene(vars, genes, reverse);
//				for(String contig : geneVars.getContigs()) {
//					for(VariantRec var : geneVars.getVariantsForContig(contig)) {
//						if (var.isHetero())
//							System.out.println("1\t" + var.getPropertyOrAnnotation(key));
//						else 
//							System.out.println("2\t" + var.getPropertyOrAnnotation(key));
//					}
//				}
				
			}
			System.out.println("Hits in target:/ non target: \t" + hitsInTarget + "\t" + hitsNonTarget + "\t" +formatter.format((double)hitsInTarget / (double)hitsNonTarget));
			System.out.println("Samples : " + sampleCount + " Alleles: " + sampleCount*2);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * An HHT-study specific function, assumes arg 4 is a tkgdata file
	 * @param args
	 */
	private static void performTKGGenePropExtract(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the name of the file containing gene names, then one property to extract, then the variant files");
			return;
		}
		try {
			String key = args[2];
			GenePool genes = new GenePool(new File(args[1]));
			double popInTarget = 0;
			double popNonTarget = 0;
			double amrInTarget = 0;
			double amrNonTarget = 0;
			double eurInTarget = 0;
			double eurNonTarget = 0;
			double cutoff = Double.parseDouble(args[3]);
			VariantLineReader reader = getReader(args[4]);
			do {
				VariantRec var = reader.toVariantRec();
				Double prop = var.getProperty(key);
				Double tkgFreq = var.getProperty("tkg.freq");
				Double eurFreq = var.getProperty("eur.freq");
				Double amrFreq = var.getProperty("amr.freq");
				String gene = var.getAnnotation(VariantRec.GENE_NAME);

				if (gene != null && prop != null && tkgFreq != null) {
					if (genes.containsGene(gene)) {
						if (prop > cutoff) {
							popInTarget += tkgFreq;
							amrInTarget += amrFreq;
							eurInTarget += eurFreq;
						}
					}
					else {
						if (prop > cutoff) {
							popNonTarget += tkgFreq;
							amrNonTarget += amrFreq;
							eurNonTarget += eurFreq;
						}
					}

				} 
			} while(reader.advanceLine());

			System.out.println("TKG hits in target / nontarget : " + popInTarget + "\t" + popNonTarget + "\t" + popInTarget/popNonTarget);
			System.out.println("AMR hits in target / nontarget : " + amrInTarget + "\t" + amrNonTarget + "\t" + amrInTarget/amrNonTarget);
			System.out.println("EUR hits in target / nontarget : " + eurInTarget + "\t" + eurNonTarget + "\t" + eurInTarget/eurNonTarget);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performHapCompare(String[] args) {
		if (args.length != 3) {
			System.out.println("Enter the names of the sample-from-hapmap csv file and the query file to compare");
			return;
		}

		DecimalFormat formatter = new DecimalFormat("#0.000");

		try {
			VariantPool hapmap = getPool(new File(args[1]));
			VariantPool sample = getPool(new File(args[2]));
			
			int falsePoz = 0;
			int falseNeg = 0;
			int truePoz = 0;
			int trueNeg = 0;
			int totHapMapVar = 0;
			int wrongZygosity = 0;
			
			VariantPool falsePosPool = new VariantPool();
			VariantPool falseNegPool = new VariantPool();
			List<String> falseNegList = new ArrayList<String>();
			
			for(String contig : hapmap.getContigs()) {
				for(VariantRec var : hapmap.getVariantsForContig(contig)) {
					VariantRec sampleVar = sample.findRecordNoWarn(contig, var.getStart());
					if (var.isVariant() && sampleVar == null) {
						totHapMapVar++;
						falseNeg++;
						falseNegList.add(contig + "\t" + var.getStart() + "\t-\t" + var.getRef() + "\t" + var.getAlt());
					}
					if (var.isVariant() && sampleVar != null) {
						totHapMapVar++;
						truePoz++;
						int flag = 0;
						if (var.isHetero())
							flag++;
						if (sampleVar.isHetero())
							flag++;
						if (flag==1)
							wrongZygosity++;
					}
					if ((!var.isVariant()) && sampleVar==null) {
						trueNeg++;
					}
					if ((!var.isVariant()) && sampleVar!=null) {
						falsePoz++;
						falsePosPool.addRecord(sampleVar);
					}
					
				}
				
			}
			
			System.out.println("Found " + hapmap.size() + " sites from hapmap");
			System.out.println("Of these " + totHapMapVar + " were non-reference");
			System.out.println("Total true positives: " + truePoz);
			System.out.println("Total true negatives: " + trueNeg);
			System.out.println("Total false positives: " + falsePoz);
			System.out.println("Total false negatives: " + falseNeg);
			System.out.println("Total true positives, but wrong zygosity : " + wrongZygosity);
			
			double falsePosRate = (double)falsePoz / (double)(hapmap.size() - totHapMapVar);
			double falseNegRate = (double)falseNeg / (double)(totHapMapVar);
			System.out.println(" False positive percentage : " + formatter.format(100* falsePosRate) + "%" );
			System.out.println(" False negative percentage : " + formatter.format(100* falseNegRate) + "%" );
			
			System.out.println("False positives TT ratio : " + falsePosPool.computeTTRatio());
			
//			System.out.println("False negatives:");
//			for(String str : falseNegList) {
//				System.out.println(str);
//			}

			System.out.println("False positives:");
			
			falsePosPool.listAll(System.out, Arrays.asList(new String[]{VariantRec.FS_SCORE, VariantRec.FALSEPOS_PROB, VariantRec.TAUFP_SCORE}));
			
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performCompare(String[] args) {
		if (args.length != 3) {
			System.out.println("Enter the names of two variant (vcf or csv) files to compare");
			return;
		}
		
		try {
			File fileA = new File(args[1]);
			File fileB = new File(args[2]);
			DecimalFormat formatter = new DecimalFormat("#0.000");
			
			VariantPool varsA = getPool(new File(args[1]));
			VariantPool varsB = getPool(new File(args[2]));
			
			System.out.println("Total variants in " + fileA.getName() + " : " + varsA.size() + " mean quality: " + formatter.format(varsA.meanQuality()));
			System.out.println("Total variants in " + fileB.getName() + " : " + varsB.size() + " mean quality: " + formatter.format(varsB.meanQuality()));
			
			int hetsA = varsA.countHeteros();
			int hetsB = varsB.countHeteros();
			System.out.println("Heterozyotes in " + fileA.getName() + " : " + hetsA + " ( " + formatter.format(100.0*(double)hetsA/(double)varsA.size()) + " % )");
			System.out.println("Heterozyotes in " + fileB.getName() + " : " + hetsB +  " ( " + formatter.format(100.0*(double)hetsB/(double)varsB.size()) + " % )");
		
			System.out.println("Transitions / Transversions in " + fileA.getName() + " : " + varsA.countTransitions() + " / " + varsA.countTransverions() + "\t ratio: " + formatter.format(varsA.computeTTRatio()));
			System.out.println("Transitions / Transversions in " + fileB.getName() + " : " + varsB.countTransitions() + " / " + varsB.countTransverions() + "\t ratio: " + formatter.format(varsB.computeTTRatio()));
						
			CompareVCF.compareVars(varsA, varsB, System.out);
			
			VariantPool intersection = (VariantPool) varsA.intersect(varsB);
			
			VariantPool uniqA = new VariantPool(varsA);
			VariantPool uniqB = new VariantPool(varsB);
			for(String contig : varsB.getContigs()) {
				for(VariantRec var : varsB.getVariantsForContig(contig)) {
					uniqA.removeVariantAllele(var.getContig(), var.getStart(), var.getAlt());
				}
			}
			
			for(String contig : varsA.getContigs()) {
				for(VariantRec var : varsA.getVariantsForContig(contig)) {
					uniqB.removeVariantAllele(var.getContig(), var.getStart(), var.getAlt());
				}
			}
			
			System.out.println("\nTotal intersection size: " + intersection.size());
			
			System.out.println("Number of variants unique to " + fileA.getName() + " : " + uniqA.size());
			System.out.println("Number of variants unique to " + fileB.getName() + " : " + uniqB.size());
			
			System.out.println("TT ratio in variants unique to " + fileA.getName() + " : " + uniqA.computeTTRatio());
			System.out.println("TT ratio in variants unique to " + fileB.getName() + " : " + uniqB.computeTTRatio());

			System.out.println("%Intersection in " + fileA.getName() + " : " + formatter.format( (double)intersection.size() / (double)varsA.size()));
			System.out.println("%Intersection in " + fileB.getName() + " : " + formatter.format( (double)intersection.size() / (double)varsB.size()));
			
			System.out.println("Mean quality of sites in intersection: " + formatter.format(intersection.meanQuality()));
			System.out.println("Mean quality of sites in A but not in intersection: " + formatter.format(uniqA.meanQuality()));
			System.out.println("Mean quality of sites in B but not in intersection: " + formatter.format(uniqB.meanQuality()));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void performComputeFP(String[] args) {
		try {
			List<String> annos = new ArrayList<String>();
			annos.add(VariantRec.FALSEPOS_PROB);
			
			System.out.print(VariantRec.getSimpleHeader() );
			for(String key : annos) 
				System.out.print("\t" + key);
			System.out.println();
			
			for(int i=1; i<args.length; i++) {		
				VariantLineReader reader;
				if (args[i].endsWith(".csv")) 
					reader = new CSVLineReader(new File(args[i]));
				else
					reader = new VCFLineParser(new VCFFile(new File(args[i])));
				
				do {
					VariantRec rec = reader.toVariantRec();
					Double fpScore = FPComputer.computeFPScore(rec);
					if (! Double.isNaN(fpScore)) {
						rec.addProperty(VariantRec.FALSEPOS_PROB, fpScore);
					}
					System.out.println(rec.toSimpleString() + "\t" + rec.getProperty(VariantRec.FALSEPOS_PROB));
				} while(reader.advanceLine());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performIntersection(String[] args) {
		if (args.length < 3) {
			System.out.println("Enter the names of two (or more) variant (vcf or csv) files to intersect");
			return;
		}
		
		int tried = 0;
		int found = 0;
		
		try {
			VariantLineReader vars = getReader(args[1]);
			
			System.out.println(vars.getHeader().trim());

			
			VariantPool[] pools = new VariantPool[args.length-2];
			for(int i=2; i<args.length; i++) {
				pools[i-2] = getPool(new File(args[i]));
			}
			
				do {
					VariantRec var  = vars.toVariantRec();
					if (var == null)
						continue;
					tried++;
					boolean missing = false;
					for(int i=0; i<pools.length; i++) {
						//VariantPool pool = getPool(new File(args[i]));
						if (pools[i].findRecordNoWarn(var.getContig(), var.getStart()) == null) {
							missing = true;
							break;
						}
					}
					if (!missing) {
						System.out.println(vars.getCurrentLine());
						found++;
					}
				} while(vars.advanceLine());
			

			System.err.println("Found " + found + " intersecting variants in pool of size " + tried);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performSubtraction(String[] args) {
		if (args.length < 3) {
			System.out.println("Enter the names of two or more variant (vcf or csv) files to subtract.");
			System.out.println("If more than two, result will be A - B - C... - N, so all subsequence files are subtracted from first given file");
			return;
		}
		
		List<VariantPool> pools = new ArrayList<VariantPool>();
		
		for(int i=2; i<args.length; i++) {
			try {
				pools.add( getPool(new File(args[i])) );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		try {
			VariantLineReader baseVars = getReader(args[1]);
			System.out.println(baseVars.getHeader().trim());
			
			do {
				VariantRec var = baseVars.toVariantRec();
				if (var == null)
					continue;
				
				boolean subtract = false;
				for(VariantPool pool : pools) {
					if (pool.findRecordNoWarn(var.getContig(), var.getStart()) != null) {
						subtract = true;
						break;
					}
				}
				
				if (! subtract) {
					System.out.println(baseVars.getCurrentLine());
				}
								
			} while(baseVars.advanceLine());
			
			
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performHomFilter(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of a variant (vcf or csv) file to filter");
			return;
		}
		
		try {
			VariantPool varsA = getPool(new File(args[1]));
			VariantPool homos = new VariantPool(varsA.filterPool(VarFilterUtils.getHomoFilter()));
			File outputFile = null;
			if (args.length==3) {
				outputFile = new File(args[2]);
			}
			
			PrintStream outputStream = System.out;
			if (outputFile != null) {
				outputStream = new PrintStream(new FileOutputStream(outputFile));
			}
			homos.listAll(outputStream);
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

	
	private static void performEmitQualityTiTv(String[] args) throws IOException {
		
		
		VariantPool pool = getPool(new File(args[1]));
		double maxQuality = 0;
		for(String contig : pool.getContigs()) {
			for (VariantRec var : pool.getVariantsForContig(contig)) {
				if (var.getQuality() > maxQuality) 
					maxQuality = var.getQuality();
			}
		}
//		List<VariantRec> vars = pool.toList();
//		
//		Collections.sort(vars, new Comparator<VariantRec>() {
//			@Override
//			public int compare(VariantRec o1, VariantRec o2) {
//				if (o1.getQuality() == o2.getQuality())
//					return 0;
//				
//				return o1.getQuality() < o2.getQuality() ? -1 : 1;
//			}
//		});
		
		
		double[] quals = new double[]{0.1, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 8.0, 10.0, 15.0, 20.0, 25.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0};
		
		
		for(int i=0; i<quals.length; i++) {
			double q = quals[i];
			VariantPool qPool = new VariantPool( pool.filterPool(VarFilterUtils.getQualityFilter(q)));
			double hetFrac = (double)qPool.countHeteros() / (double)qPool.size();
			System.out.println(q + "\t" + qPool.size() + "\t" + qPool.computeTTRatio() + "\t" + hetFrac);
			
		}
	}
	
	private static void performBuildROC(String[] args, boolean useVQSR) {
		if (args.length != 4) {
			System.out.println("Enter the name of the BED file, then the TRUE variant then query variant file");
			return;
		}
		
		DecimalFormat formatter = new DecimalFormat("0.0000#####");
		
		try {
			BEDFile bedFile = new BEDFile(new File(args[1]));
			bedFile.buildIntervalsMap();
			VariantPool trueVars = getPool(new File(args[2]));
			VariantPool qVars = getPool(new File(args[3]));
						
			double minQuality = Double.MAX_VALUE;
			double maxQuality = 0.0;
//			//find maximum quality
			for(String contig : qVars.getContigs()) {
				for(VariantRec var : qVars.getVariantsForContig(contig)) {
					double qual = var.getQuality();
					if (useVQSR) {
						if (var.getProperty(VariantRec.VQSR) == null) 
							continue;
						qual = var.getProperty(VariantRec.VQSR);
						
					}
					if (qual > maxQuality)
						maxQuality = qual;
					if (qual < minQuality) 
						minQuality = qual;
				}
			}
			
			if (minQuality < 0.0) {
				minQuality = -0.0;
			}
			if (maxQuality > 1000.0) {
				maxQuality = 1000.0;
			}
			
			if (minQuality == 0.0) {
				minQuality = 0.000001;
			}
			
			System.out.println("Min quality: " + minQuality);
			System.out.println("Max quality: " + maxQuality);
			
			
			List<Double> xVals = new ArrayList<Double>();
			List<Double> yVals = new ArrayList<Double>();
			
			double prevSpec = 0.0;
			double prevSens = 0.0;
			for(double qCutoff = minQuality; qCutoff < maxQuality; qCutoff *= 1.1) {
				
				int falsePositives = 0;
				int truePositives = 0;
				int falseNegatives = 0;
				int trueNegatives = 0;

				//Compute true positives and false negatives
				for(String contig : trueVars.getContigs()) {
					for(VariantRec tVar : trueVars.getVariantsForContig(contig)) {
						if (bedFile.contains(contig, tVar.getStart())) {
							VariantRec qVar = qVars.findRecordNoWarn(contig, tVar.getStart());
							if (qVar == null) {
								falseNegatives++;
								continue;
							}
							double qual = qVar.getQuality();
							if (useVQSR) {
								if (qVar.getProperty(VariantRec.VQSR) == null) 
									continue;
								qual = qVar.getProperty(VariantRec.VQSR);
							}
							
							if (qVar != null && qual >= qCutoff)
								truePositives++;
							else 
								falseNegatives++;
						}
					}
				}

				//Compute false positives, true negs don't exist!
				for(String contig : qVars.getContigs()) {
					for(VariantRec qVar : qVars.getVariantsForContig(contig)) {
						double qual = qVar.getQuality();
						if (useVQSR) {
							if (qVar.getProperty(VariantRec.VQSR) == null) 
								continue;
							qual = qVar.getProperty(VariantRec.VQSR);
						}
						if (qual >= qCutoff) {
							if (bedFile.contains(contig, qVar.getStart())) {
								if (! trueVars.contains(contig, qVar.getStart()))
									falsePositives++;
							}
						}
					}
				}


				trueNegatives = bedFile.getExtent() - truePositives; //Approximate 
				double sensitivity = (double)truePositives / (double)(truePositives + falseNegatives);
				double specificity = (double)trueNegatives / (double)(trueNegatives + falsePositives);

				if (prevSpec != specificity || prevSens != sensitivity) {
					System.out.println(formatter.format(qCutoff) + "\t" + formatter.format(1.0-specificity) +"\t" + sensitivity);
				}
				prevSpec = specificity;
				prevSens = sensitivity;
				xVals.add(1.0-specificity);
				yVals.add(sensitivity);
			}
			
			Collections.reverse(xVals);
			Collections.reverse(yVals);
			double area = Integration.trapezoidQuad(xVals.toArray(new Double[]{}), yVals.toArray(new Double[]{}), 6.0e-05);
			System.out.println("Approximate AUC : " + area);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		
		
	}
	private static void performBedFilter(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of a variant (vcf or csv) file and a bed file to filter by");
			return;
		}

		int count = 0;
		int retained = 0;
		
		try {
			//VariantLineReader reader = getReader(args[1]);
			BufferedReader reader = new BufferedReader(new FileReader(args[1]));
			BEDFile bedFile = new BEDFile(new File(args[2]));
			bedFile.buildIntervalsMap();

			String line = reader.readLine();
			while(line != null && line.startsWith("#")) {
				System.out.println(line);
				line = reader.readLine();
			}
			
			while(line != null && line.length()>0) {
				String[] toks = line.split("\t");
				String contig = toks[0];
				Integer pos = Integer.parseInt(toks[1]);
				
				if (bedFile.contains(contig, pos, false))	{
					retained++;
					System.out.println(line);
				}
				count++;

				line = reader.readLine();
			};
						
			reader.close();	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.err.println("Retained " + retained + " of " + count + " variants");
		return;
	}

	private static void performMergeBED(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of two bed files to combine");
			return;
		}
		
		BEDFile bedA = new BEDFile(new File(args[1]));
		BEDFile bedB = new BEDFile(new File(args[2]));
		
		System.err.println(args[1] + " : " + bedA.getIntervalCount() + " intervals, " + bedA.getExtent() + " bases");
		System.err.println(args[2] + " : " + bedB.getIntervalCount() + " intervals, " + bedB.getExtent() + " bases");
		
		bedA.mergeIntervalsFrom(bedB);
		System.err.println("Union : " + bedA.getIntervalCount() + " intervals, " + bedA.getExtent() + " bases");
		
		bedA.toBED(System.out);
	}
	
	private static void performHetFilter(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of a variant (vcf or csv) file to filter");
			return;
		}
		
		try {
			VariantPool varsA = getPool(new File(args[1]));
			VariantPool homos = new VariantPool(varsA.filterPool(VarFilterUtils.getHeteroFilter()));
			File outputFile = null;
			if (args.length==3) {
				outputFile = new File(args[2]);
			}
			
			PrintStream outputStream = System.out;
			if (outputFile != null) {
				outputStream = new PrintStream(new FileOutputStream(outputFile));
			}
			homos.listAll(outputStream);
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

	public static int toIndex(char c) {
		if (c=='A') {
			return A;
		}
		if (c=='C') {
			return C;
		}
		if (c=='T') {
			return T;
		}
		if (c=='G') {
			return G;
		}
		return -1;
	}
	
	private static void performComputeTMatrix(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files for which to build the pool");
			return;
		}

		DecimalFormat formatter = new DecimalFormat("0.000");
		double[][] matrix = new double[4][4];
		double tot = 0;
		
		try {
			for(int i=1; i<args.length; i++) {
				VariantPool vars = getPool(new File(args[i]));
				for(String contig : vars.getContigs()) {
					for(VariantRec var : vars.getVariantsForContig(contig)) {
						if (var.isSNP()) {
							char ref = var.getRef().charAt(0);
							char alt = var.getAlt().charAt(0);
							matrix[toIndex(ref)][toIndex(alt)]++;
							tot++;
						}
					}
				}

			}
			System.out.println("\tA \t C\t G\t T");
			for(int i=0; i<4; i++) {
				
				for(int j=0; j<4; j++) {
					System.out.print("\t" + formatter.format(matrix[i][j]/tot));
				}
				System.out.println();
			}
			
			System.out.println("Counted " + tot + " snps total");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private static void performGeneComp(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files for which to build the pool");
			return;
		}
		
		List<File> varFiles = new ArrayList<File>();
		
		int cutoff = 2;
		int startIndex = 1;
		try {
			cutoff = Integer.parseInt(args[1]);
			startIndex = 2;
			System.err.println("Found cutoff : " + cutoff);
		}
		catch (NumberFormatException nfe) {
			
		}
		
		for(int i=startIndex; i<args.length; i++) {
			varFiles.add(new File(args[i]));
		}
		
		try {
			compareGeneSets(cutoff, varFiles);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

//	private static void performTsTvByQuality(String[] args) throws IOException {
//		VariantLineReader reader = getReader( args[1] );
//		final int BINS = 10;
//		List<VariantRec>[] bins = new (ArrayList<VariantRec>)[BINS];
//		
//	}
	
	private static void performNovelFilter(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files to examine");
			return;
		}

		double frequencyCutoff = 1e-5;
		VariantLineReader reader = getReader( args[1] );
		System.out.print( reader.getHeader() );
		do {
			boolean passes = false;
			VariantRec var = reader.toVariantRec();
			if (var.getProperty(VariantRec.POP_FREQUENCY) == null || (var.getProperty(VariantRec.POP_FREQUENCY) != null && var.getProperty(VariantRec.POP_FREQUENCY) < frequencyCutoff)) {
				passes = true;
				
				//If variant was found in ESP5400 and with a relatively high frequency, it doesn't pass
				if (var.getProperty(VariantRec.EXOMES_FREQ) != null && var.getProperty(VariantRec.EXOMES_FREQ) > frequencyCutoff) {
					passes = false;
				}
			}
			
			if (passes) {
				System.out.println(reader.getCurrentLine());
			}
			
		} while(reader.advanceLine());
		
		
		
		return;
	}

	private static void performExtract(String[] args) {
		String prop = args[1];
		VariantPool pool;
		for(int i=2; i<args.length; i++) {
			try {

				pool = getPool(new File(args[i]));
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						String val = var.getPropertyOrAnnotation(prop);
						if (var.isHetero())
							System.out.println("1\t" + val);
						else 
							System.out.println("2\t" + val);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return;
	}

	private static void performSampleCount(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files to emit sample counts for");
			return;
		}
		
		try {
			VariantPool union = new VariantPool();
			
			for(int i=1; i<args.length; i++) {
				System.err.println("Adding file : " + args[i]);
				VariantLineReader reader = getReader(args[i]);
				do {
					VariantRec var = reader.toVariantRec();
					VariantRec uVar = union.findRecordNoWarn(var.getContig(), var.getStart());
					if (uVar == null) {
						var.addProperty(VariantRec.SAMPLE_COUNT, 1.0);
						union.addRecord(var);
					}
					else {
						Double count = uVar.getProperty(VariantRec.SAMPLE_COUNT);
						count++;
						uVar.addProperty(VariantRec.SAMPLE_COUNT, count);
					}
				} while(reader.advanceLine());
				
			}
			
			System.out.println(VariantRec.getSimpleHeader() + "\tsample.count");
			for(String contig : union.getContigs()) {
				for(VariantRec var : union.getVariantsForContig(contig)) {
					if (var.getProperty(VariantRec.SAMPLE_COUNT) > 0) {
						System.out.println(var.toSimpleString() + "\t" + var.getPropertyOrAnnotation(VariantRec.SAMPLE_COUNT) );
						//System.out.println(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt() + "\t" + var.getPropertyOrAnnotation(VariantRec.SAMPLE_COUNT));
					}
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static void performSort(String[] args) {
		if (args.length != 2) {
			System.out.println("Enter the name of a single vcf or csv file to sort.");
			return;
		}
		
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(args[1]));
			List<String> lines = new ArrayList<String>(1024);
			
			String line = reader.readLine();
			while(line != null) {
				if (line.startsWith("#")) {
					System.out.println(line);
				}
				else {
					lines.add(line);
				}
				line = reader.readLine();
			}
			
			reader.close();
			
			final List<String> refList = Arrays.asList(refOrder);
			
			Collections.sort(lines, new Comparator<String>() {

				@Override
				public int compare(String v0, String v1) {
					String[] toks0 = v0.split("\t");
					String[] toks1 = v1.split("\t");
					
					String chr0 = toks0[0];
					String chr1 = toks1[0];
					if (! chr0.equals(chr1)) {
						Integer val0 = refList.indexOf(chr0);
						Integer val1 = refList.indexOf(chr1);
						
						return val0.compareTo(val1);
					}
					else {
						Integer pos0 = Integer.parseInt(toks0[1]);
						Integer pos1 = Integer.parseInt(toks1[1]);
						return pos0.compareTo(pos1);
					}
				}
				
			});
			
			
			for(String output : lines) {
				System.out.println(output);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void performCombine(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files to combine");
			return;
		}
		
		try {
			VariantPool pool = getPool(new File(args[1]));
			for(int i=2; i<args.length; i++) {
				System.err.println("Adding file : " + args[i]);
				VariantLineReader reader = getReader(args[i]);
				do {
					VariantRec var = reader.toVariantRec();
					VariantRec existing = pool.findRecord(var.getContig(), var.getStart(), var.getRef(), var.getAlt());
					if (existing == null) {
						pool.addRecord(var);
						pool.sortAllContigs();
					}
				} while(reader.advanceLine());
				pool.sortAllContigs();
			}
			
			
			PrintStream outputStream = System.out;
			
			List<String> annoKeys = new ArrayList<String>();
			annoKeys.add(VariantRec.RSNUM);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.GENE_NAME);
			annoKeys.add(VariantRec.VARIANT_TYPE);
			annoKeys.add(VariantRec.EXON_FUNCTION);
			annoKeys.add(VariantRec.CDOT);
			annoKeys.add(VariantRec.PDOT);
			annoKeys.add(VariantRec.VQSR);
			annoKeys.add(VariantRec.DEPTH);
			annoKeys.add(VariantRec.VAR_DEPTH);
			annoKeys.add(VariantRec.FALSEPOS_PROB);
			annoKeys.add(VariantRec.FS_SCORE);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.EFFECT_PREDICTION2);
			pool.listAll(outputStream, annoKeys);
			outputStream.close();
			System.err.println("Final pool has " + pool.size() + " variants");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	
	
	private static void performRemoveDuplicates(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files to combine");
			return;
		}
		
		try {
			VariantPool pool = getPool(new File(args[1]));
			for(int i=2; i<args.length; i++) {
				System.err.println("Adding file : " + args[i]);
				VariantLineReader reader = getReader(args[i]);
				do {
					VariantRec var = reader.toVariantRec();
					pool.addRecord(var);
				} while(reader.advanceLine());
				pool.sortAllContigs();
			}
			
			
			pool.removeDuplicates();
			
			PrintStream outputStream = System.out;
			
			List<String> annoKeys = new ArrayList<String>();
			annoKeys.add(VariantRec.RSNUM);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.GENE_NAME);
			annoKeys.add(VariantRec.VARIANT_TYPE);
			annoKeys.add(VariantRec.EXON_FUNCTION);
			annoKeys.add(VariantRec.CDOT);
			annoKeys.add(VariantRec.PDOT);
			annoKeys.add(VariantRec.VQSR);
			annoKeys.add(VariantRec.DEPTH);
			annoKeys.add(VariantRec.VAR_DEPTH);
			annoKeys.add(VariantRec.FALSEPOS_PROB);
			annoKeys.add(VariantRec.FS_SCORE);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.EFFECT_PREDICTION2);
			pool.listAll(outputStream, annoKeys);
			outputStream.close();
			System.err.println("Final pool has " + pool.size() + " variants");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	

	private static void performSummary(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files to examine");
			return;
		}
		
		Histogram varDepthHisto = new Histogram(0, 1.0, 50); 
		Histogram readDepthHisto = new Histogram(1, 250, 100); 
		double qualitySum = 0;
		double tstvSum = 0;
		double hetSum = 0;
		double varSum = 0;
		
		for(int i=1; i<args.length; i++) {
			try {
				DecimalFormat formatter = new DecimalFormat("#0.00");
				if (i>1)
					System.out.println("\n\n");
				VariantPool pool = getPool(new File(args[i]));
				
				double minQual = Double.MAX_VALUE;
				double maxQual = Double.MIN_VALUE;
				for(String contig : pool.getContigs()) {
					for(VariantRec rec : pool.getVariantsForContig(contig)) {
						double q = rec.getQuality();
						if (q< minQual) {
							minQual = q;
						}
						if (q>maxQual) {
							maxQual = q;
						}
					}
				}
				
				System.out.println("Summary for file : " + args[i]);
				System.out.println("Total variants : " + pool.size());
				System.out.println("Mean variant quality : " + pool.meanQuality());
				System.out.println("\t	Min quality : " + minQual);
				System.out.println("\t	Max quality : " + maxQual);
				System.out.println("Total SNPs : " + pool.countSNPs());
				System.out.println("Total indels: " + (pool.countInsertions() + pool.countDeletions()));
				System.out.println("\t insertions: " + pool.countInsertions());
				System.out.println("\t deletions: " + pool.countDeletions());
				System.out.println(" Ts / Tv ratio: " + pool.computeTTRatio());
				
				
				qualitySum += pool.meanQuality()*pool.size();
				tstvSum += pool.computeTTRatio()*pool.size();
				hetSum += pool.countHeteros();
				varSum += pool.size();
				
				int heteros = pool.countHeteros();
				System.out.println(" Heterozygotes : " + heteros + " (" + formatter.format((double)heteros / (double)pool.size()) + "% )");
				
//				
//				VariantPool q30Pool = new VariantPool(pool.filterPool(new VariantFilter() {
//					public boolean passes(VariantRec rec) {
//						return rec.getQuality() > 29.99;
//					}
//				}));
//				
//				System.out.println("\n Variants with Q > 30 : ");
//				System.out.println("Total variants (q30): " + q30Pool.size());
//				System.out.println("Mean variant quality (q30): " + q30Pool.meanQuality());
//				System.out.println("Total SNPs (q30): " + q30Pool.countSNPs());
//				System.out.println("Total indels (q30): " + (q30Pool.countInsertions() + q30Pool.countDeletions()));
//				System.out.println("\t insertions (q30): " + q30Pool.countInsertions());
//				System.out.println("\t deletions (q30): " + q30Pool.countDeletions());
//				System.out.println(" Ts / Tv ratio (q30): " + q30Pool.computeTTRatio());
//				heteros = q30Pool.countHeteros();
//				System.out.println(" Heterozygotes : " + heteros + " (" + formatter.format((double)heteros / (double)q30Pool.size()) + "% )");

				
				//computeVarDepthHisto(pool, varDepthHisto);
				//computeReadDepthHisto(pool, readDepthHisto);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
		double qualityMean = qualitySum / varSum;
		double hetMean = hetSum / varSum;
		double tstvMean = tstvSum / varSum;
		
		if (args.length > 2) {
			System.out.println("Overall statistics :");
			System.out.println("Total variants examined :" + varSum);
			System.out.println("Mean quality score : " + qualityMean);
			System.out.println("Hetero fraction:" + hetMean);
			System.out.println("Ts/Tv mean:" + tstvMean);
		}
		
		//System.out.println("Histogram of variant frequencies:");
		//System.out.println(varDepthHisto.toString());
//		System.out.println("Histogram of read depths :");
//		System.out.println(readDepthHisto.toString());
		//System.out.println(varDepthHisto.freqsToCSV());
		
		return;
	}

	private static void performHistogram(String[] args) {
		String prop = args[1];
		LazyHistogram hist = new LazyHistogram(20);
		VariantPool pool;
		try {
			for(int i=2; i<args.length; i++) {
				pool = getPool(new File(args[i]));
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						String val = var.getPropertyOrAnnotation(prop);
						
						//Var.freq is special case
						if (prop.equals("var.freq")) {
							Double depth = var.getProperty(VariantRec.DEPTH);
							Double altDepth = var.getProperty(VariantRec.VAR_DEPTH);
							if (depth != null && altDepth != null) {
								val = "" + altDepth / depth;
							}
						}
						if (val != null && (!val.equals("-"))) {
							try {
								Double x = Double.parseDouble(val);
								hist.addValue(x);
							}
							catch (NumberFormatException nfe) {
								System.err.println("ERROR: could not parse value from : " + val);
							}
						}

					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		System.out.println(hist.toString());
		return;
	}

	/**
	 * Perform frequency comparison to 1000 Genomes
	 * @param args
	 */
	private static void performFreqComp(String[] args) {
		if (args.length < 3) {
			System.out.println("Enter a BED file and then several variant files you'd like to calculate frequency from");
			return;
		}
		
		BEDFile regions = new BEDFile(new File(args[1]));
		try {
			regions.buildIntervalsMap();


			System.err.println("Merging files and counting samples...");
			VariantPool pool = getPool(new File(args[2]));
			for(int i=3; i<args.length; i++) {
				VariantLineReader reader = getReader(args[i]);
				System.err.println("Merging " + args[i]);
				do {
					VariantRec var = reader.toVariantRec();
					VariantRec existing = pool.findRecordNoWarn(var.getContig(), var.getStart());
					if (existing == null) {
						pool.addRecord(var);
						pool.sortAllContigs();
					}
					else {
						Double count = existing.getProperty(VariantRec.SAMPLE_COUNT);
						if (count == null) {
							existing.addProperty(VariantRec.SAMPLE_COUNT, 1.0);
						}
						else {
							existing.addProperty(VariantRec.SAMPLE_COUNT, count+1.0);
						}
					}
				} while(reader.advanceLine());
				System.err.println("..done, pool has " + pool.size() + " variants");
				pool.sortAllContigs();
			}
			
			
			//System.err.println("Done merging...");
			
			System.out.println("Gene\tpdot\tmtscore\tppscore\tgerpscore\tsiftscore\ttkgfreq\tamrfreq\tsampleFreq");
			
			//Create DBNSFP reader....
			DBNSFPReader reader = new DBNSFPReader();
			
			for(String contig : regions.getContigs() ) {
				if (contig.length() > 2) {
					System.err.println("Skipping weird contig " + contig);
					continue;
				}
				for(Interval interval : regions.getIntervalsForContig(contig)) {
					
					int start = interval.begin;
					int end = interval.end;
					reader.advanceTo(contig, start);
					while(reader.getCurrentPos() < end) {
						Double tkgFreq = reader.getValue(DBNSFPReader.TKG_AFR);
						if (tkgFreq == null || Double.isNaN(tkgFreq))
							tkgFreq = 0.0;
						
						Double amrFreq = reader.getValue(DBNSFPReader.TKG_AMR);
						if (amrFreq == null || Double.isNaN(amrFreq))
							amrFreq = 0.0;
						
						VariantRec rec = pool.findRecordNoWarn(contig, reader.getCurrentPos());
						Double sampleCount = 0.0;
						if (rec != null) {
								sampleCount = rec.getProperty(VariantRec.SAMPLE_COUNT);
								if (sampleCount == null)
									sampleCount = 0.0;
						}
						
						double sampleFreq = sampleCount / (double)(args.length-2.0);
						
						double mtScore = reader.getValue(DBNSFPReader.MT);
						double ppScore = reader.getValue(DBNSFPReader.PP);
						double gerpScore = reader.getValue(DBNSFPReader.GERP);
						double siftScore = reader.getValue(DBNSFPReader.SIFT);
						String pDot = reader.getPDot();
						
						if (amrFreq > 0 || tkgFreq > 0.0 )
							System.out.println(reader.getText(DBNSFPReader.GENE) + "\t" + pDot + "\t" + mtScore + "\t" + ppScore + "\t" + gerpScore + "\t" + siftScore + "\t" + tkgFreq + "\t" + amrFreq + "\t" + sampleFreq);
						
						reader.advanceLine();
					}
				}
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
	}
	
	private static void performFilter(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the property to filter by and the cutoff value");
			return;
		}
		
		boolean greater = true;
		if (args[0].equals("ufilter"))
			greater = false;
		
		String prop = args[1];
		double val = Double.parseDouble(args[2]);
		try {
			for(int i=3; i<args.length; i++) {
				PrintStream outStream = System.out;
				VariantLineReader reader = getReader(args[i]);
				
//				if (args.length>4) {
//					String outputFileName = args[i].replace(".vcf", ".flt.vcf").replace(".csv", ".flt.csv");
//					outStream = new PrintStream(new FileOutputStream(outputFileName));
//				}
				
				outStream.print( reader.getHeader() );
				int count = 0;
				do {
					VariantRec var = reader.toVariantRec();
					if (var == null)
						continue;
					Double varVal;
					if (prop.startsWith("quality"))
						varVal = var.getQuality();
					else
						varVal = var.getProperty(prop);

					if (prop.startsWith("var.freq")) {
						Double totDepth = var.getProperty(VariantRec.DEPTH);
						Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
						if (totDepth != null && varDepth != null) {
							varVal = varDepth / totDepth;
						}
					}

					if (varVal == null) {
						outStream.println( reader.getCurrentLine() );
						count++;
						continue;
					}
					
					if (!greater) {
						if (varVal < val) {
							outStream.println( reader.getCurrentLine() );
							count++;
						}
					}
					else {
						if (varVal > val) {
							outStream.println( reader.getCurrentLine() );
							count++;
						}
					}

					
				} while(reader.advanceLine());
			
				if (greater)
					System.err.println("Found " + count + " vars with " + prop + " above " + val);
				else 
					System.err.println("Found " + count + " vars with " + prop + " below " + val);

				if (outStream != System.out)
					outStream.close();
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
				
//				VariantPool filteredVars = new VariantPool();
//				for(String contig : pool.getContigs()) {
//					for(VariantRec var : pool.getVariantsForContig(contig)) {
//											}
//				}
//
//				List<String> annoKeys = new ArrayList<String>();
//				annoKeys.add(VariantRec.RSNUM);
//				annoKeys.add(VariantRec.POP_FREQUENCY);
//				annoKeys.add(VariantRec.GENE_NAME);
//				annoKeys.add(VariantRec.VARIANT_TYPE);
//				annoKeys.add(VariantRec.EXON_FUNCTION);
//				annoKeys.add(VariantRec.CDOT);
//				annoKeys.add(VariantRec.PDOT);
//				annoKeys.add(VariantRec.VQSR);
//				annoKeys.add(VariantRec.DEPTH);
//				annoKeys.add(VariantRec.EFFECT_PREDICTION2);
//				//annoKeys.add(VariantRec.PUBMED_SCORE);
//				annoKeys.add(VariantRec.FALSEPOS_PROB);
//				annoKeys.add(VariantRec.TAUFP_SCORE);
//				annoKeys.add(VariantRec.FS_SCORE);
//				annoKeys.add(VariantRec.MT_SCORE);
//				annoKeys.add(VariantRec.POLYPHEN_SCORE);
//				annoKeys.add(VariantRec.SIFT_SCORE);
//				annoKeys.add(VariantRec.GERP_SCORE);
//				annoKeys.add(VariantRec.PHYLOP_SCORE);
//				
//				annoKeys.add(prop);
//				if (greater)
//					System.err.println("Found " + filteredVars.size() + " vars with " + prop + " above " + val);
//				else 
//					System.err.println("Found " + filteredVars.size() + " vars with " + prop + " below " + val);
				
//				if (args.length==4)
//					filteredVars.listAll(System.out, annoKeys);
//				else {
//					//Write results to a file
//					String outfilename = args[i].replace(".vcf", "").replace(".csv", "") + ".flt.csv";
//					PrintStream out = new PrintStream(new FileOutputStream(new File(outfilename)));
//					System.err.println("Emitting vars from file " + args[i] + " to : " + outfilename);
//					filteredVars.listAll(out, annoKeys);
//					out.close();
//				}
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	
	private static void performSample(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the number of samples to obtain from the variant file");
			return;
		}
		
		
		List<String> lines = new ArrayList<String>();
		VariantLineReader reader;
		try {
			reader = getReader( args[2] );
			
			
			do {
				VariantRec var = reader.toVariantRec();
				if (var != null)
					lines.add(reader.getCurrentLine());
			} while(reader.advanceLine());

			
			Double arg = Double.parseDouble(args[1]);
			int samples = -1;
			if (arg > 1.0) {
				samples = (int)Math.round(arg);
			} else {
				samples = (int)Math.round( (double)lines.size() * arg );
			}
			
			System.err.println("Sampling " + samples + " of " + lines.size() + " total variants");
			
			if (samples > lines.size()) {
				System.err.println("Error : requested number of samples is greater than number of variants in file");
				return;
			}
			
			
			System.out.print(reader.getHeader());
			Set<Integer> linesSampled = new HashSet<Integer>();
			int count = 0;
			while(count < samples) {
				int lineNum = (int)(lines.size() * Math.random());
				if (! linesSampled.contains(lineNum)) {
					System.out.println(lines.get(lineNum));
					linesSampled.add(lineNum);
					count++;
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void performCollate(String[] args) {
		
		BEDFile bed = new BEDFile(new File(args[1]));
		try {
			bed.buildIntervalsMap();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		List<VariantPool> pools = new ArrayList<VariantPool>();
		for(int i=2; i<args.length; i++) {
			try {
				pools.add( getPool(new File(args[i])) );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.print("Variant");
		for(int i=2; i<args.length; i++) {
			System.out.print("\t" + args[i]);
		}
		System.out.println();
		
		for(String contig : bed.getContigs()) {
			for(Interval interval : bed.getIntervalsForContig(contig)) {
				System.out.print(contig + ":" + interval.begin + "\t");
				for(VariantPool pool : pools) {
					VariantRec var = pool.findRecordNoWarn(contig, interval.end);
					if (var != null) {
						String hetStr = "het";
						if (! var.isHetero())
							hetStr = "hom";
						System.out.print(hetStr + ";" + var.getQuality() +";" + var.getProperty("depth") + "\t");
					}
					else {
						System.out.print("-\t");
					}
				}
				System.out.println();
			}
		}
		
		
	}
	
	private static void performEmit(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the property to filter by and the cutoff value");
			return;
		}
		String prop = args[1];
		
		List<String> annoKeys = new ArrayList<String>();
		annoKeys.add(VariantRec.RSNUM);
		annoKeys.add(VariantRec.POP_FREQUENCY);
		annoKeys.add(VariantRec.GENE_NAME);
		annoKeys.add(VariantRec.VARIANT_TYPE);
		annoKeys.add(VariantRec.EXON_FUNCTION);
		annoKeys.add(VariantRec.CDOT);
		annoKeys.add(VariantRec.PDOT);
		annoKeys.add(VariantRec.VQSR);
		annoKeys.add(VariantRec.DEPTH);
		annoKeys.add(VariantRec.EFFECT_PREDICTION2);
		//annoKeys.add(VariantRec.PUBMED_SCORE);
		annoKeys.add(VariantRec.FALSEPOS_PROB);
		annoKeys.add(VariantRec.TAUFP_SCORE);
		annoKeys.add(VariantRec.FS_SCORE);
		annoKeys.add(VariantRec.MT_SCORE);
		annoKeys.add(VariantRec.POLYPHEN_SCORE);
		annoKeys.add(VariantRec.SIFT_SCORE);
		annoKeys.add(VariantRec.GERP_SCORE);
		annoKeys.add(VariantRec.PHYLOP_SCORE);
		annoKeys.add(prop);
		
		boolean greater = true;
		
		System.out.print("source" + "\t" + VariantRec.getSimpleHeader());
		for(String key : annoKeys) {
			System.out.print("\t" + key);
		}
		System.out.println();
		
		
		
		double val = Double.parseDouble(args[2]);
		try {
			for(int i=3; i<args.length; i++) {
				VariantPool pool = getPool(new File(args[i]));
				int count = 0;
				//VariantPool filteredVars = new VariantPool();
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						Double varVal;
						if (prop.startsWith("quality"))
							varVal = var.getQuality();
						else
							varVal = var.getProperty(prop);

						if (prop.startsWith("var.freq")) {
							Double totDepth = var.getProperty(VariantRec.DEPTH);
							Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
							if (totDepth != null && varDepth != null) {
								varVal = varDepth / totDepth;
							}
						}

						if (varVal == null) {
							System.out.println(var.getAnnotation(VariantRec.SOURCE) + "\t" + var.toSimpleString() + "\t" + var.getPropertyString(annoKeys));
							count++;
						}
						else {
							if ( (!greater) && varVal < val) {
								System.out.println(var.getAnnotation(VariantRec.SOURCE) + "\t" + var.toSimpleString() + "\t" + var.getPropertyString(annoKeys));
								count++;
							}
							if (greater && varVal > val) {
								System.out.println(var.getAnnotation(VariantRec.SOURCE) + "\t" + var.toSimpleString() + "\t" + var.getPropertyString(annoKeys));
								count++;
							}
						}
					}
				}
				
				System.err.println(args[i] + " total hits: " + count);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static final String[] refOrder = new String[]{"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","X"
		,"Y","MT","GL000207.1","GL000226.1","GL000229.1","GL000231.1","GL000210.1","GL000239.1","GL000235.1","GL000201.1","GL000247.1","GL000245.1","GL000197.1","GL000203.1"
		,"GL000246.1","GL000249.1","GL000196.1","GL000248.1","GL000244.1","GL000238.1","GL000202.1","GL000234.1","GL000232.1","GL000206.1","GL000240.1","GL000236.1","GL000241.1","GL000243.1","GL000242.1","GL000230.1","GL000237.1"
		,"GL000233.1","GL000204.1","GL000198.1","GL000208.1","GL000191.1","GL000227.1","GL000228.1","GL000214.1","GL000221.1","GL000209.1","GL000218.1","GL000220.1"
		,"GL000213.1","GL000211.1","GL000199.1","GL000217.1","GL000216.1","GL000215.1","GL000205.1","GL000219.1","GL000224.1","GL000223.1","GL000195.1"
		,"GL000212.1","GL000222.1","GL000200.1","GL000193.1","GL000194.1","GL000225.1","GL000192.1" };
}



