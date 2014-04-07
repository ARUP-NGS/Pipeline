package operator.bwt2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/**
 * Uses Bowtie2's fancy new '2' algorithm to align. 
 * 
 * 
 *
 *   
 * @author Daniel
 * Heavily stealing^H^H^H^H^H^H^H^Hborrowing from brendan
 */
public class Bowtie2Align extends IOOperator {
	
	public static final String JVM_ARGS="jvmargs";
	public static final String BOWTIE2_DIR = "bowtie2.dir";
	public static final String SAMTOOLS_PATH = "samtools.path";
	public static final String BOWTIE2_STYLE = "bowtie2.style";
	public static final String BOWTIE2_SUBSTYLE = "bowtie2.substyle";
	public static final String SCORE_MIN_FN = "score.min.fn";
	public static final String SCORE_MIN_K1 = "score.min.k1";
	public static final String SCORE_MIN_K2 = "score.min.k2";
	
	String sample = "unknown";
	String samtoolsPath = null;
//	String streamsortPath = null;
	String bowtie2Path = null;
	String bowtie2Style = "--local"; //If not set, defaults to --end-to-end
	String bowtie2Substyle = "--sensitive-local"; //If not set, defaults to --sensitive or --sensitive-local, depending on bowtie2Style
	String allowedSeedMismatches = "0"; //Default value. Setting to 1 allows for higher sensitivity at great expense of speed
	String scoreMin = null; //for scoreMin to be set to filter reads below a certain score, it uses a function
	String scoreMinFn = null; //F,K1,K2
	String scoreMinK1 = null; //for min = K1 + K2*F(L). where F is the function (L linear, G nlog, S sqrt, C constant) of length L
	String scoreMinK2 = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		
		ReferenceFile refBuf = (ReferenceFile) this.getInputBufferForClass(ReferenceFile.class);
		
		List<FileBuffer> inputBuffers = this.getAllInputBuffersForClass(FastQFile.class);
		
		FileBuffer outputBAMBuffer = this.getOutputBufferForClass(BAMFile.class);
		if (outputBAMBuffer == null) {
			throw new OperationFailedException("No output BAM file found", this);
		}
		
		if (inputBuffers.size() != 2) {
			throw new OperationFailedException("Exactly two fastq files must be provided to this aligner, found " + inputBuffers.size(), this);
		}
		
		String sampleAttr = getAttribute("sample");
		if (sampleAttr != null)
			sample = sampleAttr;
		
		int threads = this.getPipelineOwner().getThreadCount();
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Bowtie2 is aligning " + inputBuffers.get(0).getFilename() + " and " + inputBuffers.get(1).getFilename() + " with " + threads + " threads");
		
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
		
		// This is where the command is made
		// Note that -p  has been substituted for -t
		String command = bowtie2Path 
				+ " -x "
				+ refBuf.getAbsolutePath() + " -1 "
				+ inputBuffers.get(0).getAbsolutePath() + " -2 "
				+ inputBuffers.get(1).getAbsolutePath()
				+ " -N " + allowedSeedMismatches
				+ " " +bowtie2Style + " " + bowtie2Substyle + " " +scoreMin
				+ " --rg-id \"@RG\\tID:unknown\\tSM:" + sample + "\\tPL:ILLUMINA\" "
				+ " 2> .bowtie2.stderr.txt "
				+ " | " + samtoolsPath + " view -S -u -h - | " + samtoolsPath + " sort - " + outputBAMBuffer.getAbsolutePath().replace(".bam", "") + " 2> .smterr.txt ";
					
		//String myCommand = "Do whatever I say";
		executeBASHCommand(command);
		
