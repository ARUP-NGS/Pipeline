package fpClassifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import buffer.CSVFile;
import buffer.variant.VariantFilter;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import cern.jet.random.Binomial;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import math.ContinuousDistribution;
import math.Histogram;
import math.UnitDistribution;

public class Classifier {

	ContinuousDistribution distOne;
	ContinuousDistribution distTwo;
	
	FreqStore freqStore;
	
	public Classifier(File dataFile, ContinuousDistribution distOne, ContinuousDistribution distTwo) throws IOException {
		this.distOne = distOne;
		this.distTwo = distTwo;
		freqStore = new FreqStore();
		freqStore.readFromFile(dataFile);
	}
	
	public double getDistOneLikelihood(double[] vals) {
		//double logL = 0.0;
		double prob = 1.0;
		for(int i=0; i<vals.length; i++) {
			prob *= distOne.getPDF(vals[i]);
			
		}
		return prob;
	}
	
	public double getDistTwoLikelihood(double[] vals) {
		//double logL = 0.0;
		double prob = 1.0;
		for(int i=0; i<vals.length; i++) {
			prob *= distTwo.getPDF(vals[i]);
		}
		return prob;
	}
	
	public double getProbOneAtPos(String contig, int pos) {
		double[] freqs = freqStore.getFreqs(contig, pos);
		return getDistOneLikelihood(freqs);
	}
	
	public double getProbTwoAtPos(String contig, int pos) {
		double[] freqs = freqStore.getFreqs(contig, pos);
		return getDistTwoLikelihood(freqs);
	}
	
//	public double getRatioForVariant(VariantRec var) {
//		if (freqStore.hasEntry(var.getContig(), var.getStart())) {
//			double probNovel = getProbOneAtPos(var.getContig(), var.getStart());
//			double probKnown = getProbTwoAtPos(var.getContig(), var.getStart());
//			
//			double varFreq = var.getProperty(VariantRec.VAR_DEPTH) / var.getProperty(VariantRec.DEPTH);
//			probNovel += Math.log( distOne.getPDF( varFreq) );
//			probKnown += Math.log( distTwo.getPDF( varFreq) );
//			
//			return probNovel - probKnown;
//		}
//		else {
//			return 1.0;
//		}
//	}
	
	public double getFalsePosPosterior(VariantRec var) {
		final double q = 0.5; //Prior probability that variant is REALLY A VARIANT
		
		double probNovel = 1.0;
		double probKnown = 1.0;
		if (freqStore.hasEntry(var.getContig(), var.getStart())) {
			probNovel = getProbOneAtPos(var.getContig(), var.getStart());
			probKnown = getProbTwoAtPos(var.getContig(), var.getStart());
		}

		double varFreq = var.getProperty(VariantRec.VAR_DEPTH) / var.getProperty(VariantRec.DEPTH);
		probNovel *=  distOne.getPDF( varFreq);
		probKnown *=  distTwo.getPDF( varFreq);

		return probNovel*(1.0-q) / (probKnown*q + probNovel*(1-q));
	}
	
	public FreqStore getFreqStore() {
		return freqStore;
	}
	
