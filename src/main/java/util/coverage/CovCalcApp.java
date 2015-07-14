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

import util.Interval;
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
	private static int minMQ = 0;
	
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
				covCalc = new CoverageCalculator(inputBam, intervals, minMQ);
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

	private static int DEFAULT_THREADS = 8;
	private static String DEFAULT_FORMAT = "interval";

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
		int threads;
		threads = DEFAULT_THREADS;
		String format = DEFAULT_FORMAT;
		
		if (args.length==0 || args[0].startsWith("-h")) {
			System.err.println("Coverage Calculator utility, v0.03");
			System.err.println("\n Usage: java -jar [bed file] [bam file] [more bam files...] [-t/--threads numThreads] [-f/--format outputFormat] [-m/--minMQ minimumMappingQuality].");
			System.err.println("If -t/--threads flag is used, it overrides the default number of threads " + DEFAULT_THREADS + ".");
			System.err.println("-m/--minMQ sets a minimum mapping quality for read inclusion in coverage calculations.");
			System.err.println("Setting -f/--format to 'bed', overrides the default format used (interval).");
			System.err.println("\n Emits mean depth of coverage for all intervals in BED file to output.");
			return;
		}
		boolean normalizeDepths = false;
		for(int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-norm") || args[i].startsWith("--norm")) {
				normalizeDepths = true;
			}
			if(args[i].startsWith("--threads") || args[i].equals("-t")){
				i++;
				try {
					threads = Integer.parseInt(args[i]);
					System.err.println("Number of threads set to " + threads + ", overriding default value of " + DEFAULT_THREADS + ".");
				}
				catch(NumberFormatException e){
				System.err.println("Could not correctly parse number of threads from --threads argument " + args[i] + ". Switching back to default value of " + threads + ".");
				}
			}
			if(args[i].startsWith("--format") || args[i].equals("-f")){
				i++;
				if(args[i].startsWith("bed"))
					format = "bed";
				else if(args[i].startsWith("int")){
					format = "interval";
				}
				else {
					try {
						throw new IllegalArgumentException("Format tag value invalid: " + args[i] + ". Valid arguments: bed, interval");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			if(args[i].startsWith("--minMQ") || args[i].equals("-m")){
				i++;
				minMQ = Integer.parseInt(args[i]);
			}
		}
		/*
		for(int i = 0; i < args.length; i++){
			try {
				threads = Integer.parseInt(args[i]);
			}
			catch(NumberFormatException e){
				//
			}
		}
		*/
		
		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
		
		
		//Read in all BED intervals into a single file
		IntervalsFile intervals = new BEDFile(new File(args[0]));
		intervals.buildIntervalsMap();
		
		Date start = new Date();
		List<CovRunner> runners = new ArrayList<CovRunner>();
		
		//Create a CovRunner for each inputBAM and add it to the thread pool, they will
		//run automatically
		for(int i=1; i<args.length; i++) {
			if(args[i].equals("-t") || args[i].startsWith("--threads")){
				i++;
				continue;  // Step over two - both the flag and the value.
			}
			else if (args[i].startsWith("--format") || args[i].equals("-f")){
				i++;
				continue;
			}
			else if (args[i].startsWith("--minMQ") || args[i].equals("-m")){
				i++;
				continue;
			}
			else if (args[i].startsWith("-")) {
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
			if(format.equals("bed")){
				IntervalCovSummary tmpIntervalCovSummary = runners.get(0).getResults().get(i);
				System.out.print(tmpIntervalCovSummary.chr + "\t" + tmpIntervalCovSummary.interval.begin + "\t" + tmpIntervalCovSummary.interval.end + "\t");
			}
			else
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
		System.err.println("\n Elapsed time: " + elapsedSeconds + " seconds" );

		
	}
}
