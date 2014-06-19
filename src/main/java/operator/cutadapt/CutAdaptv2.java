package operator.cutadapt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import operator.IOOperator;
import operator.OperationFailedException;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.TextBuffer;

/**
 * Rewrite of cutadapt
 * 
 * @author daniel
 * 
 */
public class CutAdaptv2 extends IOOperator {

	public static final String CUTADAPT_PATH = "cutadapt.path";
	public static final String ADAPTER_SEQ = "adapter.seq";
	public static final String CUT_OPTS = "cut.opts";
	private String cutAdaptPath = null;
	private String adapterString = null; // Read in from a text file
											// (textbuffer) to initialize
	private String cutOpts = null;

	@Override
	public void performOperation() throws OperationFailedException {
		if (cutAdaptPath == null) {
			cutAdaptPath = this.getPipelineProperty(CUTADAPT_PATH);
			if (cutAdaptPath == null)
				throw new OperationFailedException("No path to cutadapt found",
						this);
		}

		FileBuffer inputFastq = this.getInputBufferForClass(FastQFile.class);

		if (!(inputFastq instanceof FastQFile)) {
			throw new OperationFailedException(
					"Input fastq is not a fastq file.", this);
		}

		/*
		 * if (adapterString == null) { try { FileBuffer adapterBuf =
		 * this.getInputBufferForClass(TextBuffer.class); if (adapterBuf ==
		 * null) throw new
		 * OperationFailedException("No adapter file specified, cannot cut adapters"
		 * , this); BufferedReader reader = new BufferedReader(new
		 * FileReader(adapterBuf.getFile())); StringBuffer adapterStr = new
		 * StringBuffer(); String line = reader.readLine(); while(line != null)
		 * { adapterStr.append(" -a " + line.trim()); line = reader.readLine();
		 * } reader.close(); adapterString = adapterStr.toString(); } catch
		 * (IOException ex) { throw new
		 * OperationFailedException("Error reading adapter sequences : " +
		 * ex.getMessage(), this); } }
		 */
		if (adapterString == null) {
			adapterString = this.getAttribute(ADAPTER_SEQ);
		}
		if (adapterString == null) {
			throw new OperationFailedException(
					"No adapter string provided. It cannot be trimmed", this);
		}
		String cutAttr = this.getAttribute(CUT_OPTS);
		if (cutAttr != null) {
			cutOpts = cutAttr;
		} else {
			cutOpts = "";
		}
		FileBuffer outputFile = this.getOutputBufferForClass(FastQFile.class);
		String outputFilename = outputFile.getAbsolutePath();
		String command = cutAdaptPath + " -a " + adapterString + " " + cutOpts + " "
				+ inputFastq.getAbsolutePath();
		executeCommandCaptureOutput(command, outputFile.getFile());

	}

}
