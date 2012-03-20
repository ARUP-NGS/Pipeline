package operator.bwa;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;
import buffer.SAIFile;
import buffer.SAMFile;
import operator.OperationFailedException;
import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import util.ElapsedTimeFormatter;
import util.StringOutputStream;

/**
 * A class that uses BWA to align fastq files to a reference, where the fastq files may be paired-end reads
 * from multiple lanes. If so, corresponding pairs are come appear in separate multi-file-buffers and are
 * assumed to appear in the same lexicographical order, so that 
 * 
 * MultiBufferOne:
 * 	 reads_X_1.fq
 *   reads_Y_1.fq
 * 
 * MultiBufferTwo:
 * 	  reads_Y_2.fq
 *    reads_X_2.fq
 *    
 * will assume that reads_X_1.fq and reads_X_2.fq are opposing pairs of a paired-end read job
 * 
 * @author brendan
 *
 */
public class MultiLaneAligner extends PipedCommandOp {

	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String SKIPSAI = "skipsai";
	public static final String SINGLE_END = "single";
	
	protected String maxEditDist = "3"; //Maximum edit distance for alignment
	protected String pathToBWA = "bwa";
	protected int defaultThreads = 4;
	protected boolean pairedEnd = true; //By default, assume paired ends. SINGLE_END property is queried to set this to false
	protected int threads = defaultThreads;
	protected String referencePath = null;
	protected StringOutputStream errStream = new StringOutputStream();

	private List<StringPair> saiFileNames = new ArrayList<StringPair>();
	
	private ThreadPoolExecutor threadPool;
	protected MultiFileBuffer outputSAMs;
	
	public int getPreferredThreadCount() {
		return Pipeline.getPipelineInstance().getThreadCount();
	}

	@Override
	public void performOperation() throws OperationFailedException {
		Date start = new Date();
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		
		Object propsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.BWA_PATH);
		if (propsPath != null)
			pathToBWA = propsPath.toString();
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
		
		String threadsAttr = properties.get(THREADS);
		if (threadsAttr != null) {
			threads = Integer.parseInt(threadsAttr);
		}
		
		String singleEndAttr = properties.get(SINGLE_END);
		if (singleEndAttr != null) {
			pairedEnd = ! Boolean.parseBoolean(singleEndAttr);
		}
		if (pairedEnd)
			logger.info("Multi-lane aligner is using PAIRED-END mode");
		else
			logger.info("Multi-lane aligner is using SINGLE-END mode");
		
		
		FileBuffer reference = getInputBufferForClass(ReferenceFile.class);
		if (reference == null) {
			throw new OperationFailedException("No reference provided for MultiLaneAligner " + getObjectLabel(), this);
		}
		referencePath = reference.getAbsolutePath();
		
		boolean skipSAIGen = false;
		String skipsaiStr = properties.get(SKIPSAI);
		if (skipsaiStr != null) {
			skipSAIGen = Boolean.parseBoolean(skipsaiStr);
			logger.info("Parsed " + skipSAIGen + " for skip sai file generation");
		}
		
		List<FileBuffer> files = this.getAllInputBuffersForClass(MultiFileBuffer.class);
		if (files.size() != 1 && (!pairedEnd)) {
			throw new OperationFailedException("Need exactly one input files of type MultiFileBuffer for single-end mode", this);
		}
		if (files.size() != 2 && pairedEnd) {
			throw new OperationFailedException("Need exactly two input files of type MultiFileBuffer for paired-end mode", this);
		}
		
		MultiFileBuffer files1 = (MultiFileBuffer) files.get(0);
		MultiFileBuffer files2 = null; //Will be non-null only in case of paired-end mode
		
		//Initialize paired-end mode & perform some paired-end specific verification and logging
		if (pairedEnd) {
			files2 = (MultiFileBuffer) files.get(1);

			FilenameSorter sorter = new FilenameSorter();
			Collections.sort(files1.getFiles(), sorter);
			Collections.sort(files2.getFiles(), sorter);

			StringBuffer buff = new StringBuffer();
			System.out.println("Got " + files1.getFileCount() + " files in buffer 1, " + files2.getFileCount() + " files in buffer 2");
			for(int i=0; i<files1.getFileCount(); i++) {
				System.out.println("File " + i + " buf 1 : \t" + files1.getFile(i).getFile().getAbsolutePath());
				System.out.println("File " + i + " buf 2 : \t" + files2.getFile(i).getFile().getAbsolutePath());
				if (files1.getFile(i).getFile().getAbsolutePath().equals(files2.getFile(i).getFile().getAbsolutePath())) {
					throw new OperationFailedException("Paired files are the same file! File is : " + files1.getAbsolutePath(), this);
				}
				buff.append(files1.getFile(i).getFilename() + "\t" + files2.getFile(i).getFilename() + "\n");
			}
			logger.info("Following files are assumed to be paired-end reads: \n" + buff.toString());

			if (files1.getFileCount() != files2.getFileCount()) {
				throw new OperationFailedException("Multi-file sizes are not the same, one is " + files1.getFileCount() + " but the other is :" + files2.getFileCount(), this);
			}
		}
		
