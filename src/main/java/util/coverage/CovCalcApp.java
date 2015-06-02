package util.coverage;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import util.coverage.CoverageCalculator.IntervalCovSummary;
import util.reviewDir.ManifestParseException;
import util.reviewDir.ReviewDirectory;
import buffer.BEDFile;
import buffer.IntervalsFile;

/**
 * Contains the main method and some command line parsing stuff for running coverage calculations from the command line
 * @author brendan
 *
 */
public class CovCalcApp {
	
	static class CovRunner implements Runnable {
		
		private File inputBam;
		private IntervalsFile intervals;
		private Exception ex = null;
		private List<IntervalCovSummary> sampleResults;
		private boolean done = false;
		private boolean normalizeDepths = false;
		private double grandMeanDepth = -1; //Mean depth across all intervals, used for normalization

		
		public CovRunner(File inputBAM, IntervalsFile intervals, boolean normalizeDepths) {
			this.inputBam = inputBAM;
			this.intervals = intervals;
			this.normalizeDepths = normalizeDepths;
		}
		
		public CovRunner(File inputBAM, IntervalsFile intervals) {
			this(inputBAM, intervals, false);
		}


		@Override
		public void run() {
			CoverageCalculator covCalc;
			try {
				System.err.println("Beginning execution for "+ inputBam.getName());
				covCalc = new CoverageCalculator(inputBam, intervals);
				sampleResults = covCalc.computeCoverageByInterval();
				
				double totalExtent = 0;
				double totalDepth = 0;
				for(IntervalCovSummary cov : sampleResults) {
					totalExtent += (double)cov.intervalSize();
					totalDepth += cov.meanDepth*cov.intervalSize();
				}
				grandMeanDepth = totalDepth / totalExtent;
				
				//results are initially in more-or-less random order, so sort them by interval position
				//so at least it's consistent. May differ from order in input BED file
				Collections.sort(sampleResults);
				System.err.println("Finished execution for "+ inputBam.getName() + " grand mean depth: " + grandMeanDepth);
			} catch (IOException e) {
				System.err.println("Exception encountered for " +inputBam.getName() + ": " + e.getLocalizedMessage());
				ex = e;
			} catch (InterruptedException e) {
				ex = e;
			}
			
			done = true;
		}
		
		public List<IntervalCovSummary> getResults() {
			return sampleResults;
		}
		
		public boolean isDone() {
			return done;
		}
		
		public Exception getException() {
			return ex;
		}
		
	}

	/**
	 * If arg ends with .bam, return arg. Else, assume arg is the name of a review directory,
	 * and file the path of the bam inside of it, and return that
	 * @param arg
	 * @return
	 */
	public static File findBAM(String arg) {
		if (arg.endsWith(".bam")) {
			return new File(arg);
		}
		
		try {
			ReviewDirectory rv = new ReviewDirectory(arg);
			return rv.getBAMFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ManifestParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		if (args.length==0 || args[0].startsWith("-h")) {
			System.out.println("Coverage Calculator utility, v0.03");
			System.out.println("\n Usage: java -jar [bed file] [bam file] [more bam files...]");
			System.out.println("\n Emits mean depth of coverage for all intervals in BED file to output.");
			return;
		}
		
		boolean normalizeDepths = false;
		for(String arg : args) {
			if (arg.startsWith("-norm") || arg.startsWith("--norm")) {
				normalizeDepths = true;
			}
		}
		
		int threads = 8;
		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
		
		
		//Read in all BED intervals into a single file
		IntervalsFile intervals = new BEDFile(new File(args[0]));
		intervals.buildIntervalsMap();
		
		Date start = new Date();
		List<CovRunner> runners = new ArrayList<CovRunner>();
		
		//Create a CovRunner for each inputBAM and add it to the thread pool, they will
		//run automatically
		for(int i=1; i<args.length; i++) {
			if (args[i].startsWith("-")) {
				continue;
			}
			
			File inputBam = findBAM(args[i]);
			if (inputBam != null) {
				CovRunner runner = new CovRunner(inputBam, intervals);
				runners.add(runner);
				pool.submit(runner);
			} else {
				System.err.println("Warning: Couldn't find a BAM file for " + args[i]);
			}
		}
		

		//Wait until all CovRunners are done
		pool.shutdown();
		pool.awaitTermination(10, TimeUnit.DAYS);
		
		//Scan for exceptions, build a list of those that succeeded 
		
		List<CovRunner> worked = new ArrayList<CovRunner>();
		for(CovRunner cr : runners) {
			if (cr.getException() != null) {
				System.err.println("Ignoring " + cr.inputBam.getName() + " which failed, exception: " + cr.getException().getLocalizedMessage());
			} else {
				worked.add(cr);
			}
		}
		
		DecimalFormat formatter = new DecimalFormat("##0.0##");
		
		
		for(int i=0; i<intervals.getIntervalCount(); i++) {
			System.out.print(runners.get(0).getResults().get(i).chr + ":" + runners.get(0).getResults().get(i).interval + "\t");
			for(CovRunner cr : worked) {
				if (normalizeDepths) {
					System.out.print(formatter.format(cr.getResults().get(i).meanDepth/cr.grandMeanDepth) + "\t");
				} else {
					System.out.print(formatter.format(cr.getResults().get(i).meanDepth) + "\t");
				}
			}
			System.out.println();
			
		}
		
		
		Date end= new Date();
		long elapsed = end.getTime() - start.getTime() ;
		int elapsedSeconds = (int)(elapsed / 1000);
		System.out.println("\n Elapsed time: " + elapsedSeconds + " seconds" );

		
	}
}
