package operator.bamutils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;

/**
 * Base class for operations that perform filtering of reads in BAM (or SAM) files
 * @author brendan
 *
 */
public class AbstractBAMFilter extends IOOperator {

	public static final String threads = "threads";
	protected ThreadPoolExecutor threadPool = null;

	protected Integer userThreadCount = null;
	
	protected MultiFileBuffer inputMultiBuffer = null;
	protected List<BAMFile> inputFiles = new ArrayList<BAMFile>();
	protected MultiFileBuffer outputFiles = null;
	protected List<ReadFilter> filters;
	
	public void setFilters(List<ReadFilter> filters) {
		this.filters = filters;
	}
	
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
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (filters == null) {
			throw new OperationFailedException("Filters have not been initialized", this);
		}
		
		scanInputMultiBuffer(); //Read files from input multi buffer and put them in inputFiles list
		
		if (inputFiles.size() ==0 ){
			throw new OperationFailedException("No input files supplied to BAM filter", this);
		}
		
		String threadsStr = properties.get(threads);
		if (threadsStr != null) {
			int threads = Integer.parseInt(threadsStr);
			userThreadCount = threads;
		}
		
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPreferredThreadCount() );
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		List<FilterTask> jobs = new ArrayList<FilterTask>();
		
		for(BAMFile inputBAM : inputFiles) {
			File outputFile = new File(getProjectHome() + "/"+ inputBAM.getFilename().replace(".bam", "").replace(".sam", "") + ".flt.bam");
			logger.info("Filtering reads from BAM file " + inputBAM.getFilename() + " to " + outputFile.getName());
			FilterTask task = new FilterTask(inputBAM, logger, filters, outputFile);
			jobs.add(task);
			threadPool.submit(task);
		}
		
		try {
			logger.info("All tasks have been submitted to BAM Filter " + getObjectLabel() + ", now awaiting termination...");
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(7, TimeUnit.DAYS); //Wait until all tasks have completed
			
			//Check for errors
			boolean allOK = true;
			for(FilterTask job : jobs) {
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
		
	}

	/**
	 * Examines the input multibuffer, if it exists, and adds all files identified to the inputFiles list.
	 */
	private void scanInputMultiBuffer() {
		if (inputMultiBuffer != null) {
			for(int j=0; j<inputMultiBuffer.getFileCount(); j++) {
				FileBuffer buf = inputMultiBuffer.getFile(j);
				if (buf instanceof BAMFile) {
					if (! inputFiles.contains(buf))
						inputFiles.add((BAMFile)buf);
				}
			}
		}
	}
	
	@Override
	public void initialize(NodeList children) {
		String threadsStr = properties.get(threads);
		if (threadsStr != null) {
			int threads = Integer.parseInt(threadsStr);
			userThreadCount = threads;
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
						throw new IllegalArgumentException("Unknown object reference to MultiOperator " + getObjectLabel());
					if (obj instanceof MultiFileBuffer) {
						inputMultiBuffer = (MultiFileBuffer)obj;
					}
					else {
						if (obj instanceof BAMFile) {
							inputFiles.add( (BAMFile)obj);
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
						MultiFileBuffer files = (MultiFileBuffer)obj;
						outputFiles = files;
					}
					else {
						throw new IllegalArgumentException("Output of BAM filter must be a multi-file buffer, found " + obj.getClass() + " instead.");
					}
				}
			}
		}
		
		if (outputFiles == null) {
			throw new IllegalArgumentException("Output of BAM filter must be a multi-file buffer, but nothing was specified.");
		}
		
	}

	
	/**
	 * Little wrapper for commands so they can be executed in a thread pool
	 * @author brendan
	 *
	 */
	public class FilterTask implements Runnable {
		final BAMFile inputFile;
		private Logger logger;
		private boolean isError = false;
		private Exception exception = null;
		private List<ReadFilter> readFilters;
		private File outputDest;
		
		public FilterTask(BAMFile inputFile, Logger logger, List<ReadFilter> filters, File outputDest) {
			this.inputFile = inputFile;
			this.logger = logger;
			this.readFilters = filters;
			this.outputDest = outputDest;
		}
		
		@Override
		public void run() {
			try {
				final SAMFileReader inputSam = new SAMFileReader(inputFile.getFile());
				inputSam.setValidationStringency(ValidationStringency.LENIENT);		

				SAMFileWriterFactory factory = new SAMFileWriterFactory();
				final SAMFileWriter writer = factory.makeBAMWriter(inputSam.getFileHeader(), false, outputDest);
				
				long readCount = 0;
				long passingReads = 0;
				for (final SAMRecord samRecord : inputSam) {
					readCount++;
					boolean passes = true;
					for(ReadFilter filter : readFilters) {
						if (! filter.readPasses(samRecord)) {
							passes = false;
							break;
						}
					}
					
					if (passes) {
						passingReads++;
						writer.addAlignment(samRecord);
					}
				}
				
				logger.info("Filter task passed " + passingReads + " of " + readCount + " total reads.");
				inputSam.close();
				writer.close();
				outputFiles.addFile(new BAMFile(outputDest));
				
			} catch (Exception e) {
				logger.info("Filter task encountered exception: " + e.getLocalizedMessage());
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
