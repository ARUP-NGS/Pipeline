package util.coverage;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import pipeline.Pipeline;
import util.Interval;
import util.bamWindow.BamWindow;

/**
 * A CoverageCalculator can quickly compute a histogram describing read depth
 * in the given BAM file in regions defined by the intervals list. It does this using multiple threads
 * and a BAMWindow object (one for each thread).   
 * @author brendan
 *
 */
public class CoverageCalculator {
	
	
	protected File inputBam = null;
	protected HasIntervals intervals;
	private int threads = Math.max(1, Runtime.getRuntime().availableProcessors()/2);
	private int minMQ = 0;
	private final boolean countTemplates;
	
	/**
	 * Creates a new CoverageCalculator object that will examine the given BAM file over
	 * the given set of intervals
	 * @param inputBam
	 * @param intervals
	 * @throws IOException
	 */
	public CoverageCalculator(File inputBam, HasIntervals intervals, boolean countTemplates) throws IOException {
		this.inputBam = inputBam;
		this.intervals = intervals;
		this.countTemplates = countTemplates;
	}
	
	/**
	 * Creates a new CoverageCalculator object that will examine the given BAM file over
	 * the given set of intervals, requiring a minimum Mapping Quality.
	 * @param inputBam
	 * @param intervals
	 * @throws IOException
	 */
	public CoverageCalculator(File inputBam, HasIntervals intervals, int minMQ, boolean countTemplates) throws IOException {
		this.inputBam = inputBam;
		this.intervals = intervals;
		this.minMQ = minMQ;
		this.countTemplates = countTemplates;
	}

	/**
	 * Set the total number of threads to use. 
	 * @param threads
	 */
	public void setThreadCount(int threads) {
		this.threads = threads;
		if (threads < 1) {
			threads = 1;
		}
	}
	
	/**
	 * Compute the mean coverage separately for each interval and return them all in a big list
	 * @return
	 * @throws InterruptedException
	 */
	public List<IntervalCovSummary> computeCoverageByInterval() throws InterruptedException {
		
		List<IntervalCovSummary> covs = new ArrayList<IntervalCovSummary>(1024);
		List<CovCalculator> jobs = new ArrayList<CovCalculator>();
		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool( threads );

		for(String chr : intervals.getContigs()) {
			CovCalculator covJob = new CovCalculator(inputBam, chr, intervals.getIntervalsForContig(chr), new int[32768], countTemplates, getMinMQ());
			pool.submit(covJob);
			jobs.add(covJob);
		}
		
		//Wait until all threads complete
		pool.shutdown();
		pool.awaitTermination(10, TimeUnit.DAYS);
		
		for(CovCalculator job : jobs) {
			covs.addAll(job.getIntervalResults());
		}
		
		Collections.sort(covs);
		
		return covs;
	}
	
	/**
	 * Using multiple threads, compute the number of bases in the target intervals that are covered by X
	 * reads, and return that information as a histogram. The i-th element of the histogram is the number of
	 * positions covered by exactly i reads. For instance, if array[22]=100, then 100 positions have a 
	 * depth of 22.   Right now the max histogram length is 32768, meaning we don't keep track of depths
	 * greater than 32768. 
	 * @return
	 * @throws InterruptedException
	 */
	public int[] computeOverallCoverage() throws InterruptedException {
		int maxSubIntervalSize = 10000;
		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool( threads );
		int[] overallDepths = new int[32768];
		
		for(String chr : intervals.getContigs()) {
			List<Interval> subIntervals = new ArrayList<Interval>();
			for(Interval interval : intervals.getIntervalsForContig(chr)) {
				subIntervals.add(interval);
				
				if (subIntervals.size() > maxSubIntervalSize) {
					CovCalculator covJob = new CovCalculator(inputBam, chr, subIntervals, overallDepths, countTemplates, getMinMQ());
					pool.submit(covJob);
					subIntervals = new ArrayList<Interval>();
				}
			}
			
			//There may still be a few remaining 
			if (subIntervals.size() > 0) {
				CovCalculator covJob = new CovCalculator(inputBam, chr, subIntervals, overallDepths, countTemplates, getMinMQ());
				pool.submit(covJob);
				subIntervals = new ArrayList<Interval>();
			}
		}
		
		//Wait until all threads complete
		pool.shutdown();
		pool.awaitTermination(10, TimeUnit.DAYS);

		
		return overallDepths;
	}
	

