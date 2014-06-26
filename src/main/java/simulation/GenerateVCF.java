package simulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cern.jet.random.Exponential;
import cern.jet.random.Poisson;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

/**
 * Some static utilities to generate a vcf with snps and indels, with a specified "mutation rate" (not really a
 * mutation rate) and ttRatio for the snps, and separate 'mutation rate' and size for the indels.  
 * 
 * @author brendan
 *
 */
public class GenerateVCF {

	static RandomEngine rng = new MersenneTwister( (int)System.currentTimeMillis() );
	static Poisson poisGen; 
	static Exponential expGen;
	
	//Base frequencies from which new mutations are drawn, this is a cumulative distro
	//Order is A, G, T (all others are C)
	static final double[] baseFreqs = new double[]{0.3, 0.55, 0.78};
	
	//Probability that new mutation is a transition, ttRatio is x/(1-x) 
	//static final double transitionProb = 0.72; //0.72 makes ttRatio about 2.57
	
	public static char pickChar(char curChar, double ttRatio) {
		curChar = Character.toUpperCase(curChar);
		if (curChar == 'N')
			return 'N';
		
		double transitionProb = ttRatio / (1.0 + ttRatio);
		
		boolean transition = rng.nextDouble() < transitionProb;
		
		if (curChar == 'A') {
			if (transition)
				return 'G';
			else {
				if (rng.nextDouble() < 0.5)
					return 'C';
				else
					return 'T';
			}
		}
		if (curChar == 'G') {
			if (transition)
				return 'A';
			else {
				if (rng.nextDouble() < 0.5)
					return 'C';
				else
					return 'T';
			}
		}
		if (curChar == 'C') {
			if (transition)
				return 'T';
			else {
				if (rng.nextDouble() < 0.5)
					return 'A';
				else
					return 'G';
			}
		}
		if (curChar == 'T') {
			if (transition)
				return 'C';
			else {
				if (rng.nextDouble() < 0.5)
					return 'A';
				else
					return 'G';
			}
		}
		
		//we should never get here
		throw new IllegalArgumentException("Unknown base found : " + curChar);
	}

	
	
	
	/**
	 * Given a uniform random variate u, and a distribution mean 'mean', return an exponentially-distributed
	 * variate
	 * @param u
	 * @param mean
	 * @return
	 */
	public static double convExp(double u, double mean) {
		return -Math.log(u)*mean;
	}
	
	/**
	 * Generate a geometrically distributed variate with mean mean by creating an exponential variate 
	 * and rounding it. Presumably this could give size of 0?  
	 * @param mean
	 * @return
	 */
	public static int genGeo(double mean) {
		return (int)Math.round( convExp( Math.random(), mean) );
	}
	
	public static String randomSeq(int size) {
		StringBuilder buf = new StringBuilder();
		for(int i=0; i<size; i++) {
			char c;
			double r = Math.random();
			if (r< 0.25)
				c = 'A';
			else if (r<0.5)
				c = 'G';
			else if (r< 0.75)
				c = 'T';
			else 
				c = 'C';
			buf.append(c);
		}
		
		return buf.toString();
	}
	
	
	public static List<MutRec> generateIndels(StringBuilder ref, double indelRate, double indelMeanSize) {
		List<MutRec> indels = new ArrayList<MutRec>(2048);
		int site = 2; //Starting at two prevents errors when first site is 0
		expGen = new Exponential(indelRate, rng);
		site += expGen.nextInt();
		
		while (site < ref.length()) {
			MutRec rec = new MutRec();
			
			boolean isInsertion = Math.random() < 0.50;
			int size = (int)Math.ceil( expGen.nextDouble(1.0/indelMeanSize) );
			if (isInsertion) {
				rec.pos = site;
				char refChar = Character.toUpperCase(ref.charAt(site-1));
				rec.ref = "" + refChar;
				String insert = randomSeq(size);
				rec.alt = refChar + insert;
			}
			else {
				rec.pos = site;
				int start = rec.pos-2;
				int end = rec.pos-1+size;
				rec.ref = ref.substring(start, end);
				rec.alt = "" + rec.ref.charAt(0);
			}
			
			//N's in indels tend break FastaAlternateReferenceMaker
			if (! rec.ref.contains("N"))
				indels.add(rec);
			//System.out.println(rec);
			site += size+expGen.nextInt();
		}
		
		return indels;
	}
	
