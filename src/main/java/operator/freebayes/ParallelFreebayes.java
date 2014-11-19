package operator.freebayes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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
import buffer.VCFFile;

/**
 * A version of the FreeBayes operator that splits the input BED file into 
 * sub-regions by chromosome, then submits individuals FreeBayes jobs to a threadpool
 * for execution, then merges the resulting VCFs back into a single vcf. 
 * @author brendan
 *
 */
public class ParallelFreebayes extends FreeBayes {

	
	public void performOperation() throws OperationFailedException {
		
		parseOptions();
		
		List<BEDFile> subRegions;
		try {
			subRegions = splitBEDFileByChr(inputBED);
		} catch (IOException e1) {
			throw new OperationFailedException(e1.getLocalizedMessage(), this);
		}
		
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Freebayes is looking for SNPs with reference " + refBuf.getFilename() + " in source BAM file of " + inputBuffers.get(0).getFilename() + "." );
		

		ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPipelineOwner().getThreadCount() );

		List<VCFFile> tempVCFs = new ArrayList<VCFFile>();
		List<FreeBayesRunner> runners = new ArrayList<FreeBayesRunner>();
		int count = 0;
		for(BEDFile region : subRegions) {
			count++;
			VCFFile vcf = new VCFFile(new File(getProjectHome() + "fb-tmp" + count + ".vcf") );
			tempVCFs.add(vcf);
			FreeBayesRunner runner = new FreeBayesRunner(region, vcf);
			runners.add(runner);
			threadPool.submit(runner);
			
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info(runners.size() + " jobs submitted to pool.");
		
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new OperationFailedException("Interrupted", this);
		}
		
		//Scan completed jobs for any that encountered an exception
		for(FreeBayesRunner runner : runners) {
			if (runner.getException() != null) {
				throw new OperationFailedException(runner.getException().getMessage(), this);
			}
		}
		
		
		try {
			Logger.getLogger(Pipeline.primaryLoggerName).info("ParallelFreebayes is merging VCFs into " + outputVCF.getAbsolutePath() );
			mergeVCFs(tempVCFs, outputVCF);
		} catch (IOException e) {
			throw new OperationFailedException(e.getLocalizedMessage(), this);
		}
		Logger.getLogger(Pipeline.primaryLoggerName).info("ParallelFreebayes operator is totally done.");
	}
	
	private void mergeVCFs(List<VCFFile> vcfs, VCFFile output) throws IOException {
		File dest = output.getFile();
		dest.createNewFile();
		boolean first = true;
		BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
		for(VCFFile source : vcfs) {
			
			BufferedReader reader = new BufferedReader(new FileReader(source.getAbsolutePath()));
			String line = reader.readLine();
			while(line != null) {
				if (first || !line.startsWith("#")) {
					writer.write(line + "\n");	
				}
				
				line = reader.readLine();
			}
			
			reader.close();
			first = false;
		}
		writer.close();
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
		inputBED.buildIntervalsMap();
		for(String chr : inputBED.getContigs()) {
			List<Interval> ints = inputBED.getIntervalsForContig(chr);
			String subbedName = this.getProjectHome() + "/" + inputBED.getFilename().replace(".bed", "_chr" + chr + ".bed");
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
		private final VCFFile vcf;
		private Exception ex = null;
		
		public FreeBayesRunner(BEDFile regions, VCFFile vcf) {
			this.regions = regions;
			this.vcf = vcf;
		}
		
		@Override
		public void run() {
			try {
				String cmd = getCommand(" -t " + regions.getAbsolutePath(), vcf);
				Logger.getLogger(Pipeline.primaryLoggerName).info("ParallelFreebayes operator is executing: " + cmd);
				executeCommand(cmd);
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
