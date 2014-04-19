package util.coverage;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import buffer.BEDFile;
import buffer.IntervalsFile;
import util.Interval;
import util.bamWindow.BamWindow;

public class CoverageCalcTest {
	
	
	protected File inputBam = null;
	protected IntervalsFile intervals;
	private int threads = Runtime.getRuntime().availableProcessors();
	
	public CoverageCalcTest(File inputBam, IntervalsFile intervals) throws IOException {
		this.inputBam = inputBam;
		this.intervals = intervals;
		if (! intervals.isMapCreated()) {
			intervals.buildIntervalsMap();
		}
	}

	public void setThreadCount(int threads) {
		this.threads = threads;
		if (threads < 1) {
			threads = 1;
		}
	}
	
	public int[] computeOverallCoverage() throws InterruptedException {
		int maxSubIntervalSize = 10000;
		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool( threads );
		int[] overallDepths = new int[1000];
		
		System.out.println("Submitting jobs..");
		for(String chr : intervals.getContigs()) {
			List<Interval> subIntervals = new ArrayList<Interval>();
			for(Interval interval : intervals.getIntervalsForContig(chr)) {
				subIntervals.add(interval);
				
				if (subIntervals.size() > maxSubIntervalSize) {
					CovCalculator covJob = new CovCalculator(inputBam, chr, subIntervals, overallDepths);
					pool.submit(covJob);
					subIntervals = new ArrayList<Interval>();
				}
			}
			
			//There may still be a few remaining 
			if (subIntervals.size() > 0) {
				CovCalculator covJob = new CovCalculator(inputBam, chr, subIntervals, overallDepths);
				pool.submit(covJob);
				subIntervals = new ArrayList<Interval>();
			}
		}
		
		System.out.println("All jobs have been submitted, approx task count is: " + pool.getTaskCount());
		pool.shutdown();
		pool.awaitTermination(10, TimeUnit.DAYS);
		
		return overallDepths;
	}
	
	public static double getMean(int[] depths) {
		double tot = 0;
		double sum = 0;
		for(int i=0; i<depths.length; i++) {
			sum += depths[i];
			tot += i*depths[i];
		}
		
		return tot / sum;
	}
	
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
			cdf[i] = 100.0 - 100.0*cdf[i]/(double)total;
		}
		
		return cdf;
	}
	
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
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		File inputBam = new File(args[0]);
		IntervalsFile intervals = new BEDFile(new File(args[1]));
		
		Date start = new Date();
		CoverageCalcTest covCalc = new CoverageCalcTest(inputBam, intervals);
		int[] depths = covCalc.computeOverallCoverage();
		
		
		for(int i=0; i<depths.length; i++) {
			System.out.println(i + "\t" + depths[i]);
		}
		
		covCalc.summarize(depths, System.out);
		
		Date end= new Date();
		long elapsed = end.getTime() - start.getTime() ;
		int elapsedSeconds = (int)(elapsed / 1000);
		System.out.println("\n Elapsed time: " + elapsedSeconds + " seconds" );

		
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
		private boolean done = false;
		
		public CovCalculator(File inputBam, String chr, List<Interval> subIntervals, int[] depths) {
			this.inputBam = inputBam;
			this.chr = chr;
			this.subIntervals = subIntervals;
			this.depths = depths;
		}
		
		@Override
		public void run() {
			BamWindow window = new BamWindow(inputBam);
			for(Interval interval : subIntervals) {
				CoverageCalcTest.calculateDepthHistogram(window, chr, interval.begin, interval.end, depths);
			}
			System.out.println("Done with chr " + chr + " " + subIntervals.size() + " subintervals");
			window.close();
			done = true;
		}
		
		public boolean isDone() {
			return done;
		}
		
		public int[] getDepths() {
			return depths;
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

	/**
	 * Actually perform the depth computation. This examines each position in the interval and sees how many reads
	 * map to it, and for each position increments the depths array at index (depth) by one. For instance, if
	 * there are X reads mapping some position, depths[X] is incremented by one. 
	 * @param bam
	 * @param chr
	 * @param start
	 * @param end
	 * @param depths
	 */
	public static void calculateDepthHistogram(BamWindow bam, String chr, int start, int end, int[] depths) {
		int advance = 4;
		bam.advanceTo(chr, start);
		
		boolean cont = bam.hasMoreReadsInCurrentContig();
		while(cont && bam.getCurrentPosition() < end) {
			int depth = bam.size();
			if (depth < depths.length) {
				depths[depth]++;
			}
			cont = bam.advanceBy(advance);
		}
	}
}
