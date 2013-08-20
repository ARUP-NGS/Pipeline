package operator.bwa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import util.ElapsedTimeFormatter;
import util.StringOutputStream;
import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;
import buffer.SAIFile;

public class MultiAlignAndBAM extends PipedCommandOp {

	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String SKIPSAI = "skipsai";
	public static final String SINGLE_END = "single";
	public static final String SAMPE_THREADS = "sampe.threads";
	public static final String SEED_LENGTH = "seed.length";
	public static final String SAMPLE = "sample";
	public static final String MAXEDITDIST = "max.edit.dist";
	public static final String MAXGAPOPEN = "max.gap.open";
	public static final String MAXGAPEXT = "max.gap.ext";
	public static final String PENGAPOPEN = "pen.gap.open";
	public static final String PENGAPEXT = "pen.gap.ext";
	
	
	protected int sampeThreads = 1;  //Overridden in performOperation
	protected String maxEditDist; //Maximum edit distance for alignment
	protected String maxGapOpen; //Maximum number of gap opens for alignment, default is [1]
	protected String maxGapExt; //Maximum number of gap extensions, -1 for k-difference mode (disallowing long gaps), default is [-1]
	protected String penGapOpen; //Gap open penalty, default is [11]
	protected String penGapExt; //Gap extension penalty, default is [4]
	protected String pathToBWA = "bwa";
	protected String pathToSamTools = "samtools";
	protected int defaultThreads = 4;
	protected String readGroup = "unknown";
	protected int seedLength = 1000; //Effectively the default
	protected boolean pairedEnd = true; //By default, assume paired ends. SINGLE_END property is queried to set this to false
	protected int threads = defaultThreads;
	protected String referencePath = null;
	protected StringOutputStream errStream = new StringOutputStream();
	

	private List<StringPair> saiFileNames = new ArrayList<StringPair>();
	
	private ThreadPoolExecutor threadPool;
	protected MultiFileBuffer outputSAMs;
	
	public int getPreferredThreadCount() {
		return getPipelineOwner().getThreadCount();
	}