		FileBuffer outputBuffer = outputBuffers.get(0);
		
		if (outputBuffer instanceof MultiFileBuffer) {
			outputSAMs = (MultiFileBuffer) outputBuffer;
		}
		else {
			throw new OperationFailedException("Must have exactly one Multi-file buffer to store output SAM files", this);
		}
		
		logger.info("Beginning multi-lane alignment with " + files1.getFileCount() + " read pairs");
		
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( Math.max(1, getPreferredThreadCount()/threads) );
		
		if (pairedEnd) {
			runPairedEndAlignment(files1, files2, skipSAIGen);
		}
		else {
			runSingleEndAlignment(files1, skipSAIGen);
		}
		
		
		try {
			logger.info("All alignment jobs have been submitted to MultiLaneAligner, " + getObjectLabel() + ", now awaiting termination");
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(2, TimeUnit.DAYS); //Wait until all tasks have completed			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger.info("All bwa aln steps have completed, now creating SAM files");
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPreferredThreadCount() );
		//Now build sam files in parallel since we can do that and bwa can't
		for(StringPair saiFiles : saiFileNames) {
			SamBuilderJob makeSam = null;
			if (pairedEnd) {
				makeSam = new SamBuilderJob(saiFiles.readsOne, saiFiles.readsTwo, saiFiles.saiOne, saiFiles.saiTwo);
			}
			else {
				makeSam = new SamBuilderJob(saiFiles.readsOne, saiFiles.saiOne);
			}
			threadPool.submit(makeSam);
		}
		
		
		try {
			logger.info("All tasks have been submitted to MultiLaneAligner, " + getObjectLabel() + ", now awaiting termination");
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(2, TimeUnit.DAYS); //Wait until all tasks have completed			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		Date end = new Date();
		logger.info("MultiLaneAligner " + getObjectLabel() + " has completed (Total time " + ElapsedTimeFormatter.getElapsedTime(start.getTime(), end.getTime()) + " )");

	}
	
	/**
	 * Submit all jobs for a single-end alignment. This assumes that all files in the MultiFileBuffer are
	 * FastQFiles
	 * @param files
	 * @param skipSAIGen
	 */
	protected void runSingleEndAlignment(MultiFileBuffer files, boolean skipSAIGen) {
		for(int i=0; i<files.getFileCount(); i++) {
			FileBuffer reads1 = files.getFile(i);
			
			if (!skipSAIGen) {
				submitAlignmentJob((FastQFile)reads1);
			}
			StringPair outputNames = new StringPair();
			outputNames.readsOne = reads1.getAbsolutePath(); 
			outputNames.readsTwo = null;
			outputNames.saiOne = reads1.getAbsolutePath() + ".sai";
			outputNames.saiTwo = null;
			saiFileNames.add(outputNames);
		}
	}
	
	/**
	 * Submit all jobs for a paired-end alignment. This runs bwa aln as usual, but stores 
	 * @param files1
	 * @param files2
	 * @param skipSAIGen
	 */
	protected void runPairedEndAlignment(MultiFileBuffer files1, MultiFileBuffer files2, boolean skipSAIGen) {
		for(int i=0; i<files1.getFileCount(); i++) {
			FileBuffer reads1 = files1.getFile(i);
			FileBuffer reads2 = files2.getFile(i);
			
			if (!skipSAIGen) {
				submitAlignmentJob((FastQFile)reads1);
				submitAlignmentJob((FastQFile)reads2);
			}
			StringPair outputNames = new StringPair();
			outputNames.readsOne = reads1.getAbsolutePath(); 
			outputNames.readsTwo = reads2.getAbsolutePath();
			outputNames.saiOne = reads1.getAbsolutePath() + ".sai";
			outputNames.saiTwo = reads2.getAbsolutePath() + ".sai";
			saiFileNames.add(outputNames);
		}
	}
	