		//executeBASHCommand(command); // and this is where it is executed, calling the function below
	}
	
	private void executeBASHCommand(String command) throws OperationFailedException {
		String filename = this.getProjectHome() + "/bowtie2command-" + ((1000000.0*Math.random())+"").substring(0, 6).replace(".", "") + ".sh";
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			writer.write(command + "\n");
			writer.close();
		} catch (IOException e) {
			throw new OperationFailedException("IO Error writing bowtie2 command file : " + e.getMessage(), this);
		}
		
		
		ProcessBuilder procBuilder = new ProcessBuilder("/bin/bash", filename);
		try {
			final Process proc = procBuilder.start();
			
			//If runtime is going down, destroy the process so it won't become orphaned
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					proc.destroy();
				}
			});
			
			Logger.getLogger(Pipeline.primaryLoggerName).info("Bowtie2 is executing command: " + command);
			int exitVal = proc.waitFor();
			
			if (exitVal != 0) {
				throw new OperationFailedException("Bowtie2 process exited with nonzero status, aborting", this);
			}
		} catch (IOException e) {
			throw new OperationFailedException("Error running Bowtie2 : " + e.getLocalizedMessage(), this);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String pathAttr = this.getAttribute(BOWTIE2_DIR);
		if (pathAttr == null) {
			pathAttr = this.getPipelineProperty(BOWTIE2_DIR);
		}
		if (pathAttr == null) {
			throw new IllegalArgumentException("No path to Bowtie2 found, please specify " + BOWTIE2_DIR);
		}
		if (! (new File(pathAttr + "bowtie2").exists())) {
			throw new IllegalArgumentException("No file found at Bowtie2 path : " + pathAttr + "bowtie2");
		}
		this.bowtie2Path = (pathAttr + "/bowtie2");
		
		String samtoolsAttr = this.getAttribute(SAMTOOLS_PATH);
		if (samtoolsAttr == null) {
			samtoolsAttr = this.getPipelineProperty(SAMTOOLS_PATH);
		}
		if (samtoolsAttr == null) {
			throw new IllegalArgumentException("No path to samtools found, please specify " + SAMTOOLS_PATH);
		}
		if (! (new File(samtoolsAttr).exists())) {
			throw new IllegalArgumentException("No file found at samtools path : " + samtoolsAttr);
		}
		this.samtoolsPath = samtoolsAttr;
		
		String bowtie2StyleAttr = this.getAttribute(BOWTIE2_STYLE);
		if(bowtie2StyleAttr == null) {
			bowtie2StyleAttr = this.getPipelineProperty(BOWTIE2_STYLE);
		}
		if(bowtie2StyleAttr != null) {
			this.bowtie2Style = bowtie2StyleAttr;
		}
		
		String bowtie2SubstyleAttr = this.getAttribute(BOWTIE2_SUBSTYLE);
		if(bowtie2SubstyleAttr == null) {
			bowtie2SubstyleAttr = this.getPipelineProperty(BOWTIE2_SUBSTYLE);
		}
		if(bowtie2SubstyleAttr != null) {
			this.bowtie2Substyle = bowtie2SubstyleAttr;
		}
		
		String scoreMinFnAttr = this.getAttribute(SCORE_MIN_FN);
		if(scoreMinFnAttr == null) {
			scoreMinFnAttr = this.getPipelineProperty(SCORE_MIN_FN);
		}
		if(scoreMinFnAttr != null) {
			this.scoreMinFn = scoreMinFnAttr;
		}
		
		String scoreMinK1Attr = this.getAttribute(SCORE_MIN_K1);
		if(scoreMinK1Attr == null) {
			scoreMinK1Attr = this.getPipelineProperty(SCORE_MIN_K1);
		}
		if(scoreMinK1Attr != null) {
			this.scoreMinK1 = scoreMinK1Attr;
		}
		
		String scoreMinK2Attr = this.getAttribute(SCORE_MIN_K2);
		if(scoreMinK2Attr == null) {
			scoreMinK2Attr = this.getPipelineProperty(SCORE_MIN_K2);
		}
		if(scoreMinK2Attr != null) {
			this.scoreMinK2 = scoreMinK2Attr;
		}
		
		if(this.scoreMinFn == null || this.scoreMinK1 == null || this.scoreMinK2 == null ) {
			this.scoreMinFn = null;
			this.scoreMinK1 = null;
			this.scoreMinK2 = null;
		}	else {
			this.scoreMin = this.scoreMinFn + "," + this.scoreMinK1 + "," + this.scoreMinK2;
		}
			
		
//		String samtoolsMTAttr = this.getAttribute(SAMTOOLS_MT_PATH);
//		if (samtoolsMTAttr == null) {
//			samtoolsMTAttr = this.getPipelineProperty(SAMTOOLS_MT_PATH);
//		}
//		if (samtoolsMTAttr == null) {
//			throw new IllegalArgumentException("No path to multithreaded samtools found, please specify " + SAMTOOLS_MT_PATH);
//		}
//
//		this.samtoolsMTPath = samtoolsMTAttr;
	}

}