	public static boolean isTransition(char ref, char alt) {
		return (ref == 'A' && alt == 'G' 
				|| ref == 'G' && alt == 'A'
				|| ref == 'T' && alt == 'C'
				|| ref == 'C' && alt == 'T');
				
	}
	
	/**
	 * Generate a list of sites to be mutated, with reference and alt allele
	 * These snps can then be applied to the reference contig to generate the
	 * real mutated sequence.  
	 * @param ref
	 * @param mutRate
	 * @return
	 */
	public static List<MutRec> generateSNPs(StringBuilder ref, Double mutRate, Double ttRatio) {
		List<MutRec> snps = new ArrayList<MutRec>(2048);
		int site = 0;
		expGen = new Exponential(mutRate, rng);
		site += expGen.nextInt();
		
		int transitionCount = 0;
		int transversionCount = 0;
		
		while (site < ref.length()) {
			MutRec rec = new MutRec();
			char r = Character.toUpperCase(ref.charAt(site-1));
			rec.ref = String.valueOf(r);
			char newBase = 'X';
			if (r != 'N') {
				newBase = pickChar(r, ttRatio);
				if ( isTransition(r, newBase)) {
					transitionCount++;
				}
				else {
					transversionCount++;
				}

				rec.alt = String.valueOf(newBase);
				rec.pos = site;
				snps.add(rec);
			}
			//System.out.println(rec);
			site += expGen.nextInt();
		}
		
		int actualSnpCount = snps.size();
		int sites = ref.length();
		double actualTTRatio = (double)transitionCount / (double)transversionCount;
		System.err.println("Actual snp number: " + actualSnpCount + " actual mut rate: " + (double)actualSnpCount / (double)ref.length() + " actual ttRatio: " + actualTTRatio);
		return snps;
		
	}
	

	
	public static void usage() {
		System.out.println("simVCF : Generate a simulated vcf of variants suitable for applying to a reference file given sequence in fasta format ");
		System.out.println(" simVCF.jar [reference.fasta] [snp. mut. rate. (float)] [snp Ti / Tv ratio] [indel rate (float)] [mean indel size (float)]");
	}
	
