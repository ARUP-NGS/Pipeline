package operator.freebayes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import operator.OperationFailedException;
import pipeline.Pipeline;
import util.Interval;
import buffer.BEDFile;

/**
 * A version of the FreeBayes operator that splits the input BED file into 
 * sub-regions by chromosome, then submits individuals FreeBayes jobs to a threadpool
 * for execution
 * @author brendan
 *
 */
public class ParallelFreebayes extends FreeBayes {

	
	public void performOperation() throws OperationFailedException {
		
		List<BEDFile> subRegions = splitBEDFileByChr(inputBED);
		
		parseOptions();
		Logger.getLogger(Pipeline.primaryLoggerName).info("Freebayes is looking for SNPs with reference " + refBuf.getFilename() + " in source BAM file of " + inputBuffers.get(0).getFilename() + "." );
		

		ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPipelineOwner().getThreadCount(); );

		
		for(BEDFile region : subRegions) {
			Runnable runner = new FreeBayesRunner(region, getCommand(""));
			threadPool.submit(runner);
		}
		
		threadPool.shutdown();
		threadPool.awaitTermination(10, TimeUnit.DAYS);
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("ParallelFreebayes operator is totally done.");
	}
	
	/**
	 * Splits the input BED file into multiple sub-bed files by chromosome, and 
	 * returns a list of the newly created sub-bed files. 
	 * @param inputBED
	 * @return
	 * @throws IOException
	 */
	private List<BEDFile> splitBEDFileByChr(BEDFile inputBED) throws IOException {
		List<BEDFile> subbeds = new ArrayList<BEDFile>();
		for(String chr : inputBED.getContigs()) {
			List<Interval> ints = inputBED.getIntervalsForContig(chr);
			String subbedName = inputBED.getAbsolutePath().replace(".bed", "_chr" + chr + ".bed");
			File subbedFile = new File(subbedName);
			BufferedWriter writer = new BufferedWriter(new FileWriter(subbedFile));
			for(Interval interval : ints) {
				writer.write(chr + "\t" + interval.begin + "\t" + interval.end + "\n");
			}
			if (ints.size() > 0) {
				subbeds.add(new BEDFile(subbedFile));
			}
			
			writer.close();
		}
		
		return subbeds;
	}
	
	
	class FreeBayesRunner implements Runnable {

		private final BEDFile regions;
		private final String command;
		private Exception ex = null;
		
		public FreeBayesRunner(BEDFile regions, String command) {
			this.regions = regions;
			this.command = command;
		}
		
		@Override
		public void run() {
			try {
				executeCommand(getCommand(" -t " + regions.getAbsolutePath()));
			} catch (OperationFailedException e) {
				e.printStackTrace();
				this.ex = e;
			}
		}
		
		/**
		 * Gets the exception that was thrown if this operation failed, or none 
		 * if execution completed successfully. 
		 * @return
		 */
		public Exception getException() {
			return ex;
		}
		
	}
}
