package operator.abra;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;
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
 * If a bed file is not formatted to abra's preferences (only 3 columns, sorted by position),
 * then this program creates a properly-formatted bed file and uses that instead.
 * 
 * If rm.tmp.dir is unset, it defaults to true. IE, remove the temp directory.
 * 
 * @author daniel
 *
 */
public class ABRA extends IOOperator {

	public static final String ABRA_PATH = "abra.jar.path";
	public static final String TEMPDIR = "tmp.dir";
	public static final String EXTRA_OPTIONS = "abra.options";
	public static final String THREADS = "threads";
	public static final String REMOVE_TMP_DIR = "rm.tmp.dir";
	String abraPath;
	String AnalysisType;
	String tempdir;
	String extraOpts;
	String threads;
	String lowCovThreshold;
	boolean RemoveTmpDir;

	@Override
	public void performOperation() throws OperationFailedException, IOException {
		Logger.getLogger(Pipeline.primaryLoggerName).info(
				"Abra is about to run.");
		FileBuffer refBuf = this.getInputBufferForClass(ReferenceFile.class);
		FileBuffer outBAM = this.getOutputBufferForClass(BAMFile.class);
		FileBuffer inBAM = this.getInputBufferForClass(BAMFile.class);
		FileBuffer bed = this.getInputBufferForClass(BEDFile.class);
		String bedPath = bed.getAbsolutePath();
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
		
		//Check the bed file to make sure it has only three columns
		FileInputStream bedReader = new FileInputStream(bed.getAbsolutePath());
		Scanner bedscanner = new Scanner(bedReader);
		if(bedscanner.nextLine().trim().split("\t").length > 3){
			Logger.getLogger(Pipeline.primaryLoggerName).info("Bed file incorrectly formatted.");
			String CleanBedCommand = "cut -f1-3 " + bed.getAbsolutePath();
			String tmpBed = bed.getAbsolutePath().substring(bed.getAbsolutePath().lastIndexOf("/")) + String.valueOf((Math.round((1e9) * Math.random())));
			executeCommandCaptureOutput(CleanBedCommand, new File(tmpBed));
			String SortBedCommand = "sort -k1,1 -k2,2n " + tmpBed;
			String tmpBed2 = bed.getAbsolutePath().substring(bed.getAbsolutePath().lastIndexOf("/")) + String.valueOf((Math.round((1e9) * Math.random())));
			executeCommandCaptureOutput(SortBedCommand, new File(tmpBed2));
			executeCommand("mv " + tmpBed2 + " " + tmpBed);
			bedPath = tmpBed;
			Logger.getLogger(Pipeline.primaryLoggerName).info("New correctly-formatted bed file at location: " + bedPath);
		}
		bedscanner.close();
		bedReader.close();
		command_str = "java -jar " + abraPath + " --in "
				+ inBAM.getAbsolutePath() + " --ref " + ref + " --targets "
				+ bedPath + " --out " + outBAM.getAbsolutePath()
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
		RemoveTmpDir = true;

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
		
		String removeAttr = this.searchForAttribute(REMOVE_TMP_DIR);
		if(removeAttr != null)
			RemoveTmpDir = Boolean.parseBoolean(removeAttr);

		/*
		 * String lowCovThresholdAttr = this.getAttribute(LOWCOVTHRESHOLD); if
		 * (lowCovThresholdAttr == null) { this.lowCovThreshold = ""; } else
		 * this.lowCovThreshold = "--lowcov " + lowCovThresholdAttr;
		 */
	}
}
