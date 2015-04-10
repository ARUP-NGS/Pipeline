package operator.gatk;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.GlobFileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;

/**
 * Splits a single input BAM file into contigs based on chromosome and 
 * emits the result as a MultiFileBuffer
 * @author brendan
 *
 */
public class SplitByChromosome extends IOOperator {

	protected BAMFile inputBam;
	protected ThreadPoolExecutor threadPool = null;
	public static final String JVM_ARGS="jvmargs";
	public static final String PATH = "path";
	public static final String CHECKCONTIGS="check";
	public static final String CHROMOSOMES = "chromosomes";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected String jvmARGStr = "";
	protected String referencePath = null;
	protected MultiFileBuffer multiBAM;
	protected MultiFileBuffer outputFiles;
	protected boolean checkChrs = true;
	//Default list of chromosomes to split out, which is all of them
	static final String[] defaultChrs = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y"}; 
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(getPipelineOwner().getThreadCount());
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning splitting operation for operator " + getObjectLabel());		
		
		referencePath = ((ReferenceFile) getInputBufferForClass(ReferenceFile.class)).getAbsolutePath();
		inputBam = (BAMFile) getInputBufferForClass(BAMFile.class);
		
		if (inputBam == null)
			throw new OperationFailedException("No input BAM file found", this);
		
		outputFiles = (MultiFileBuffer) getOutputBufferForClass(MultiFileBuffer.class);
		if (outputFiles instanceof GlobFileBuffer) {
			((GlobFileBuffer)outputFiles).findFiles();
		}
		
		String check = this.getAttribute(CHECKCONTIGS);
		if (check != null) {
			checkChrs = Boolean.parseBoolean(check);
		}
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String[] chromsToMake = parseChromosomes();		
		if (chromsToMake == null) {
			chromsToMake = defaultChrs;
		}
		else {
			StringBuilder strB = new StringBuilder("Extracting contigs : ");
			for(int i=0; i<chromsToMake.length; i++)
				strB.append(chromsToMake[i] + ", ");
			logger.info(strB.toString());
		}
		
		//Additional args for jvm
		jvmARGStr = getPipelineProperty(JVM_ARGS);
		logger.info(getObjectLabel() + " found jvm argument : " + jvmARGStr);
		
		//If it's still null then be sure to make it the empty string
    	if (jvmARGStr == null || jvmARGStr.length()==0) {
		jvmARGStr = "";
		}

		//Submit all jobs to the thread pool
		for(int i=0; i<chromsToMake.length; i++) {
			String contig = chromsToMake[i];
			Split job = new Split(contig);
			Logger.getLogger(Pipeline.primaryLoggerName).info("Split operator is submitting split job for chromosome " + contig);		
			threadPool.submit(job);
		}

		try {
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(96, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (chromsToMake == defaultChrs && checkChrs)
			checkContigs(outputFiles.getFileList()); //Ensure all contigs have been created
		logger.info("Done with splitting operator " + getObjectLabel());		
	}
	
	/**
	 * Attempt to read and parse the "chromosomes" property, which specifies exactly which
	 * chromosomes to split out. We look for a comma-separated list of chrs, such as 
	 *   chromosomes="1,2,3,X,Y"
	 * 
	 * @return
	 */
	private String[] parseChromosomes() {
		String chrStr = properties.get(CHROMOSOMES);
		if (chrStr == null || chrStr.length()==0) {
			return null;
		}
		
		String[] toks = chrStr.split(",");
		for(int i=0; i<toks.length; i++) {
			toks[i] = toks[i].trim();
		}
		return toks;
	}

	protected void addOutputFile(FileBuffer outputFile) {
		outputFiles.addFile(outputFile);
	}
	
	
	public class Split implements Runnable {

		final String contig;
		
		public Split(String contig) {
			this.contig = contig;
		}
		
		@Override
		public void run() {
			try {

				Logger.getLogger(Pipeline.primaryLoggerName).info("Split operator is running for contig " + contig);
				String inputPath = inputBam.getAbsolutePath();
				int index = inputPath.lastIndexOf(".");
				String prefix = inputPath;
				if (index>0)
					prefix = inputPath.substring(0, index);
				String outputPath = prefix + ".c" + contig + ".bam";
				String command = "java -Xmx4g " + jvmARGStr + " -jar " + gatkPath + 
						" -R " + referencePath + 
						" -I " + inputBam.getAbsolutePath() + 
						" -T PrintReads " +
						" -o " + outputPath +
						" -L " + contig;

				System.out.println("Running command: "+ command);

				Logger.getLogger(Pipeline.primaryLoggerName).info("Split operator is executing command " + command);		
				executeCommand(command);
				addOutputFile(new BAMFile(new File(outputPath), contig));

			} catch (OperationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
