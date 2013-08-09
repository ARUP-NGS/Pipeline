package simulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.VCFLineParser;
import buffer.VCFFile;
import cern.jet.random.Exponential;
import cern.jet.random.Poisson;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

/**
 * Sprinkle some mutations on a sequence
 * @author brendan
 *
 */
public class MutateSequence {
	
	static RandomEngine rng = new MersenneTwister( (int)System.currentTimeMillis() );
	static Poisson poisGen; 
	static Exponential expGen;
	
	//Base frequencies from which new mutations are drawn, this is a cumulative distro
	//Order is A, G, T (all others are C)
	static final double[] baseFreqs = new double[]{0.3, 0.55, 0.78};
	
	//Probability that new mutation is a transition, ttRatio is x/(1-x) 
	static final double transitionProb = 0.72; //0.72 makes ttRatio about 2.57
	
	public static char pickChar(char curChar) {
		curChar = Character.toUpperCase(curChar);
		if (curChar == 'N')
			return 'N';
		boolean transition = rng.nextDouble() < transitionProb;
		
		if (curChar == 'A') {
			if (transition)
				return 'G';
			else {
				if (rng.nextDouble() < 0.5)
					return 'C';
				else
					return 'G';
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
	 * Takes mutations found in the given file and applies them to the this sequence
	 * @param seq
	 * @param variants
	 * @throws IOException 
	 */
	public static List<MutRec> applyMutantsFromFile(String contig, StringBuilder seq, VCFFile variants, boolean firstStrand) throws IOException {
		VCFLineParser reader = new VCFLineParser(variants);
		List<MutRec> muts = new ArrayList<MutRec>();
		int count = 0;
		
		int n = 0;
		while(seq.charAt(n) == 'N') {
			n++;
		}
		System.out.println("Sequence starts with " + n + "  n's");
		
		int OFFSET =0;
		
		do {
			if (! contig.equals( reader.getContig() )) 
				continue;
			
			Integer pos = reader.getStart();
			if (pos < OFFSET)
				continue;
			if (reader.getRef().length()==1 && reader.getAlt().length()==1 && (!reader.getAlt().equals("."))) {
				//If we doing strand 1, only write if first is alt
				if (firstStrand && reader.firstIsAlt()) {
					if (seq.length()>(pos+2)) {
						MutRec rec = new MutRec();
						rec.ref = "" + seq.charAt(pos-1-OFFSET); // 
						
						if (! reader.getRef().equalsIgnoreCase(rec.ref)) {
							System.err.println("Uh-oh, reference from VCF does not match reference sequence at pos " + pos);
							System.err.println("VCF ref: " + reader.getRef() + " actual seq:" + rec.ref + " pos on seq:" + (pos+1-OFFSET) );
						}
						
						rec.alt = reader.getAlt();
						rec.pos = pos;
						if (count % 100 == 0)
							System.out.println("Applied: " + count + " mutations,... mutating " + pos + " from " + rec.ref + " to " + rec.alt);
						count++;

						seq.replace(pos-1, pos, reader.getAlt());
						muts.add(rec);
					}
					else { 
						System.err.println("Warning, file includes variant at site " + (pos+1) + " but seq is only " + seq.length() + " bases long");
						break;
					}
					
				}
				
				//If we're doing strand two, only write if second is alt
				if ((!firstStrand) && (reader.secondIsAlt())) {
					if (seq.length()>(pos+2)) {
						MutRec rec = new MutRec();
						rec.ref = "" + seq.charAt(pos-1-OFFSET); // 
						rec.alt = reader.getAlt();
						rec.pos = pos;
						
						if (! reader.getRef().equalsIgnoreCase(rec.ref)) {
							System.err.println("Uh-oh, reference from VCF does not match reference sequence at pos " + pos);
							System.err.println("VCF ref: " + reader.getRef() + " actual seq:" + rec.ref + " pos on seq:" + (pos+1-OFFSET) );
						}
						
						if (count % 100 == 0)
							System.out.println("Applied: " + count + " mutations,... mutating " + pos + " from " + rec.ref + " to " + rec.alt);
						count++;
						
						seq.replace(pos-1, pos, reader.getAlt());
						muts.add(rec);
					}
					else {
						System.err.println("Warning, file includes variant at site " + (pos+1) + " but seq is only " + seq.length() + " bases long");
					}
				}

			}
			
		} while(reader.advanceLine());
		
		return muts;
	}
	
	public static StringBuilder addSNPs(StringBuilder seq, int numSNPs) {
		int totalSites = seq.length();
		for(int i=0; i<numSNPs; i++) {
			int site = (int)Math.round( (double)totalSites * Math.random() );
			char curBase = seq.charAt(site);
			while (curBase ==  '\n') {
				site = (int)Math.round( (double)totalSites * Math.random() );
				curBase = seq.charAt(site);
			}
			char newBase = pickChar(curBase);
			//System.err.println(site + "\t" + seq.charAt(site) + "\t" + newBase);
			seq.replace(site, site+1, "" + newBase);
			if (i%1000==0)
				System.out.println("Mutating number " + i);
		}
		
		return seq;
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
	
	public static void addIndels(StringBuilder seq, int numInserts, int numDels, double meanSize) {
		
		//Deletions...
		for(int i=0; i<numDels; i++) {
			int size = genGeo(meanSize);
			int start = (int)( Math.random() * (seq.length()-size-1) );
			seq.replace(start, start+size, "");
		}
		
		//Insertions
		for(int i=0; i<numDels; i++) {
			int size = genGeo(meanSize);
			int start = (int)( Math.random() * (seq.length()-size-1) );
			String insertion = randomSeq(size);
			seq.replace(start, start+size, insertion);
		}
		
	}
	
	public static List<MutRec> generateIndels(StringBuilder ref, double indelRate, double indelMeanSize) {
		List<MutRec> indels = new ArrayList<MutRec>(2048);
		int site = 0;
		expGen = new Exponential(indelRate, rng);
		site += expGen.nextInt();
		
		while (site < ref.length()) {
			MutRec rec = new MutRec();
			
			boolean isInsertion = Math.random() < 0.50;
			int size = (int)Math.ceil( expGen.nextDouble(1.0/indelMeanSize) );
			if (isInsertion) {
				rec.pos = site;
				rec.ref = "-";
				String insert = randomSeq(size);
				rec.alt = insert;
			}
			else {
				rec.pos = site;
				int start = rec.pos-1;
				int end = rec.pos-1+size;
				rec.ref = ref.substring(start, end);
				rec.alt = "-";
			}
			

			indels.add(rec);
			//System.out.println(rec);
			site += size+expGen.nextInt();
		}
		
		return indels;
	}
	
	public static void applyIndels(StringBuilder ref, List<MutRec> indels) {
		//Important that we traverse in reverse order (from bottom backward) so 
		//we don't mess up indexing as we add / remove bits
		int prevpos = (int)(1e12); 
		for(int i=indels.size()-1; i>=0; i--) {
			MutRec rec = indels.get(i);
			if (rec.pos >= prevpos) {
				throw new IllegalArgumentException("Indels aren't in sorted order");
			}
			prevpos = rec.pos;
			if (rec.ref == "-") { 
				//indel is an insertion
				ref.replace(rec.pos-1, rec.pos-1, rec.alt);
			}
			else {
				//indel is a deletion
				int start = rec.pos-1;
				int end = rec.pos-1+rec.ref.length();
				String compRef = ref.substring(start, end);
				if (! compRef.equals(rec.ref)) {
					throw new IllegalArgumentException("reference sequences have changed! old ref:" + rec.ref + " new ref: " + compRef);
				}
				ref.replace(start, end, "");
			}
		}
		
	}
	
	
	
	public static void writeContig(String contig, StringBuilder seq, BufferedWriter writer) throws IOException {
		writer.write(">" + contig + "\n");
		int start = 0;
		int step = 80;
		int end = 0;
		while(end < seq.length()) {
			end = start + step;
			writer.write(seq.substring(start, Math.min(seq.length()-1, end)) + "\n");
			start = end;
		}
	}
	
	/**
	 * Generate a list of sites to be mutated, with reference and alt allele
	 * These snps can then be applied to the reference contig to generate the
	 * real mutated sequence.  
	 * @param ref
	 * @param mutRate
	 * @return
	 */
	public static List<MutRec> generateSNPs(StringBuilder ref, Double mutRate) {
		List<MutRec> snps = new ArrayList<MutRec>(2048);
		int site = 0;
		expGen = new Exponential(mutRate, rng);
		site += expGen.nextInt();
		
		while (site < ref.length()) {
			MutRec rec = new MutRec();
			char r = Character.toUpperCase(ref.charAt(site-1));
			rec.ref = String.valueOf(r);
			char newBase = 'X';
			if (r == 'N') 
				newBase = 'N';
			else
				newBase = pickChar(r);
			rec.alt = String.valueOf(newBase);
			rec.pos = site;
			snps.add(rec);
			//System.out.println(rec);
			site += expGen.nextInt();
		}
		
		int actualSnpCount = snps.size();
		int sites = ref.length();
		System.out.println("Actual snp number: " + actualSnpCount + " actual mut rate: " + (double)actualSnpCount / (double)ref.length());
		return snps;
	}
	
	public static void applySNPs(StringBuilder ref, List<MutRec> snps) {
		int lengthBefore = ref.length();
		int count = 0;
		for(MutRec rec : snps) {
			ref.replace(rec.pos-1, rec.pos, rec.alt);
			count++;
			if (count % 1000 == 0){
				System.out.println("On base " + count + " out of " + snps.size());
			}
		}
		int lengthAfter = ref.length();
		if (lengthBefore != lengthAfter) {
			System.out.println("Uh-oh, length before not the same as length after...");
		}
	}
	

	public static void mutateContig(String key, 
			StringBuilder contSeq,
			Double mutRate, 
			Double indelRate, 
			Double indelMeanSize) {

		int mutNum = poisGen.nextInt( mutRate * contSeq.length() );
		int numInserts = poisGen.nextInt( indelRate*contSeq.length() );
		int numDels = poisGen.nextInt( indelRate*contSeq.length() );
		System.out.println("Contig " + key + " gets " + mutNum + " SNPs, " + numInserts + " insertions and " + numDels + " deletions");

		addSNPs(contSeq, mutNum);
		System.out.println("done adding snps, now adding indels");
		
		addIndels(contSeq, numInserts, numDels, indelMeanSize);
		System.out.println("done mutating, now writing...");
		
	}
	
	public static void usage() {
		System.out.println("MutateSequence : mutate a given sequence in fasta format ");
		System.out.println(" mutateSeq.jar [reference.fasta] [mut. rate. (float)] [indel rate (float)] [mean indel size (float)] [outputfilename]");
	}
	
	public static void main(String[] args) {
		
		
		if (args.length != 5 && args.length != 6) {
			usage();
			return;
		}
		
		File inputFile = new File( args[0]);
		Double mutRate = Double.parseDouble(args[1]);
		Double indelRate = Double.parseDouble(args[2]);
		Double indelMeanSize = Double.parseDouble(args[3]);
		File outputFile = new File(args[4]);
		
		File vcfMuts = null;
		if (args.length==6)
			vcfMuts = new File(args[5]);

		
		try {
			//Read the WHOLE REFERENCE INTO MEMORY! 
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			String line = reader.readLine();
			while (line != null && line.length()==0) {
				line = reader.readLine();
			}

			Map<String, StringBuilder> contigs = new HashMap<String, StringBuilder>();
			StringBuilder seq = new StringBuilder();
			String contig = null;
			
			contig = line.replace(">", "").trim();
			line = reader.readLine();
			
			
			while (line != null) {
				seq.append(line.trim());
				line = reader.readLine();
				if (line != null && line.contains(">")) {
					contigs.put(contig, seq);
					System.out.println("Found contig: " + contig + " of length: " + seq.length());
					contig = line.replace(">", "").trim(); //New contig
					
					seq = new StringBuilder();
					line = reader.readLine();
				}
			}
		
			//don't forget to add the last contig
			contigs.put(contig, seq);
			System.out.println("Found contig: " + contig + " of length: " + seq.length());

			String trueMutsFilename = outputFile + "-truemuts.vcf";
			
			BufferedWriter trueMutsWriter = new BufferedWriter(new FileWriter(trueMutsFilename));
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			for(String key : contigs.keySet()) {
				StringBuilder contSeq = contigs.get(key);
				System.err.println("Contig " + key + " length: " + contSeq.length());

				List<MutRec> mutants = new ArrayList<MutRec>(1024);
				if (vcfMuts != null) {
					System.err.println("Applying mutants from file : " + vcfMuts.getName());
					VCFFile mutFile = new VCFFile(vcfMuts);
					List<MutRec> fileMuts = applyMutantsFromFile(key, contSeq, mutFile, false);
					mutants.addAll(fileMuts);
					System.err.println("Added " + fileMuts.size() + " variants from file");
				}
				
				if (mutRate > 0) {
					System.err.println("Generating snps..");
					List<MutRec> snps = generateSNPs(contSeq, mutRate);
					System.err.println("Applying " + snps.size() + " snps..");
					applySNPs(contSeq, snps);
					mutants.addAll(snps);
				}
				
				if (indelRate > 0) {
					System.err.println("Generating indels..");
					List<MutRec> indels = generateIndels(contSeq, indelRate, indelMeanSize);
					System.err.println("Applying " + indels.size() + " indels ");
					applyIndels(contSeq, indels);
					mutants.addAll(indels);
				}
				
				Collections.sort(mutants);
				
				System.err.println("Final contig length : " + contSeq.length());
				for(MutRec snp : mutants) {
					//System.out.println(key + "\t" + snp);
					trueMutsWriter.write(key + "\t" + snp + "\n");
				}
				
				
			}
			
			for(String key : contigs.keySet()) {
				writeContig(key, contigs.get(key), writer);
			}
			writer.close();
			trueMutsWriter.close();
			
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
			return pos + "\t" + ref + "\t" + alt;
		}

		@Override
		public int compareTo(MutRec rec) {
			if (pos < rec.pos)
				return -1;
			else
				return 1;
		}
	}
	
//	class ContigMutator extends SwingWorker {
//
//		String contig;
//		StringBuilder seq;
//		Double mutRate;
//		Double indelRate;
//		Double indelMeanSize;
//		
//		public ContigMutator(String contig, StringBuilder seq, double mutRate, double indelRate, double indelSize) {
//			this.contig = contig;
//			this.seq = seq;
//			this.mutRate = mutRate;
//			this.indelRate = indelRate;
//			this.indelMeanSize = indelSize;
//		}
//		
//		@Override
//		protected Object doInBackground() throws Exception {
//			mutateContig(contig, seq, mutRate, indelRate, indelMeanSize);
//			return null;
//		}
//		
//	}

}