	public int getMinMQ() {
		return minMQ;
	}

	public void setMinMQ(int minMQ) {
		this.minMQ = minMQ;
	}
	
	/**
	 * Compute the mean from the depth distribution
	 * @param depths
	 * @return
	 */
	public static double getMean(int[] depths) {
		double tot = 0;
		double sum = 0;
		for(int i=0; i<depths.length; i++) {
			sum += depths[i];
			tot += i*depths[i];
		}
		
		return tot / sum;
	}
	
	/**
	 * Compute the median of the depth distribution
	 * @param depths
	 * @return
	 */
	public static double getMedian(int[] depths) {
		double sum = 0;
		for(int i=0; i<depths.length; i++) {
			sum += depths[i];
		}
		
		sum /= 2.0;
		for(int i=0; i<depths.length; i++) {
			sum -= depths[i];
			if (sum <= 0) {
				return i;
			}
		}
		
		throw new IllegalStateException("This should never happen");
	}
	
	/**
	 * Converts the raw depth counts into an array where the value at index i is the fraction of bases with coverage > i
	 * @param depths
	 * @return
	 */
	public static double[] convertCountsToProportions(int[] depths) {
		int total = 0;
		
		double[] cdf = new double[depths.length];
		for(int i=0; i<depths.length; i++) {
			total += depths[i];
			cdf[i] = total;
		}
		
		for(int i=0; i<cdf.length; i++) {
			cdf[i] = 1 - cdf[i]/(double)total;
		}
		
		return cdf;
	}
	
	/**
	 * Print a nice little summary to the given printstream
	 * @param depths
	 * @param out
	 */
	public void summarize(int[] depths, PrintStream out) {
		int total = 0;
		
		double[] cdf = new double[depths.length];
		int maxNonZero = 0;
		for(int i=0; i<depths.length; i++) {
			total += depths[i];
			cdf[i] = total;
			if (depths[i] > 0) {
				maxNonZero = i;
			}
		}
		
		NumberFormat formatter = new DecimalFormat("0.00");
		
		out.println("Depth\t% bases covered");
		for(int i=0; i<maxNonZero; i++) {
			out.println(i + "\t" + formatter.format(100.0 - 100.0*cdf[i]/(double)total) );
		}
	}

	
	
	/**
	 * Stores single-interval specific coverage information, used when we compute
	 * coverage by interval 
	 * @author brendan
	 *
	 */
	public class IntervalCovSummary implements Comparable<IntervalCovSummary> {
		String chr;
		Interval interval;
		public double meanDepth;
		
		public IntervalCovSummary(String chr, Interval interval, double meanDepth) {
			this.chr = chr;
			this.interval = interval;
			this.meanDepth = meanDepth;
		}
		
		public String toString() {
			return chr + ": " + interval.begin + "-" + interval.end + "\t:\t" + meanDepth;
		}

		public int intervalSize() {
			return interval.end - interval.begin;
		}
		
		public String getChr() {
			return chr;
		}
		
		public Interval getInterval() {
			return interval;
		}
		/**
		 * Returns exact mean depth (not computed from histogram)
		 * @return
		 */
		public double getMeanDepth() {
			return meanDepth;
		}
		
		@Override
		/**
		 * Compare interval coverage summaries based on interval position
		 */
		public int compareTo(IntervalCovSummary ic) {
			if (! this.chr.equals(ic.chr)) {
				//Annoyingly, see if we can parse integers from the chr names... 
				try {
					Integer chr1val = Integer.parseInt(this.chr);
					Integer chr2val = Integer.parseInt(ic.chr);
					return chr1val.compareTo(chr2val);
				} catch (Exception ex) {
					//Don't worry about it, we're probably just looking at chr X or something
				}
				return chr.compareTo(ic.chr);
			} else {
				return this.interval.compareTo(ic.interval);
			}
		}
	}
	

	
	/**
	 * This class encapsulates a single job or task of computing coverage. It implements Runnable, so it
	 * can be executed by a thread in a thread pool. When that happens, it just calls the 'calculateDepthHistogram'
	 * function. 
	 * @author brendanofallon
	 *
	 */
	public class CovCalculator implements Runnable {

