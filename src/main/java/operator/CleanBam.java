package operator;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileReader.ValidationStringency;

import operator.MultiOperator.TaskOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import util.ElapsedTimeFormatter;
import buffer.BAMFile;
import buffer.FileBuffer;

/**
 * Uses util.CleanBam to remove reads with unmapped mates and other low-quality
 * and duplicate reads
 * @author brendan
 *
 */
public class CleanBam extends MultiOperator {

	private String samtoolsPath = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPreferredThreadCount() );
		
		if (samtoolsPath == null) {
			Object samPropsPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
			samtoolsPath = samPropsPath.toString();
		}
		
		Date start = new Date();
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		if (inputFiles == null) {
			throw new OperationFailedException("InputFiles buffer has not been initialized for MultiOperator " + getObjectLabel(), this);
		}
		logger.info("Beginning parallel multi-operation " + getObjectLabel() + " with " + inputFiles.getFileCount() + " input files");
		if (inputFiles != null && checkContigs) {
			checkInputContigs();
		}
		
		List<CleanBamJob> jobs = new ArrayList<CleanBamJob>();
		
		for(int i=0; i<inputFiles.getFileCount(); i++) {
			FileBuffer inputBuffer = inputFiles.getFile(i);
			
			if (inputBuffer instanceof BAMFile) {
				String outputBAMPath = inputBuffer.getAbsolutePath().replace(".bam", ".clean.bam");
				BAMFile outputBAM = new BAMFile(new File(outputBAMPath));
				this.addOutputFile(outputBAM);
				CleanBamJob task = new CleanBamJob( (BAMFile)inputBuffer, outputBAM, logger);
				jobs.add(task);
				threadPool.submit(task);	
			}
		}
		
		try {
			logger.info("All tasks have been submitted to multioperator " + getObjectLabel() + ", now awaiting termination...");
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(7, TimeUnit.DAYS); //Wait until all tasks have completed
			
			//Check for errors
			boolean allOK = true;
			for(CleanBamJob job : jobs) {
				if (job.isError()) {
					allOK = false;
					logger.severe("Parallel task in operator " + getObjectLabel() + " encountered error: " + job.getException());
				}
			}
			if (!allOK) {
				throw new OperationFailedException("One or more tasks in parallel operator " + getObjectLabel() + " encountered an error.", this);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		if (outputFiles != null && checkContigs) {
			checkOutputContigs();
		}
		if (inputFiles != null && outputFiles != null && inputFiles.getFileCount() != outputFiles.getFileCount()) {
			logger.severe("Uh oh, we didn't find the name number of input as output files! input files size: " + inputFiles.getFileCount() + " output files: " + outputFiles.getFileCount());
		}
		
		Date end = new Date();
		logger.info("Multi-operation " + getObjectLabel() + " has completed (Total time " + ElapsedTimeFormatter.getElapsedTime(start.getTime(), end.getTime()) + " )");

	}
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Actual cleaning operation happens here
	 * @param inputBAM Input bam to be cleaned
	 * @param outputBAM Output, cleaned bam
	 */
	public static void cleanBAM(File inputBAM, File outputBAM) {
			SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
			final SAMFileReader inputSam = new SAMFileReader(inputBAM);
			inputSam.setValidationStringency(ValidationStringency.LENIENT);
			
			final SAMFileWriter outputSam = new SAMFileWriterFactory().makeSAMOrBAMWriter(inputSam.getFileHeader(),
					true, outputBAM);

			int readCount = 0;
			int outReadCount = 0;
			for (final SAMRecord samRecord : inputSam) {
				readCount++;
				if ( !samRecord.getMateUnmappedFlag() && (!samRecord.getReadUnmappedFlag()) && (!samRecord.getReadFailsVendorQualityCheckFlag()) && (!samRecord.getDuplicateReadFlag())) {
					outputSam.addAlignment(samRecord);
					outReadCount++;
				}
			}

			outputSam.close();
			inputSam.close();
	}
	
	/**
	 * Little wrapper for commands so they can be executed in a thread pool
	 * @author brendan
	 *
	 */
	public class CleanBamJob implements Runnable {

		Logger logger;
		boolean isError = false;
		Exception exception = null;
		final BAMFile inputBAM;
		final BAMFile outputBAM;
		
		public CleanBamJob(BAMFile inputBAM, BAMFile outputBAM, Logger logger) {
			this.inputBAM = inputBAM;
			this.outputBAM = outputBAM;
			this.logger = logger;
		}
		
		@Override
		public void run() {
			try {
				Date begin = new Date();
				logger.info("Beginning bam cleaning for input bam : " + inputBAM.getAbsolutePath() + "\n Total tasks: " + threadPool.getTaskCount() + "\n Active tasks: " + threadPool.getActiveCount() + "\n Completed tasks: " + threadPool.getCompletedTaskCount());
				cleanBAM(inputBAM.getFile(), outputBAM.getFile());
				String command = samtoolsPath + " index " + outputBAM.getAbsolutePath();
				logger.info("Indexing cleaned bam file : " + outputBAM.getAbsolutePath() + "\n Total tasks: " + threadPool.getTaskCount() + "\n Active tasks: " + threadPool.getActiveCount() + "\n Completed tasks: " + threadPool.getCompletedTaskCount() );
				executeCommand(command);
				Date end = new Date();
				logger.info("Done indexing and cleaning bam file " + inputBAM.getAbsolutePath() + " (elapsed time " + ElapsedTimeFormatter.getElapsedTime(begin.getTime(), end.getTime()) + ")\n Total tasks: " + threadPool.getTaskCount() + "\n Active tasks: " + threadPool.getActiveCount() + "\n Completed tasks: " + threadPool.getCompletedTaskCount());
			} catch (OperationFailedException e) {
				// TODO Auto-generated catch block
				isError = true;
				exception = e;
				e.printStackTrace();
			}
		}
		
		/**
		 * Returns true if an operation failed exception was thrown while executing the command
		 * @return
		 */
		public boolean isError() {
			return isError;
		}
		
		/**
		 * If an exception was thrown during execution, this is it
		 * @return
		 */
		public Exception getException() {
			return exception;
		}
	}


}
