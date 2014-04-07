package operator.freebayes;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.BEDFile;
import buffer.VCFFile;
import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;

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
	
	String freeBayesPath = null;
	String minMapScore = "30"; //Defaults to more stringent options because I felt like it, no good reason.
	String minBaseScore = "20";//Ibid.
	String readMismatchLimit = "0";//Ibid.
	String mismatchQualityMin = "10"; //Default quality for FreeBayes. I imagine it could be pretty useful, so I'm writing it in.
	String bedFilePath = "";
	public void performOperation() throws OperationFailedException {
		
		ReferenceFile refBuf = (ReferenceFile) this.getInputBufferForClass(ReferenceFile.class);
		List<FileBuffer> inputBuffers = this.getAllInputBuffersForClass(BAMFile.class);
		FileBuffer inputBED = this.getInputBufferForClass(BEDFile.class);
		String baseName = inputBuffers.get(0).getAbsolutePath().substring(0, inputBuffers.get(0).getAbsolutePath().lastIndexOf('.'));
		Logger.getLogger(Pipeline.primaryLoggerName).info("Freebayes is looking for SNPs with reference " + refBuf.getFilename() + " in source BAM file of " + inputBuffers.get(0).getFilename() + "." );

		if(inputBED != null) {
			bedFilePath = " -t " + inputBED.getAbsolutePath();
		}
		
		String inputBAM = null;
		if( inputBuffers.get(0) != null) {
			inputBAM = " -b " + inputBuffers.get(0).getAbsolutePath();
		}
		
		String command = freeBayesPath
				+ " --fasta-reference " + refBuf.getAbsolutePath()
				+ inputBAM
				+  " -m " + minMapScore + " -q " + minBaseScore + " -U " + readMismatchLimit + " -Q " + mismatchQualityMin
				+ bedFilePath + " -v " + baseName + ".allvariants.vcf";
		executeCommand(command);

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
		
		}
	
}