		private int[] depths;
		private File inputBam;
		private String chr;
		private List<Interval> subIntervals;
		private List<IntervalCovSummary> intervalResults = null;
		private boolean countTemplates;
		final int minMapQ;

		private boolean done = false;
		private Exception error = null;
		private int sitesAssessed = 0;
		private long covSum = 0L;
		
		public CovCalculator(File inputBam, String chr, List<Interval> subIntervals, int[] depths, boolean countTemplates, int minMapQ) {
			this.inputBam = inputBam;
			this.chr = chr;
			this.subIntervals = subIntervals;
			this.depths = depths;
			this.countTemplates = countTemplates;
			this.minMapQ = minMapQ;
		}
		
		@Override
		public void run() {
			intervalResults = new ArrayList<IntervalCovSummary>();
			try {
				System.err.println("Beginning job for " + subIntervals.size() + " intervals..");
				BamWindow window = new BamWindow(inputBam, minMQ);
				
				for(Interval interval : subIntervals) {
					CovResult result = CoverageCalculator.calculateDepthHistogram(window, chr, interval.begin, interval.end, depths, countTemplates);
					double mean = (double)result.covSum / (double)result.sitesAssessed;
					IntervalCovSummary intervalCov = new IntervalCovSummary(chr, interval, mean);
					intervalResults.add(intervalCov);
				}
				
				window.close();
				done = true;
			}
			catch (Exception ex) {
				this.error = ex;
				ex.printStackTrace();
				Logger.getLogger(Pipeline.primaryLoggerName).severe("Exception in coverage calculation task: " + ex);
			}
		}
		
		public boolean isDone() {
			return done;
		}
		
		public boolean isError() {
			return error != null;
		}
		
		public List<IntervalCovSummary> getIntervalResults() {
			return intervalResults;
		}
		
		public Exception getException() {
			return error;
		}
		
		public int[] getDepths() {
			return depths;
		}
		
		public int getSitesAssessed() {
			return sitesAssessed;
		}
		
		public long getCovSum() {
			return covSum;
		}
	}
	
	/**
	 * For each index i in first, add the value of first[i] to second[i]
	 * @param first
	 * @param second
	 */
	public static void addFirstToSecond(int[] first, int[] second) {
		for(int i=0; i<first.length; i++) {
			second[i] += first[i];
		}
	}

	public static CovResult calculateTemplateDepthHistogram(BamWindow bam, String chr, int start, int end, int[] depths) {
		return calculateDepthHistogram(bam, chr, start, end, depths, true);
	}
	/**
	 * Actually perform the depth computation. This examines each position in the interval and sees how many reads
	 * map to it, and for each position increments the depths array at index (depth) by one. For instance, if
	 * there are X reads mapping some position, depths[X] is incremented by one. 
	 * Modified: Not this examines only every Z (=4) positions, and increments depth by Z at each one. This is way faster but not exactly
	 * correct (although its asymptotically correct) for smaller regions. 
	 * @param bam
	 * @param chr
	 * @param start
	 * @param end
	 * @param depths
	 * @param countTemplates If true, count inferred number of templates at position, instead of number of reads
	 */
	public static CovResult calculateDepthHistogram(BamWindow bam, String chr, int start, int end, int[] depths, boolean countTemplates) {
		int advance = 4;
		CovResult result = new CovResult();
		
		//If this is a tiny interval just look at each base, this allows us to look at single sites accurately
		if (end-start < 40) {
			advance = 1;
		}
		bam.advanceTo(chr, start);
		
		long covSum = 0L; //Tracks sum of all coverage, used for calculating mean coverage exactly
		int sitesAssessed = 0; //Tracks total number of sites examined, used for calculating exact mean
		int pos = start;
		while(pos < end) {
			int depth = -1;
			if (countTemplates) {
				depth = bam.templateCount();
			} else {
				depth = bam.size();
			}
			
			sitesAssessed += advance;
			covSum += advance * depth;
			depth = Math.min(depth, depths.length-1);
			depths[depth]+=advance; //We assume this base and the next 'advance' bases all have the same coverage
			bam.advanceBy(advance);
			pos += advance;
		}
		

		result.sitesAssessed = sitesAssessed;
		result.covSum = covSum;
		return result;
	}
	
	static class CovResult {
		int sitesAssessed = 0;
		long covSum = 0;
	}


}

