package operator.bwt2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import json.JSONException;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.BAMFile;
import buffer.ReferenceFile;

public class Bowtie2Align extends IOOperator {
	public static final String BOWTIE2_DIR = "bowtie2.dir";
	public static final String SAMTOOLS_PATH = "samtools.path";
	public static final String BOWTIE2_STYLE = "bowtie2.style";
	public static final String BOWTIE2_SENSITIVITY = "bowtie2.sensitivity";
	public static final String EXTRA_OPTIONS = "bwt2.options";
	public static final String THREADS = "threads";
	public static final String SAMPLE = "sample";

	String sample = "unknown";
	String samtoolsPath = "samtools";
	String style = "--local";
	String sensitivity = "--very-sensitive";
	String bowtie2path = null;
	String extraOpts = "";
	String threads = "4";

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

		String sampleAttr = this.getAttribute(SAMPLE);
		if(sampleAttr != null) {
			sample = sampleAttr;
			logger.info("Sample name now set to: " + sample + ".");
		}
		
		String sensitivityAttr = this.getAttribute(BOWTIE2_SENSITIVITY);
		if (sensitivityAttr != null) {
			sensitivity = sensitivityAttr;
			logger.info("Bowtie sensitivity setting set to: " + sensitivity
					+ ".");
		} else {
			logger.info("Bowtie sensitivity setting is the default: "
					+ sensitivity + ".");
		}
		String styleAttr = this.getAttribute(BOWTIE2_STYLE);
		if (styleAttr != null) {
			style = styleAttr;
			logger.info("Bowtie style setting set to: " + style + ".");
		} else {
			logger.info("Bowtie style setting set to default: " + style + ".");
		}

		String extraAttr = this.getAttribute(EXTRA_OPTIONS);
		if (extraAttr != null) {
			extraOpts = extraAttr;
			logger.info("Additional options were set for bowtie2 alignment. Provided: "
					+ extraOpts + ".");
		}
		String threadsAttr = this.getAttribute(THREADS);
		if (threadsAttr != null) {
			threads = threadsAttr;
			logger.info("Now aligning with " + threads + " threads.");
		}

		String pathAttr = this.getPipelineProperty(BOWTIE2_DIR);
		if (pathAttr == null) {
			throw new IllegalArgumentException(
					"No path to Bowtie2 found, please specify " + BOWTIE2_DIR);
		}
		if(!pathAttr.endsWith("/"))
			pathAttr=pathAttr+"/";
		if (!(new File(pathAttr + "bowtie2").exists())) {
			throw new IllegalArgumentException(
					"No file found at Bowtie2 path : " + pathAttr + "bowtie2");
		}
		bowtie2path = (pathAttr + "bowtie2");

		String samtoolsAttr = this.getPipelineProperty(SAMTOOLS_PATH);
		if (samtoolsAttr == null) {
			throw new IllegalArgumentException(
					"No path to samtools found, please specify "
							+ SAMTOOLS_PATH);
		}
		if (!(new File(samtoolsAttr).exists())) {
			throw new IllegalArgumentException(
					"No file found at samtools path : " + samtoolsAttr);
		}
		samtoolsPath = samtoolsAttr;
		String ref = this.getInputBufferForClass(ReferenceFile.class)
				.getAbsolutePath();
		List<FileBuffer> InputFastqs = this
				.getAllInputBuffersForClass(FastQFile.class);
		String Reads1 = InputFastqs.get(0).getAbsolutePath();
		String Reads2 = InputFastqs.get(1).getAbsolutePath();
		String OutputBAM = this.getOutputBufferForClass(BAMFile.class)
				.getAbsolutePath();
		String OutputSAM = OutputBAM.substring(0, OutputBAM.lastIndexOf('.'))
				+ ".sam";
		String command = bowtie2path + " -p " + threads + " " + extraOpts + " "
				+ style + " " + sensitivity + " --rg-id \"unknown\" --rg SM:" + sample + " -x " + ref + " -1 " + Reads1
				+ " -2 " + Reads2 + " -S " + OutputSAM;
		logger.info("About to align with bowtie2.\nCommand String is: "
				+ command);
		executeCommand(command);
		String Sam2Bam = samtoolsPath + " view -Sbhu -o " + OutputBAM + " "
				+ OutputSAM;
		logger.info("About to convert SAM to BAM.\nCommand String is: "
				+ Sam2Bam);
		executeCommand(Sam2Bam);
		String rmSam = "rm " + OutputSAM;
		logger.info("Removing intermediate SAM file.\nCommand String is: "
				+ rmSam);
		executeCommand(rmSam);
	}

}
