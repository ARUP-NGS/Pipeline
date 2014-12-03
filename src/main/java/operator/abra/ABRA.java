package operator.abra;

import java.io.IOException;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.BAMFile;
import buffer.BEDFile;

/**
 * Runs abra!
 * 
 * @author daniel
 *
 */
public class ABRA extends IOOperator {

	public static final String ABRA_PATH = "abra.jar.type";
	public static final String TEMPDIR = "tmp.dir";
	public static final String EXTRA_OPTIONS = "abra.options";
	public static final String THREADS = "threads";
	String abraPath;
	String AnalysisType;
	String tempdir;
	String extraOpts;
	String threads;
	String lowCovThreshold;

	@Override
	public void performOperation() throws OperationFailedException, IOException {
		Logger.getLogger(Pipeline.primaryLoggerName).info(
				"Abra is about to run.");
		FileBuffer refBuf = this.getInputBufferForClass(ReferenceFile.class);
		FileBuffer outBAM = this.getOutputBufferForClass(BAMFile.class);
		FileBuffer inBAM = this.getInputBufferForClass(BAMFile.class);
		FileBuffer bed = this.getInputBufferForClass(BEDFile.class);
		String ref = refBuf.getAbsolutePath();
		if (ref == null) {
			throw new OperationFailedException("Reference file is null", this);
		}
		String outputBAM = outBAM.getAbsolutePath();
		if (outputBAM == null) {
			throw new OperationFailedException("Output BAM is null", this);
		}
		String inputBAM = inBAM.getAbsolutePath();
		if (inputBAM == null) {
			throw new OperationFailedException("Input BAM file is null", this);
		}
		String command_str = "";
		String tempdirStr;
		String projHome = getProjectHome();
		String tempDirName = projHome + ".io.tmp."
				+ (int) Math.round((1e9) * Math.random());
		if (tempdir.equals(""))
			tempdirStr = " --working " + tempDirName;
		else
			tempdirStr = "--working " + tempdir;
		command_str = "java -jar " + abraPath + " --in "
				+ inBAM.getAbsolutePath() + " --ref " + ref + " --targets "
				+ bed.getAbsolutePath() + " --out " + outBAM.getAbsolutePath()
				+ " --working" + tempdirStr + " " + threads + " " + extraOpts;
		Logger.getLogger(Pipeline.primaryLoggerName).info(
				"Command String: " + command_str);
		executeCommand(command_str);
		Logger.getLogger(Pipeline.primaryLoggerName).info(
				"Now removing the temporary directory.");
		executeCommand("rm -r " + tempDirName);
		return;
	}

	@Override
	public void initialize(NodeList children) {
		super.initialize(children);

		String abraAttr = this.getAttribute(ABRA_PATH);
		if (abraAttr == null) {
			abraAttr = this.getPipelineProperty(ABRA_PATH);
		}
		if (abraAttr == null) {
			throw new IllegalArgumentException(
					"No path to abra found. Please specify " + ABRA_PATH);
		}
		this.abraPath = abraAttr;

		String tempdirAttr = this.getAttribute(TEMPDIR);
		if (tempdirAttr == null) {
			tempdirAttr = "";
		}
		this.tempdir = tempdirAttr;

		String extraOptsAttr = this.getAttribute(EXTRA_OPTIONS);
		if (extraOptsAttr == null) {
			extraOptsAttr = "";
		}
		this.extraOpts = extraOptsAttr;

		this.threads = "--threads 8"; // Default 8
		String threadsAttr = this.searchForAttribute(THREADS);
		if (threadsAttr != null) {
			this.threads = "--threads 8" + threadsAttr;
		}

		/*
		 * String lowCovThresholdAttr = this.getAttribute(LOWCOVTHRESHOLD); if
		 * (lowCovThresholdAttr == null) { this.lowCovThreshold = ""; } else
		 * this.lowCovThreshold = "--lowcov " + lowCovThresholdAttr;
		 */
	}
}