	@Override
	public void performOperation() throws OperationFailedException {
		Date start = new Date();
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.BWA_PATH);
		if (propsPath != null)
			pathToBWA = propsPath.toString();
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
		
		
		String samToolsPathAttr = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samToolsPathAttr != null) {
			pathToSamTools = samToolsPathAttr;
		}
		
		String threadsAttr = properties.get(THREADS);
		if (threadsAttr != null) {
			threads = Integer.parseInt(threadsAttr);
		}
		
		String maxEditDistAttr = properties.get(MAXEDITDIST);
		if(maxEditDistAttr != null){
			maxEditDist = maxEditDistAttr;
		}
		else{
			maxEditDist = null;
		}

		String maxGapOpenAttr = properties.get(MAXGAPOPEN);
		if(maxGapOpenAttr != null){
			maxGapOpen = maxGapOpenAttr;
		}
		else{
			maxGapOpen = null;
		}

		String maxGapExtAttr = properties.get(MAXGAPEXT);
		if(maxGapExtAttr != null){
			maxGapExt = maxGapExtAttr;
		}
		else{
			maxGapExt = null;
		}

		String penGapOpenAttr = properties.get(PENGAPOPEN);
		if(penGapOpenAttr != null){
			penGapOpen = penGapOpenAttr;
		}
		else{
			penGapOpen = null;
		}

		String penGapExtAttr = properties.get(PENGAPEXT);
		if(penGapExtAttr != null){
			penGapExt = penGapExtAttr;
		}
		else{
			penGapExt = null;
		}
		
		String singleEndAttr = properties.get(SINGLE_END);
		if (singleEndAttr != null) {
			pairedEnd = ! Boolean.parseBoolean(singleEndAttr);
		}
		if (pairedEnd)
			logger.info("Multi-lane aligner is using PAIRED-END mode");
		else
			logger.info("Multi-lane aligner is using SINGLE-END mode");
		
		sampeThreads = getPipelineOwner().getThreadCount();
		String sampeThreadStr = properties.get(SAMPE_THREADS);
		if (sampeThreadStr != null) {
			sampeThreads = Integer.parseInt(sampeThreadStr);
		}
			
		String seedLengthStr = properties.get(SEED_LENGTH);
		if (seedLengthStr != null) {
			seedLength = Integer.parseInt(seedLengthStr);
			logger.info("Using seed length : " + seedLength);	
		}
		
		String rgStr = properties.get(SAMPLE);
		if (rgStr != null) 
			readGroup = rgStr;
		logger.info("Using sample name : " + rgStr);
		
		
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
		
		//If additional fastq's are listed, add them to the multifilebuffers in FileBuffer
		if (this.getAllInputBuffersForClass(FastQFile.class).size() > 0) {
			List<FileBuffer> fqs = this.getAllInputBuffersForClass(FastQFile.class);
			
			//If no MultifileBuffers provided we need to make them
			if (files.size()==0 && pairedEnd) {
				files.add( new MultiFileBuffer());
				files.add( new MultiFileBuffer());
			}
			else {
				files.add( new MultiFileBuffer());
			}
			
			int index = 0;
			for(FileBuffer fq : fqs) {
				((MultiFileBuffer)files.get(index % files.size())).addFile(fq);
				index++;
			}
			
			
		}
		
		
		
		if (files.size() != 2 && pairedEnd) {
			throw new OperationFailedException("Need exactly two input files of type MultiFileBuffer for paired-end mode", this);
		}
		
		MultiFileBuffer files1 = (MultiFileBuffer) files.get(0);
		MultiFileBuffer files2 = null; //May be null if we're doing single-end. If single-end and non-null, treated exactly same way as files1
		if (files.size()>1)
			files2 = (MultiFileBuffer) files.get(1);
		
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
					throw new OperationFailedException("Paired files are the same file! File is : " + files1.getFile(i).getAbsolutePath(), this);
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

		
		//These are done in serial since bwa can parallelize itself
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( Math.max(1, getPreferredThreadCount()/threads) );
		
		if (pairedEnd) {
			runPairedEndAlignment(files1, files2, skipSAIGen);
		}
		else {
			runSingleEndAlignment(files1, skipSAIGen);
			if (files2 != null)
				runSingleEndAlignment(files2, skipSAIGen);
		}
		
		
		if (!skipSAIGen) {
			try {
				logger.info("All alignment jobs have been submitted to MultiLaneAligner, " + getObjectLabel() + ", now awaiting termination");
				threadPool.shutdown(); //No new tasks will be submitted,
				threadPool.awaitTermination(2, TimeUnit.DAYS); //Wait until all tasks have completed			
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			logger.info("Skipping .sai generation, no jobs submitted to pool for this task");
		}
		
		logger.info("All bwa aln steps have completed, now creating SAM files");
		if (sampeThreads > 1)
			threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPreferredThreadCount()/ sampeThreads );
		else
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
			String projHome = this.getProjectHome();
			StringPair outputNames = new StringPair();
			outputNames.readsOne = reads1.getAbsolutePath(); 
			outputNames.readsTwo = null;
			outputNames.saiOne = projHome + "/" + reads1.getFilename() + ".sai";
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
			
			String projHome = this.getProjectHome();
			outputNames.saiOne = projHome + "/" + reads1.getFilename() + ".sai";
			outputNames.saiTwo = projHome + "/" + reads2.getFilename() + ".sai";
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
	
	/**
	 * Run BWA aln as a separate thread
	 * @author brendan
	 *
	 */
	class AlignerJob implements Runnable {

		//protected String defaultRG = "@RG\\tID:unknown\\tSM:unknown\\tPL:ILLUMINA";
		final String command;
		String baseFilename;
		
		public AlignerJob(FileBuffer inputFile) {
			String temp="";
			if(maxGapOpen!= null) temp=temp+ " -o " + maxGapOpen;
			if(maxGapExt!= null) temp=temp+ " -o " + maxGapExt;
			if(penGapOpen!= null) temp=temp+ " -o " + penGapOpen;
			if(penGapExt!= null) temp=temp+ " -o " + penGapExt;
			
			baseFilename = inputFile.getFilename();
			if(maxEditDist != null){
				command = pathToBWA + " aln "+ temp+" -n " + maxEditDist + " -t " + Math.max(1, threads) + " " + referencePath + " " + inputFile.getAbsolutePath();
			}
			else{
				command = pathToBWA + " aln "+ temp+" -t " + Math.max(1, threads) + " " + referencePath + " " + inputFile.getAbsolutePath();
			}
		}
		
		@Override
		public void run() {
			Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
			try {
					Date begin = new Date();
					//logger.info("Beginning task with command : " + command + "\n Threadpool has " + threadPool.getActiveCount() + " active tasks");
					String saiPath = getProjectHome() + baseFilename + ".sai";
					System.out.println("Beginning task with command: " + command + " and piping to destination: " + saiPath + "\n Threadpool has " + threadPool.getActiveCount() + " active tasks");
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

		protected String defaultRG = "@RG\\tID:unknown\\tSM:$SAMPLE$\\tPL:ILLUMINA";
		final String command1;
		//final String command2;
		String baseFilename;
		String bamPath = null;
		
		/**
		 * Constructor for paired-end reads jobs, take two fastq and sai files and make one SAM file
		 * @param readsOne
		 * @param readsTwo
		 * @param saiFileOne
		 * @param saiFileTwo
		 */
		public SamBuilderJob(String readsOne, String readsTwo, String saiFileOne, String saiFileTwo) {
			baseFilename = saiFileOne;
			String threadsStr = "";
			if (sampeThreads > 1) {
				threadsStr = " -t " + sampeThreads;
			}
				
			String rgStr = defaultRG.replace("$SAMPLE$", SAMPLE);
			
			bamPath = baseFilename.replace(".sai", "");
			bamPath = bamPath.replace(".fastq", "");
			bamPath = bamPath.replace(".gz", "");
			bamPath = bamPath.replace(".fq", "");
			bamPath = bamPath.replace(".txt", "");
			bamPath = bamPath + ".bam";
			
			
			String comStr = pathToBWA + " sampe -r \"" + rgStr + "\" " + referencePath + " " + threadsStr + " " + saiFileOne + " " + saiFileTwo + " " + readsOne + " " + readsTwo + 
								" | " +  pathToSamTools + " view -Sb - > " + bamPath;
			String fileName = getProjectHome() + "sambuilder-" + ((1000000.0*Math.random())+"").substring(0, 6).replace(".", "") + ".sh";
			
			//System.out.println("Writing command string : " + comStr);
			//System.out.println("To file : " + fileName);
			BufferedWriter writer;
			try {
				writer = new BufferedWriter(new FileWriter(fileName));
				writer.write(comStr + "\n");
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			command1 = "/bin/bash " + fileName;
		}
		
		/**
		 * Constructor for SINGLE-END reads, takes one sai file and one fastq file
		 * @param readsOne
		 * @param saiFileOne
		 */
		public SamBuilderJob(String readsOne, String saiFileOne) {
			baseFilename = saiFileOne;
			baseFilename = saiFileOne;
			String threadsStr = "";
			if (sampeThreads > 1) {
				threadsStr = " -t " + sampeThreads;
			}
				
			String rgStr = defaultRG.replace("$SAMPLE$", SAMPLE);
			
			bamPath = baseFilename.replace(".sai", "");
			bamPath = bamPath.replace(".fastq", "");
			bamPath = bamPath.replace(".gz", "");
			bamPath = bamPath.replace(".fq", "");
			bamPath = bamPath.replace(".txt", "");
			bamPath = bamPath + ".bam";
			
			String comStr = pathToBWA + " samse -r \"" + rgStr + "\" " + referencePath + " " + threadsStr + " " + saiFileOne + " " + readsOne + 
								" | " +  pathToSamTools + " view -Sb - > " + bamPath;
			String fileName = getProjectHome() + "sambuilder-" + ((1000000.0*Math.random())+"").substring(0, 6).replace(".", "") + ".sh";
			
			System.out.println("Writing command string : " + comStr);
			System.out.println("To file : " + fileName);
			BufferedWriter writer;
			try {
				writer = new BufferedWriter(new FileWriter(fileName));
				writer.write(comStr + "\n");
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//command1 = pathToBWA + " samse -r " + defaultRG + " " + referencePath + " " + saiFileOne + " " + readsOne;
			command1 = "/bin/bash " + fileName;
		}
		
		@Override
		public void run() {
			Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
			try {
					Date begin = new Date();
					logger.info("Beginning task with command : " + command1 + " threadpool has " + threadPool.getActiveCount() + " active tasks");
					
					FileBuffer pipeDestination = new BAMFile(new File(bamPath));
					runAndCaptureOutput(command1, Logger.getLogger(Pipeline.primaryLoggerName), pipeDestination);
					Date end = new Date();
					synchronized (this) {
						outputSAMs.addFile(pipeDestination);
						logger.info("Task with command : " + command1 + " has completed (elapsed time " + ElapsedTimeFormatter.getElapsedTime(begin.getTime(), end.getTime()) + ") \n Threadpool has " + threadPool.getActiveCount() + " active tasks");
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

	@Override
	protected String getCommand() throws OperationFailedException {
		// TODO Auto-generated method stub
		return null;
	}
}
