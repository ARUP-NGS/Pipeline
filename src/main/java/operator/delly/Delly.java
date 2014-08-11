package operator.delly;

import java.io.IOException;
import java.util.logging.Logger;

import javax.management.openmbean.OpenDataException;

import json.JSONException;
import operator.CommandOperator;
import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import buffer.BAMFile;

/**
 * Runs Delly, written by Tobias Rausch of EMBL.
 * 
 * @author daniel
 *
 */
public class Delly extends IOOperator {

	public static final String DELLY_PATH = "delly.path";
	public static final String SV_ANALYSIS_TYPE = "sv.analysis.type";
	public static final String EXCLUSION_BED = "exclusion.bed";
	String dellyPath;
	String excludeString;
	String sample = "unknown";
	String svAnalysis;

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		Logger.getLogger(Pipeline.primaryLoggerName).info(
				"Delly is searching for structural variations of the type: "
						+ svAnalysis);
		FileBuffer refBuf = this.getInputBufferForClass(ReferenceFile.class);
		FileBuffer outVCF = this.getOutputBufferForClass(VCFFile.class);
		FileBuffer inBAM = this.getInputBufferForClass(BAMFile.class);
		String ref = refBuf.getAbsolutePath();
		if(ref==null) {
			throw new OperationFailedException("Reference file is null", this);
		}
		String outputVCF = outVCF.getAbsolutePath();
		if(outputVCF==null) {
			throw new OperationFailedException("Output VCF is null", this);
		}
		String inputBAM = inBAM.getAbsolutePath();
		if(inputBAM==null) {
			throw new OperationFailedException("Input BAM file is null", this);
		}
		String command_str = dellyPath + " -g " + ref + " -t " + svAnalysis
				+ excludeString + " -o " + outputVCF + " " + inputBAM;
		Logger.getLogger(Pipeline.primaryLoggerName).info("Command String: " + command_str);
		executeCommand(command_str,true);
		return;
	}

	@Override
	public void initialize(NodeList children) {
		super.initialize(children);

		String dellyAttr = this.getAttribute(DELLY_PATH);
		if (dellyAttr == null) {
			dellyAttr = this.getPipelineProperty(DELLY_PATH);
		}
		if (dellyAttr == null) {
			throw new IllegalArgumentException(
					"No path to delly found. Please specify " + DELLY_PATH);
		}
		this.dellyPath = dellyAttr;

		String svAttr = this.getAttribute(SV_ANALYSIS_TYPE);
		if (svAttr == null) {
			throw new IllegalArgumentException(
					"You must specify the kind of SV analysis desired. Translocation (TRA), Deletion (DEL), Duplication (DUP), Inversion (INV).");
		}
		this.svAnalysis = svAttr;

		String excludeAttr = this.getAttribute(EXCLUSION_BED);
		if (excludeAttr == null) {
			excludeAttr = "";
			this.excludeString = excludeAttr;
		} else {
			this.excludeString = " -x " + excludeAttr + " ";
		}
	}
}
