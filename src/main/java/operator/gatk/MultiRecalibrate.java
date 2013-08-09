package operator.gatk;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import operator.MultiOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import util.ElapsedTimeFormatter;
import buffer.BAMFile;
import buffer.CSVFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

public class MultiRecalibrate extends MultiOperator {

	public final String defaultMemOptions = "-Xmx4g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	public void performOperation() throws OperationFailedException {
		
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPreferredThreadCount() );
		
		Date start = new Date();
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		if (inputFiles == null) {
			throw new OperationFailedException("InputFiles buffer has not been initialized for MultiOperator " + getObjectLabel(), this);
		}
		logger.info("Beginning parallel multi-operation " + getObjectLabel() + " with " + inputFiles.getFileCount() + " input files");
		
		List<TaskOperator> jobs = new ArrayList<TaskOperator>();
		
		List<BAMFile> inputBAMs = new ArrayList<BAMFile>();
		List<CSVFile> recalDataFiles = new ArrayList<CSVFile>(); //Stores recalibration data
		for(int i=0; i<inputFiles.getFileCount(); i++) {
			if (inputFiles.getFile(i) instanceof BAMFile) {
				inputBAMs.add( (BAMFile) inputFiles.getFile(i) );
				String recalFileName = inputFiles.getFile(i).getAbsolutePath().replace(".bam", ".recal.csv");
				recalDataFiles.add(new CSVFile(new File(recalFileName)));
			}
		}
		
		if (inputBAMs.size()==0)
			throw new OperationFailedException("Did not find any BAM files for input to MultiRecalibrate", this);
		
		for(int i=0; i<inputBAMs.size(); i++) {
			FileBuffer inputBuffer = inputBAMs.get(i);
			String command[] = getFullCommand(inputBuffer, recalDataFiles.get(i));
			logger.info("Submitting task with command : " + command[0]);
			if (command != null) {
				TaskOperator task = new TaskOperator(command, logger);
				jobs.add(task);
				threadPool.submit(task);
			}
		}
		
		try {
			logger.info("All count-covariate tasks have been submitted to multioperator " + getObjectLabel() + ", now awaiting termination...");
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(7, TimeUnit.DAYS); //Wait until all tasks have completed
			//threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPreferredThreadCount() );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		try {
			logger.info("All tasks have been submitted to multioperator " + getObjectLabel() + ", now awaiting termination...");
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(7, TimeUnit.DAYS); //Wait until all tasks have completed
			
			//Check for errors
			boolean allOK = true;
			for(TaskOperator job : jobs) {
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
	
		Date end = new Date();
		logger.info("Parallel multi-operation " + getObjectLabel() + " has completed (Total time " + ElapsedTimeFormatter.getElapsedTime(start.getTime(), end.getTime()) + " )");

	}
	

	/**
	 * Obtain an array with both commands we want to execute (count covariates & apply recal)
	 * This replaces getCommand(...) since we need to pass in the csv file as well 
	 * @param inputBuffer
	 * @param csvFile
	 * @return
	 */
	private String[] getFullCommand(FileBuffer inputBuffer, CSVFile csvFile) {
		return new String[]{ getCountCovarCommand(inputBuffer, csvFile), getApplyRecalCommand(inputBuffer, csvFile) };
	}

	private String getApplyRecalCommand(FileBuffer inputBuffer, CSVFile recalDataFile) {
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) getPipelineProperty(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
		if (!jvmARGStr.contains("java.io.tmpdir"))
			jvmARGStr =jvmARGStr + " -Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir");
		
		String newPath = inputBuffer.getAbsolutePath().replace(".bam", ".recal.bam");
		BAMFile recalBamFile = new BAMFile(new File(newPath), inputBuffer.getContig());
		System.out.println("Adding file : " + recalBamFile.getAbsolutePath() + " to recal output bams");
		addOutputFile(recalBamFile);
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference.getAbsolutePath() + 
				" -I " + inputBuffer.getAbsolutePath() + 
				" -T TableRecalibration " + 
				" -o " + recalBamFile.getAbsolutePath() + 
				" -recalFile " + recalDataFile.getAbsolutePath();
		if (inputBuffer.getContig() != null) {
				command = command + " -L " + inputBuffer.getContig();
		}
		return command;
	}

	protected String getCountCovarCommand(FileBuffer inputBuffer, CSVFile recalDataFile) {
		if (inputBuffer instanceof VCFFile || inputBuffer instanceof ReferenceFile || inputBuffer instanceof CSVFile) {
			return null;
		}
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) getPipelineProperty(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
		
		String referencePath = reference.getAbsolutePath();
		List<FileBuffer> knownSitesFiles = getAllInputBuffersForClass(VCFFile.class);
		
		StringBuffer knownSitesStr = new StringBuffer();
		for(FileBuffer buff : knownSitesFiles) {
			knownSitesStr.append("-knownSites " + buff.getAbsolutePath() + " ");
		}
		
		if (knownSitesFiles.size() == 0) {
			throw new IllegalArgumentException("You must list some known sites files for CountCovariates to work");
		}
		
		
		String covariateList = "-cov QualityScoreCovariate -cov DinucCovariate -cov MappingQualityCovariate -cov HomopolymerCovariate ";
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + referencePath + 
				" -I " + inputBuffer.getAbsolutePath() + 
				" -T CountCovariates " + 
				covariateList + " "	+ 
				knownSitesStr.toString() +
				" -recalFile " + recalDataFile.getAbsolutePath();
		if (inputBuffer.getContig() != null) {
			command = command + " -L " + inputBuffer.getContig();
		}
		return command;
	}

	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
		//Blank on purpose since we've overridden performOperation
		return null;
	}


}