	public static void main(String[] args) {
		
		//args = new String[1];
		//args[0] = "/media/MORE_DATA/detect_fp/HHT26_testvars.csv";
		
		UnitDistribution novelDist = new UnitDistribution(novelVarFreqs);
		UnitDistribution knownDist = new UnitDistribution(knownVarFreqs);
		
		File freqDB = new File("/media/MORE_DATA/detect_fp/novel_csvs/novel_exome_db_c17.csv");
		
		Classifier classifier;
		try {
			classifier = new Classifier(freqDB, novelDist, knownDist);
			
//			classifier.getFreqStore().emitFreqDistro();
//			System.exit(0);
			
//		double[] vals = new double[]{0.956043956043951,	0.9642857142857143,	1.0,1.0,0.23333333333333334,1.0,1.0,0.96875,0.975609756097561,1.0,0.3963963963963964,0.45985401459854014,0.9857142857142858,0.5254237288135594};
//		double pNovel = classifier.getDistOneLikelihood( vals );
//		double pKnown = classifier.getDistTwoLikelihood( vals );
//		System.out.println("Novel prob: " + pNovel + "\nKnown prob:" + pKnown);
//		System.exit(0);
//
//			double x = 0;
//			for(x=0; x<1.0; x+=0.1) {
//				double[] vals = new double[]{x};
//				double pNovel = classifier.getDistOneLikelihood( vals );
//				double pKnown = classifier.getDistTwoLikelihood( vals );
//				System.out.println(x + "\t" + pNovel + "\t" + pKnown);
//			}
//			System.exit(0);
			
			
			int hits = 0;
			int misses = 0;
			
			VariantPool pool = new VariantPool(new CSVFile(new File(args[0])));
			VariantPool falseposPool = new VariantPool();
			VariantPool allHits = new VariantPool();
			Histogram posteriorDist = new Histogram(0, 1, 25);
			List<VariantRec> allVars = new ArrayList<VariantRec>(100);
			
			for(String contig : pool.getContigs()) {
				for(VariantRec var : pool.getVariantsForContig(contig)) {
					double posterior = classifier.getFalsePosPosterior(var);
					if ( classifier.getFreqStore().hasEntry(var.getContig(), var.getStart())) {
						hits++;
						var.addProperty(VariantRec.FALSEPOS_PROB, posterior);
						allHits.addRecord(var);
						//if (posterior > 0.8)
						falseposPool.addRecordNoSort(var);
						posteriorDist.addValue(posterior);
						allVars.add(var);
					}
					else {
						misses++;
					}
				}
			}
			
			for(double x = 0; x<1.0; x+=0.1) {
				VariantPool slice =new VariantPool(pool.filterPool(new PosteriorFilter(x, x+0.1))); 
				double tt = slice.computeTTRatio();
				System.err.println(x + "\t" + tt + "\t" + slice.size());
			}
			
		
			VariantPool slice =new VariantPool(pool.filterPool(new PosteriorFilter(0.2, 0.8))); 
			double tt = slice.computeTTRatio();
			System.err.println(" > 0.2 && < 0.8 " + "\t" + tt + "\t" + slice.size());
	
			
			System.err.println("Total hits : " + hits);
			System.err.println("Misses : " + misses);
			System.err.println("Histogram of posteriors probs:\n" + posteriorDist.toString());
			System.err.println("Ts / Tv of all hits: " + allHits.computeTTRatio());
			falseposPool.sortAllContigs();
			List<String> keys = new ArrayList<String>();
			keys.add(VariantRec.DEPTH);
			keys.add(VariantRec.VAR_DEPTH);
			keys.add(VariantRec.FALSEPOS_PROB);
			keys.add(VariantRec.VQSR);
			falseposPool.listAll(System.out, keys);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		
	}
	
	public static class PosteriorFilter implements VariantFilter {
		double minPost = 0;
		double maxPost = 1.0;
		
		public PosteriorFilter(double min, double max) {
			this.minPost = min;
			this.maxPost = max;
		}
		
		@Override
		public boolean passes(VariantRec rec) {
			Double post = rec.getProperty(VariantRec.FALSEPOS_PROB);
			if (post == null)
				return false;
			else {
				return post > minPost && post <= maxPost;
			}
		}
		
	}
	
	public static final double[] knownVarFreqs = new double[]{9.728348854134176E-5,3.334373004203241E-4,4.151257259130538E-4,5.96325506096927E-4,0.001154777287647225,0.0026585869387633857,0.0038163347146103464,0.004662181229485066,0.005451588468564809,0.005684771792244055,0.007645294004069569,0.007836890492952516,0.00886616465416091,0.008940426859154303,0.010384826746275751,0.012338665359651859,0.016818161564853183,0.015253456905642442,0.02226380905701852,0.022677449538831707,0.03917405575606351,0.04138929733101635,0.04830830697025056,0.05081391376672756,0.03983944511280429,0.059527098278602086,0.03743854802536797,0.029149400704005703,0.023620579542247767,0.013546168812844392,0.018101412467138974,0.010632119888903741,0.006676172228905821,0.0090236005287469,0.0043918668033091236,0.006249907172243758,0.0036069152965289846,0.0038727739904053232,0.003340313980602712,0.001885517384782189,0.006512795377920361,0.004663666473584934,0.0045478174337952445,0.004172050676528687,0.0044438503468044975,0.00681281468609366,0.008608474802833846,0.013790491467272646,0.03215850525033789,0.07011837395475946};
	public static final double[] novelVarFreqs = new double[]{0.005802200110268056,0.01812859355719499,0.023458216282916326,0.025164745727112813,0.028131481530100554,0.03712357898605897,0.0413636482974087,0.041376775446979444,0.03674289164850744,0.032778492478143295,0.03533828664443803,0.030796292892961222,0.03129512457664942,0.029732993777731104,0.03044185985455118,0.032358423691879544,0.03233216939273806,0.03183333770904986,0.03368426579852451,0.030454987004121924,0.03768804641760088,0.034196224631783456,0.034064953136076034,0.032410932290162515,0.026306807739767388,0.03537766809315025,0.025230381474966524,0.021843576885715036,0.01966447005697183,0.01366536270314264,0.014846806164509438,0.011630654519677597,0.010173540917325212,0.009359657643939196,0.007101787917771535,0.006760482028932238,0.005500275670140986,0.004791409593320906,0.00427945076006196,0.002809210008138833,0.003990653469505632,0.0033080416918270365,0.0028485914568510593,0.0024153955210165666,0.002087216781748011,0.002205361127884691,0.002244742576596918,0.0023760140723043397,0.002323505474021371,0.0016408936963427762};
	
}