	/**
	 * Create a new AlignerJob and submit it to the threadPool. This returns immediately
	 * @param reads
	 */
	private void submitAlignmentJob(FastQFile reads) {
		AlignerJob task = new AlignerJob(reads);
		threadPool.submit(task);
	}
	
	@Override
	protected String getCommand() throws OperationFailedException {
		//No need to return a command here since we've overridden performOperation to use multiple commands
		return null;
	}

	/**
	 * Run BWA aln as a seperate thread
	 * @author brendan
	 *
	 */
	class AlignerJob implements Runnable {

		protected String defaultRG = "@RG\\tID:unknown\\tSM:unknown\\tPL:ILLUMINA";
		final String command;
		String baseFilename;
		
		public AlignerJob(FileBuffer inputFile) {
			baseFilename = inputFile.getAbsolutePath();
			command = pathToBWA + " aln -n " + maxEditDist + " -t " + threads + " " + referencePath + " " + inputFile.getAbsolutePath();
		}
		
		@Override
		public void run() {
			Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
			try {
					Date begin = new Date();
					logger.info("Beginning task with command : " + command + "\n Threadpool has " + threadPool.getActiveCount() + " active tasks");
					String saiPath = baseFilename + ".sai";
					//System.out.println("Beginning task with command: " + command + " and piping to destination: " + saiPath + "\n Threadpool has " + threadPool.getActiveCount() + " active tasks");
					synchronized (this) {
						FileBuffer pipeDestination = new SAIFile(new File(saiPath));
						runAndCaptureOutput(command, Logger.getLogger(Pipeline.primaryLoggerName), pipeDestination);
						Date end = new Date();
						logger.info("Task with command : " + command + " has completed (elapsed time " + ElapsedTimeFormatter.getElapsedTime(begin.getTime(), end.getTime()) + ") \n Threadpool has " + threadPool.getActiveCount() + " active tasks");
					}
				
			} catch (OperationFailedException e) {
				e.printStackTrace(System.err);
			}
		}
		
	}
	
	
	class SamBuilderJob implements Runnable {

		protected String defaultRG = "@RG\\tID:unknown\\tSM:unknown\\tPL:ILLUMINA";
		final String command;
		String baseFilename;
		
		/**
		 * Constructor for paired-end reads jobs, take two fastq and sai files and make one SAM file
		 * @param readsOne
		 * @param readsTwo
		 * @param saiFileOne
		 * @param saiFileTwo
		 */
		public SamBuilderJob(String readsOne, String readsTwo, String saiFileOne, String saiFileTwo) {
			baseFilename = readsOne;
			command = pathToBWA + " sampe -r " + defaultRG + " " + referencePath + " " + saiFileOne + " " + saiFileTwo + " " + readsOne + " " + readsTwo;
		}
		
		/**
		 * Constructor for SINGLE-END reads, takes one sai file and one fastq file
		 * @param readsOne
		 * @param saiFileOne
		 */
		public SamBuilderJob(String readsOne, String saiFileOne) {
			baseFilename = readsOne;
			command = pathToBWA + " samse -r " + defaultRG + " " + referencePath + " " + saiFileOne + " " + readsOne;
		}
		
		@Override
		public void run() {
			Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
			try {
					Date begin = new Date();
					logger.info("Beginning task with command : " + command + " threadpool has " + threadPool.getActiveCount() + " active tasks");
					String samPath = baseFilename.replace(".sai", "");
					samPath = samPath.replace(".fastq", "");
					samPath = samPath.replace(".gz", "");
					samPath = samPath.replace(".fq", "");
					samPath = samPath.replace(".txt", "");
					samPath = samPath + ".sam";
					FileBuffer pipeDestination = new SAMFile(new File(samPath));
					runAndCaptureOutput(command, Logger.getLogger(Pipeline.primaryLoggerName), pipeDestination);
					Date end = new Date();
					synchronized (this) {
						outputSAMs.addFile(pipeDestination);
						logger.info("Task with command : " + command + " has completed (elapsed time " + ElapsedTimeFormatter.getElapsedTime(begin.getTime(), end.getTime()) + ") \n Threadpool has " + threadPool.getActiveCount() + " active tasks");
					}
					
				
			} catch (OperationFailedException e) {
				e.printStackTrace(System.err);
			}
		}
		
	}
	
	class FilenameSorter implements Comparator<FileBuffer> {

		@Override
		public int compare(FileBuffer o1, FileBuffer o2) {
			return (o1.getAbsolutePath().compareTo(o2.getAbsolutePath()));
		}
		
	}
	
	class StringPair {
		String readsOne;
		String readsTwo;
		String saiOne;
		String saiTwo;
	}

	
}
