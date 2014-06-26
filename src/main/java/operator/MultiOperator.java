package operator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.ElapsedTimeFormatter;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;

/**
 * This operator only recognizes a single, MultiFileBuffer as an input and output argument, and runs
 * the same operation on each input file and adds the result to the output multi file buffer  
 * @author brendan
 *
 */
public abstract class MultiOperator extends IOOperator {

	protected MultiFileBuffer inputFiles;
	protected MultiFileBuffer outputFiles;
	protected ReferenceFile reference;
	protected ThreadPoolExecutor threadPool = null;

	protected Integer userThreadCount = null;
	
	public static final String threads = "threads";
	public static final String checkcontigs = "checkcontigs";
	
	//If true, make sure that all contigs (except y) are present in input and output files
	protected boolean checkContigs = false;
	
	public MultiOperator() {
		
	}
	
	/**
	 * Adds a new file to the list of output files. Under normal use getCommand() should call this
	 * to add the appropriate file to the list of output files
	 */
	protected synchronized void addOutputFile(FileBuffer outputFile) {
		outputFiles.addFile(outputFile);
	}
	
	protected abstract String[] getCommand(FileBuffer inputBuffer) throws OperationFailedException;
	
	/**
	 * Return number of threads to use in pool. This is Pipeline.getThreadCount()
	 * unless the user has specified a threads="x" argument to this operator, in which
	 * case it's x
	 * @return
	 */
	public int getPreferredThreadCount() {
		if (userThreadCount != null)
			return userThreadCount;
		else
			return getPipelineOwner().getThreadCount();
	}
	
	/**
	 * Traverse through input files and see if we have all of the contigs (Y is optional) 
	 */
	protected void checkInputContigs() {
		checkContigs(inputFiles.getFileList());
	}
	
	protected void checkOutputContigs() {
		checkContigs(outputFiles.getFileList());
	}
	
	
	@Override
	public void performOperation() throws OperationFailedException {
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPreferredThreadCount() );
		
		Date start = new Date();
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		if (inputFiles == null) {
			throw new OperationFailedException("InputFiles buffer has not been initialized for MultiOperator " + getObjectLabel(), this);
		}
		if (inputFiles.getFileCount()==0) {
			throw new OperationFailedException("No input files specified to operator " + getObjectLabel() + ", aborting. ", this);
		}
		
		logger.info("Beginning parallel multi-operation " + getObjectLabel() + " with " + inputFiles.getFileCount() + " input files");
		if (inputFiles != null && checkContigs) {
			checkInputContigs();
		}
		
		List<TaskOperator> jobs = new ArrayList<TaskOperator>();
		
		for(int i=0; i<inputFiles.getFileCount(); i++) {
			FileBuffer inputBuffer = inputFiles.getFile(i);
			String command[] = getCommand(inputBuffer);
			if (command != null) {
				TaskOperator task = new TaskOperator(command, logger);
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

		
		if (outputFiles != null && checkContigs) {
			checkOutputContigs();
		}
		if (inputFiles != null && outputFiles != null && inputFiles.getFileCount() != outputFiles.getFileCount()) {
			logger.severe("Uh oh, we didn't find the name number of input as output files! input files size: " + inputFiles.getFileCount() + " output files: " + outputFiles.getFileCount());
		}
		
		Date end = new Date();
		logger.info("Parallel multi-operation " + getObjectLabel() + " has completed (Total time " + ElapsedTimeFormatter.getElapsedTime(start.getTime(), end.getTime()) + " )");

	}

	
	@Override
	public void initialize(NodeList children) {
		String threadsStr = properties.get(threads);
		if (threadsStr != null) {
			int threads = Integer.parseInt(threadsStr);
			userThreadCount = threads;
		}
		
		String checkStr = properties.get(checkcontigs);
		if (checkStr != null) {
			Boolean check = Boolean.parseBoolean(checkStr);
			checkContigs = check;
			Logger.getLogger(Pipeline.primaryLoggerName).info("Check contig is " + checkContigs + " for MultiOperator " + getObjectLabel());
		}
		
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		if (inputList != null) {
			NodeList inputChildren = inputList.getChildNodes();
			for(int i=0; i<inputChildren.getLength(); i++) {
				Node iChild = inputChildren.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj == null)
						throw new IllegalArgumentException("Unknown object reference to MultiOperator " + getObjectLabel() + " Unknown object is: " + iChild.getNodeName());
					if (obj instanceof MultiFileBuffer) {
						inputFiles = (MultiFileBuffer)obj;

					}
					else {
						//Potentially error-prone special case here.. if obj is a ReferenceFile, set it to reference
						//Otherwise, add it to inputBuffer list. 
						if (obj instanceof ReferenceFile) {
							reference = (ReferenceFile) obj;
						}
						else {
							
							if (obj instanceof FileBuffer) {
								addInputBuffer( (FileBuffer)obj);
							}
						}
					}
				}
		}
	}
		
		if (outputList != null) {
			NodeList outputChilden = outputList.getChildNodes();
			for(int i=0; i<outputChilden.getLength(); i++) {
				Node iChild = outputChilden.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj == null)
						throw new IllegalArgumentException("Unknown object reference to MultiOperator " + getObjectLabel());
					if (obj instanceof MultiFileBuffer) {
						outputFiles = (MultiFileBuffer)obj;
					}
					else {
						throw new IllegalArgumentException("Found non-MultiFileBuffer object in output list for Operator " + getObjectLabel());
					}
				}
			}
		}
		
		if (inputFiles == null) {
			Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
			logger.warning("No MultiFile found for input to MultiOperator " + getObjectLabel());
			if (inputBuffers.size()==0 || (inputBuffers.size()==1 && reference != null)) {
				logger.severe("Also, no file buffers as input either. This is probably an error.");
				throw new IllegalArgumentException("No input buffers found for multi-operator " + getObjectLabel());
			}
		}
	}
	

	
	/**
	 * Little wrapper for commands so they can be executed in a thread pool
	 * @author brendan
	 *
	 */
	public class TaskOperator implements Runnable {

		final String[] command;
		Logger logger;
		boolean isError = false;
		Exception exception = null;
		
		public TaskOperator(String[] command, Logger logger) {
			this.command = command;
			this.logger = logger;
		}
		
		@Override
		public void run() {
			try {
				for (int i=0; i<command.length; i++) {
					Date begin = new Date();
					logger.info("Beginning task with command : " + command[i] + "\n Total tasks: " + threadPool.getTaskCount() + "\n Active tasks: " + threadPool.getActiveCount() + "\n Completed tasks: " + threadPool.getCompletedTaskCount() + "\n Pool size: " + threadPool.getCorePoolSize());
					executeCommand(command[i]);
					Date end = new Date();
					
					logger.info("Task with command : " + command[i] + " has completed (elapsed time " + ElapsedTimeFormatter.getElapsedTime(begin.getTime(), end.getTime()) + ")\n Total tasks: " + threadPool.getTaskCount() + "\n Active tasks: " + threadPool.getActiveCount() + "\n Completed tasks: " + threadPool.getCompletedTaskCount() + "\n Pool size: " + threadPool.getCorePoolSize());
				}
				
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