	public static void main(String[] args) {
		
		if (args.length != 5 && args.length != 6) {
			usage();
			return;
		}
		
		File inputFile = new File( args[0]);
		Double mutRate = Double.parseDouble(args[1]);
		Double ttRatio = Double.parseDouble(args[2]);
		Double indelRate = Double.parseDouble(args[3]);
		Double indelMeanSize = Double.parseDouble(args[4]);
		
		
	
		try {
			//Read the WHOLE REFERENCE INTO MEMORY! 
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			String line = reader.readLine();
			while (line != null && line.length()==0) {
				line = reader.readLine();
			}

			List<String> contigs = new ArrayList<String>(16);
			List<StringBuilder> seqs = new ArrayList<StringBuilder>(16);
			StringBuilder seq = new StringBuilder();
			String contig = null;
			
			contig = line.replace(">", "").trim();
			int index = contig.indexOf(" ");
			if (index > -1) {
				contig = contig.substring(0, index);
			}
			
			
			line = reader.readLine();
			
			
			System.out.println(vcfHeader);
			
			while (line != null) {
				seq.append(line.trim());
				line = reader.readLine();
				if (line != null && line.contains(">")) {
					contigs.add(contig);
					seqs.add(seq);
					
					System.err.println("Found contig: " + contig + " of length: " + seq.length());
					contig = line.replace(">", "").trim(); //New contig
					index = contig.indexOf(" ");
					if (index > -1) {
						contig = contig.substring(0, index);
					}
					
					seq = new StringBuilder();
					line = reader.readLine();
				}
			}
		
			//don't forget to add the last contig
			contigs.add(contig);
			seqs.add(seq);
			System.err.println("Found contig: " + contig + " of length: " + seq.length());

			
			for(int i=0; i<contigs.size(); i++) {
				String key = contigs.get(i);
				StringBuilder contSeq = seqs.get(i);
				System.err.println("Contig " + key + " length: " + contSeq.length());

				List<MutRec> mutants = new ArrayList<MutRec>(1024);
			
				
				if (mutRate > 0) {
					System.err.println("Generating snps..");
					List<MutRec> snps = generateSNPs(contSeq, mutRate, ttRatio);
					mutants.addAll(snps);
				}
				
				if (indelRate > 0) {
					System.err.println("Generating indels..");
					List<MutRec> indels = generateIndels(contSeq, indelRate, indelMeanSize);
					mutants.addAll(indels);
				}
				
				Collections.sort(mutants);
				
				System.err.println("Final contig length : " + contSeq.length());
				for(MutRec snp : mutants) {
					//System.out.println(key + "\t" + snp);
					System.out.println(key + "\t" + snp);
				}
				
				
			}
		
			reader.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public static class MutRec implements Comparable<MutRec> {
		int pos; 
		String ref;
		String alt;
		
		/**
		 * Emits position in vcf-like, 1-indexed format
		 */
		public String toString() {
			return pos + "\t" + "-" + "\t"+ ref + "\t" + alt + "\t100.0\tPASS\t-\t-\t-";
		}

		@Override
		public int compareTo(MutRec rec) {
			if (pos < rec.pos)
				return -1;
			else
				return 1;
		}
	}

	static final String vcfHeader = "##fileformat=VCFv4.1\n"
			+ "##contig=<ID=1,length=249250621,assembly=b37>\n" 
+ "##contig=<ID=10,length=135534747,assembly=b37>\n" 
+ "##contig=<ID=11,length=135006516,assembly=b37>\n" 
+ "##contig=<ID=12,length=133851895,assembly=b37>\n"  
+ "##contig=<ID=13,length=115169878,assembly=b37>\n"
+ "##contig=<ID=14,length=107349540,assembly=b37>\n"
 + "##contig=<ID=15,length=102531392,assembly=b37>\n"
 + "##contig=<ID=16,length=90354753,assembly=b37>\n"
 + "##contig=<ID=17,length=81195210,assembly=b37>\n"
 + "##contig=<ID=18,length=78077248,assembly=b37>\n"
 + "##contig=<ID=19,length=59128983,assembly=b37>\n"
 + "##contig=<ID=2,length=243199373,assembly=b37>\n"
 + "##contig=<ID=20,length=63025520,assembly=b37>\n"
 + "##contig=<ID=21,length=48129895,assembly=b37>\n"
 + "##contig=<ID=22,length=51304566,assembly=b37>\n"
 + "##contig=<ID=3,length=198022430,assembly=b37>\n"
 + "##contig=<ID=4,length=191154276,assembly=b37>\n"
 + "##contig=<ID=5,length=180915260,assembly=b37>\n"
 + "##contig=<ID=6,length=171115067,assembly=b37>\n"
 + "##contig=<ID=7,length=159138663,assembly=b37>\n"
 + "##contig=<ID=8,length=146364022,assembly=b37>\n"
 + "##contig=<ID=9,length=141213431,assembly=b37>\n"
 + "##contig=<ID=GL000191.1,length=106433,assembly=b37>\n"
 + "##contig=<ID=GL000192.1,length=547496,assembly=b37>\n"
 + "##contig=<ID=GL000193.1,length=189789,assembly=b37>\n"
 + "##contig=<ID=GL000194.1,length=191469,assembly=b37>\n"
 + "##contig=<ID=GL000195.1,length=182896,assembly=b37>\n"
 + "##contig=<ID=GL000196.1,length=38914,assembly=b37>\n"
 + "##contig=<ID=GL000197.1,length=37175,assembly=b37>\n"
 + "##contig=<ID=GL000198.1,length=90085,assembly=b37>\n"
 + "##contig=<ID=GL000199.1,length=169874,assembly=b37>\n"
 + "##contig=<ID=GL000200.1,length=187035,assembly=b37>\n"
 + "##contig=<ID=GL000201.1,length=36148,assembly=b37>\n"
 + "##contig=<ID=GL000202.1,length=40103,assembly=b37>\n"
 + "##contig=<ID=GL000203.1,length=37498,assembly=b37>\n"
 + "##contig=<ID=GL000204.1,length=81310,assembly=b37>\n"
 + "##contig=<ID=GL000205.1,length=174588,assembly=b37>\n"
 + "##contig=<ID=GL000206.1,length=41001,assembly=b37>\n"
 + "##contig=<ID=GL000207.1,length=4262,assembly=b37>\n"
 + "##contig=<ID=GL000208.1,length=92689,assembly=b37>\n"
 + "##contig=<ID=GL000209.1,length=159169,assembly=b37>\n"
 + "##contig=<ID=GL000210.1,length=27682,assembly=b37>\n"
 + "##contig=<ID=GL000211.1,length=166566,assembly=b37>\n"
 + "##contig=<ID=GL000212.1,length=186858,assembly=b37>\n"
 + "##contig=<ID=GL000213.1,length=164239,assembly=b37>\n"
 + "##contig=<ID=GL000214.1,length=137718,assembly=b37>\n"
 + "##contig=<ID=GL000215.1,length=172545,assembly=b37>\n"
 + "##contig=<ID=GL000216.1,length=172294,assembly=b37>\n"
 + "##contig=<ID=GL000217.1,length=172149,assembly=b37>\n"
 + "##contig=<ID=GL000218.1,length=161147,assembly=b37>\n"
 + "##contig=<ID=GL000219.1,length=179198,assembly=b37>\n"
 + "##contig=<ID=GL000220.1,length=161802,assembly=b37>\n"
 + "##contig=<ID=GL000221.1,length=155397,assembly=b37>\n"
 + "##contig=<ID=GL000222.1,length=186861,assembly=b37>\n"
 + "##contig=<ID=GL000223.1,length=180455,assembly=b37>\n"
 + "##contig=<ID=GL000224.1,length=179693,assembly=b37>\n"
 + "##contig=<ID=GL000225.1,length=211173,assembly=b37>\n"
 + "##contig=<ID=GL000226.1,length=15008,assembly=b37>\n"
 + "##contig=<ID=GL000227.1,length=128374,assembly=b37>\n"
 + "##contig=<ID=GL000228.1,length=129120,assembly=b37>\n"
 + "##contig=<ID=GL000229.1,length=19913,assembly=b37>\n"
 + "##contig=<ID=GL000230.1,length=43691,assembly=b37>\n"
 + "##contig=<ID=GL000231.1,length=27386,assembly=b37>\n"
 + "##contig=<ID=GL000232.1,length=40652,assembly=b37>\n"
 + "##contig=<ID=GL000233.1,length=45941,assembly=b37>\n"
 + "##contig=<ID=GL000234.1,length=40531,assembly=b37>\n"
 + "##contig=<ID=GL000235.1,length=34474,assembly=b37>\n"
 + "##contig=<ID=GL000236.1,length=41934,assembly=b37>\n"
 + "##contig=<ID=GL000237.1,length=45867,assembly=b37>\n"
 + "##contig=<ID=GL000238.1,length=39939,assembly=b37>\n"
 + "##contig=<ID=GL000239.1,length=33824,assembly=b37>\n"
 + "##contig=<ID=GL000240.1,length=41933,assembly=b37>\n"
 + "##contig=<ID=GL000241.1,length=42152,assembly=b37>\n"
 + "##contig=<ID=GL000242.1,length=43523,assembly=b37>\n"
 + "##contig=<ID=GL000243.1,length=43341,assembly=b37>\n"
 + "##contig=<ID=GL000244.1,length=39929,assembly=b37>\n"
 + "##contig=<ID=GL000245.1,length=36651,assembly=b37>\n"
 + "##contig=<ID=GL000246.1,length=38154,assembly=b37>\n"
 + "##contig=<ID=GL000247.1,length=36422,assembly=b37>\n"
 + "##contig=<ID=GL000248.1,length=39786,assembly=b37>\n"
 + "##contig=<ID=GL000249.1,length=38502,assembly=b37>\n"
 + "##contig=<ID=MT,length=16569,assembly=b37>\n"
 + "##contig=<ID=X,length=155270560,assembly=b37>\n"
 + "##contig=<ID=Y,length=59373566,assembly=b37>\n"
 + "##reference=file:///uufs/chpc.utah.edu/common/home/u0379426/resources/human_g1k_v37.fasta\n"
 + "#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	sample";
	
}
