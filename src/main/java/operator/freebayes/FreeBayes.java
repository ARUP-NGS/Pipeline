package operator.freebayes;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

/**
 * Uses Boston College's awesome FreeBayes
 * Requires a sorted, indexed .bam file
 * Optional: BED file (to limit algorithm to regions of interest), stringency settings (see public static final String section)
 * Default settings for discovery have been chosen to be moderately stringent. Please check before running.
 *
 *  
 * @author daniel
 * 
 */

public class FreeBayes extends IOOperator {
		
	public static final String JVM_ARGS="jvmargs";
	public static final String FB_PATH="freeBayes.path";
	public static final String MIN_MAP_SCORE="min.map.score";
	public static final String MIN_BASE_SCORE="min.base.score";
	public static final String READ_MISMATCH_LIMIT="read.mismatch.limit";
	public static final String MISMATCH_QUALITY_MIN="mismatch.quality.min";
	public static final String EXTRA_OPTIONS="FB.options";
	
	protected String freeBayesPath = null;
	protected String minMapScore = "30"; //Defaults to more stringent options because I felt like it, no good reason.
	protected String minBaseScore = "20";//Ibid.
	protected String readMismatchLimit = "4";//Ibid.
	protected String mismatchQualityMin = "10"; //Default quality for FreeBayes. I imagine it could be pretty useful, so I'm writing it in.
	protected String bedFileOpt = "";
	protected String extraOptions = "";
	protected String inputBAMs = "";
	protected ReferenceFile refBuf = null;
	protected VCFFile outputVCF = null;
	protected List<FileBuffer> inputBuffers;
	protected BEDFile inputBED = null;
	
	protected void parseOptions() {
		FileBuffer inputBEDfb = this.getInputBufferForClass(BEDFile.class);
		if(inputBEDfb != null) {
			inputBED = (BEDFile)inputBEDfb;
			bedFileOpt = " -t " + inputBED.getAbsolutePath();
		} else {
			bedFileOpt = "";
		}
		
		
		List<FileBuffer> inputBAMBuffers = this.getAllInputBuffersForClass(BAMFile.class);
		for(FileBuffer bamBuffer : inputBAMBuffers) {
			inputBAMs = inputBAMs + " -b " + bamBuffer.getAbsolutePath();
		}
		
		if (extraOptions == null || extraOptions.equals("null")) {
			extraOptions = "";
		}
		inputBuffers = this.getAllInputBuffersForClass(BAMFile.class);
		refBuf = (ReferenceFile) this.getInputBufferForClass(ReferenceFile.class);
		outputVCF = (VCFFile)this.getOutputBufferForClass(VCFFile.class);
	}
	
	protected String getCommand(String bedFileOpt, VCFFile vcf) {
		return  freeBayesPath
				+ " --fasta-reference " + refBuf.getAbsolutePath()
				+ inputBAMs
				+  " -m " + minMapScore + " -q " + minBaseScore + " -U " + readMismatchLimit + " -Q " + mismatchQualityMin
				+ bedFileOpt + " -v " + vcf.getAbsolutePath() + " " + extraOptions;
	}
	
	public void performOperation() throws OperationFailedException {	
		
		parseOptions();
		Logger.getLogger(Pipeline.primaryLoggerName).info("Freebayes is looking for SNPs with reference " + refBuf.getFilename() + " in source BAM file of " + inputBuffers.get(0).getFilename() + "." );
		
		Logger.getLogger(Pipeline.primaryLoggerName).info(this.getObjectLabel() + " is executing: " + getCommand(bedFileOpt, outputVCF));

		
		executeCommand(getCommand(bedFileOpt, outputVCF));
	}

	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String pathAttr = this.getAttribute(FB_PATH);
		if (pathAttr == null) {
			pathAttr = this.getPipelineProperty(FB_PATH);
		}
		if (pathAttr == null) {
			throw new IllegalArgumentException("No path to Freebayes found, please specify " + FB_PATH);
		}
		if (! (new File(pathAttr).exists())) {
			throw new IllegalArgumentException("No file found at Freebayes path : " + pathAttr);
		}
		this.freeBayesPath = (pathAttr);
		
		String minMapAttr = this.getAttribute(MIN_MAP_SCORE);
		if(minMapAttr == null) {
			minMapAttr = this.getPipelineProperty(MIN_MAP_SCORE);
		}
		if(minMapAttr != null) {
			this.minMapScore = minMapAttr;
		}
		
		String minBaseAttr = this.getAttribute(MIN_BASE_SCORE);
		if(minBaseAttr == null) {
			minBaseAttr = this.getPipelineProperty(MIN_BASE_SCORE);
		}
		if(minBaseAttr != null) {
			this.minBaseScore = minBaseAttr;
		}
		
		String mismatchAttr = this.getAttribute(READ_MISMATCH_LIMIT);
		if(mismatchAttr == null) {
			mismatchAttr = this.getPipelineProperty(READ_MISMATCH_LIMIT);
		}
		if(mismatchAttr != null) {
			this.readMismatchLimit = mismatchAttr;
		}
		
		String misQualAttr = this.getAttribute(MISMATCH_QUALITY_MIN);
		if(misQualAttr == null) {
			misQualAttr = this.getPipelineProperty(MISMATCH_QUALITY_MIN);
		}
		if(misQualAttr != null) {
			this.mismatchQualityMin = misQualAttr;
		}
		
		String extraAttr = this.getAttribute(EXTRA_OPTIONS);
		if(extraAttr == null) {
			extraAttr = this.getPipelineProperty(EXTRA_OPTIONS);
		}
		
		if(EXTRA_OPTIONS != null) {
			this.extraOptions = extraAttr;
		}

	}
}