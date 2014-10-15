package operator.scalpel;

import java.io.IOException;
import java.util.logging.Logger;

import json.JSONException;
import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import buffer.BAMFile;
import buffer.BEDFile;

/**
 * Runs scalpel, not finished.
 * 
 * @author daniel
 *
 */
public class Scalpel extends IOOperator {

	public static final String SCALPEL_PATH = "scalpel.path";
	public static final String ANALYSIS_TYPE = "analysis.type";
	public static final String OUTDIR = "out.dir";
	public static final String EXTRA_OPTIONS = "scalpel.options";
	public static final String THREADS = "threads";
	public static final String LOWCOVTHRESHOLD = "low.cov.threshold";
	String scalpelPath;
	String AnalysisType;
	String outdir;
	String extraOpts;
	String threads;
	String lowCovThreshold;

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		Logger.getLogger(Pipeline.primaryLoggerName).info(
				"Scalpel is about to run.");
		FileBuffer refBuf = this.getInputBufferForClass(ReferenceFile.class);
		FileBuffer outVCF = this.getOutputBufferForClass(VCFFile.class);
		FileBuffer inBAM = this.getInputBufferForClass(BAMFile.class);
		FileBuffer bed = this.getInputBufferForClass(BEDFile.class);
		String ref = refBuf.getAbsolutePath();
		if (ref == null) {
			throw new OperationFailedException("Reference file is null", this);
		}
		String outputVCF = outVCF.getAbsolutePath();
		if (outputVCF == null) {
			throw new OperationFailedException("Output VCF is null", this);
		}
		String inputBAM = inBAM.getAbsolutePath();
		if (inputBAM == null) {
			throw new OperationFailedException("Input BAM file is null", this);
		}
		String command_str = "";
		if (AnalysisType.equals("--single")) {
			String outdirStr;
			if (outdir.equals(""))
				outdirStr = "";
			else
				outdirStr = "--dir " + outdir;
			command_str = scalpelPath + " " + AnalysisType + " --bam "
					+ inBAM.getAbsolutePath() + " --ref " + ref + " "
					+ outdirStr + " " + threads + " " + extraOpts;
			Logger.getLogger(Pipeline.primaryLoggerName).info(
					"Command String: " + command_str);
		} else if (AnalysisType.equals("--somatic")) {
			command_str = scalpelPath + " " + AnalysisType + " --bam " + ref
					+ " -t ";
			Logger.getLogger(Pipeline.primaryLoggerName).info(
					"Command String: " + command_str);
		} else {
			throw new OperationFailedException(
					"Analysis type either not yet supported by Pipeline or invalid.",
					this);
		}
		executeCommand(command_str);
		return;
	}

	@Override
	public void initialize(NodeList children) {
		super.initialize(children);

		String scalpelAttr = this.getAttribute(SCALPEL_PATH);
		if (scalpelAttr == null) {
			scalpelAttr = this.getPipelineProperty(SCALPEL_PATH);
		}
		if (scalpelAttr == null) {
			throw new IllegalArgumentException(
					"No path to scalpel found. Please specify " + SCALPEL_PATH);
		}
		this.scalpelPath = scalpelAttr;

		String analysisAttr = this.getAttribute(ANALYSIS_TYPE);
		if (analysisAttr == null) {
			throw new IllegalArgumentException(
					"You must specify the analysis type. Options: --single, --somatic. Not yet supported: --denovo, --export");
		}
		this.AnalysisType = analysisAttr.toLowerCase();
		String outdirAttr = this.getAttribute(OUTDIR);
		if (outdirAttr == null) {
			outdirAttr = "";
		}
		this.outdir = outdirAttr;

		String extraOptsAttr = this.getAttribute(EXTRA_OPTIONS);
		if (extraOptsAttr == null) {
			extraOptsAttr = "";
		}
		this.extraOpts = extraOptsAttr;
		
		String threadsAttr = this.getAttribute(THREADS);
		if (threadsAttr == null) {
			threadsAttr = "";
			this.threads = "";
		}
		else
			this.threads = "--numprocs " + threadsAttr;

		String lowCovThresholdAttr = this.getAttribute(LOWCOVTHRESHOLD);
		if (lowCovThresholdAttr == null) {
			this.lowCovThreshold =  "";
		}
		else
			this.lowCovThreshold = "--lowcov " + lowCovThresholdAttr;
/*		
		String lowCovThresholdAttr = this.getAttribute(LOWCOVTHRESHOLD);
		if (lowCovThresholdAttr == null) {
			this.lowCovThreshold =  "";
		}
		else
			this.lowCovThreshold = "--lowcov " + lowCovThresholdAttr;
*/
	}
}
