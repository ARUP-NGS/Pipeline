package operator.bwa;

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
 * Uses BWA's fancy new 'mem' algorithm to align. Output is directed into new parallel sorter 
 * for faster sorting, then to samtools for immediate bamification of sorted alignment.
 *  Should be a bit faster.
 * Currently requires paired-end reads and can't handle samples from multiple lanes. 
 *   
 * @author brendan
 *
 */
public class BWAMEMAlign extends IOOperator {
	
	public static final String JVM_ARGS="jvmargs";
	public static final String BWA_PATH = "bwa.mem.path";
	public static final String SAMTOOLS_PATH = "samtools.path";
	public static final String SAMTOOLS_MT_PATH = "samtools-mt.path";
	public static final String EXTRA_OPTIONS="bwa.options";
	protected String extraOptions = "";
	String sample = "unknown";
	String samtoolsPath = null;
	//String samtoolsMTPath = null;
	String bwaPath = null;

	@Override
	public void performOperation() throws OperationFailedException {
		
		
		ReferenceFile refBuf = (ReferenceFile) this.getInputBufferForClass(ReferenceFile.class);
		
		List<FileBuffer> inputBuffers = this.getAllInputBuffersForClass(FastQFile.class);
		
		File outputBAMBuffer = this.getOutputBufferForClass(BAMFile.class).getFile();
		if (outputBAMBuffer == null) {
			throw new OperationFailedException("No output BAM file found", this);
		}
		
		
		String sampleAttr = getAttribute("sample");
		if (sampleAttr != null)
			sample = sampleAttr;
		
		int threads = this.getPipelineOwner().getThreadCount();
		
			
		
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
		
		String secondFastq = "";
		if (inputBuffers.size()>1) {
			secondFastq = inputBuffers.get(1).getAbsolutePath();
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("BWA-MEM is aligning " + inputBuffers.get(0).getFilename() + " " + secondFastq + " with " + threads + " threads");
		
		String command = bwaPath 
				+ " mem "
				+ refBuf.getAbsolutePath() + " "
				+ inputBuffers.get(0).getAbsolutePath() + " "
				+ secondFastq + " "
				+ " -t " + threads
				+ " " + extraOptions
				+ " -R \"@RG\\tID:unknown\\tSM:" + sample + "\\tPL:ILLUMINA\" "
				+ " 2> .bwa.mem.stderr.txt "
				+ " | " + samtoolsPath + " view -S -u -h - | " + samtoolsPath + " sort - " + outputBAMBuffer.getAbsolutePath().replace(".bam", "") + " 2> .smterr.txt ";
					
		executeBASHCommand(command);
	}
	
	private void executeBASHCommand(String command) throws OperationFailedException {
		String filename = this.getProjectHome() + "/bwacommand-" + ((1000000.0*Math.random())+"").substring(0, 6).replace(".", "") + ".sh";
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			writer.write(command + "\n");
			writer.close();
		} catch (IOException e) {
			throw new OperationFailedException("IO Error writing bwa command file : " + e.getMessage(), this);
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
			
			Logger.getLogger(Pipeline.primaryLoggerName).info("BWA-MEM is executing command: " + command);
			int exitVal = proc.waitFor();
			
			if (exitVal != 0) {
				throw new OperationFailedException("BWA-MEM process exited with nonzero status, aborting", this);
			}
		} catch (IOException e) {
			throw new OperationFailedException("Error running BWA-MEM : " + e.getLocalizedMessage(), this);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String pathAttr = this.getAttribute(BWA_PATH);
		if (pathAttr == null) {
			pathAttr = this.getPipelineProperty(BWA_PATH);
		}
		if (pathAttr == null) {
			throw new IllegalArgumentException("No path to BWA found, please specify " + BWA_PATH);
		}
		if (! (new File(pathAttr).exists())) {
			throw new IllegalArgumentException("No file found at BWA path : " + bwaPath);
		}
		this.bwaPath = pathAttr;
		
		
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
		
		String extraAttr = this.getAttribute(EXTRA_OPTIONS);
		if(extraAttr == null) {
			extraAttr = this.getPipelineProperty(EXTRA_OPTIONS);
		}
		
		if(extraAttr != null) {
			this.extraOptions = extraAttr;
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
