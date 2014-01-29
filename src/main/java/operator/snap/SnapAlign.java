package operator.snap;

import java.io.File;
import java.util.List;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FileBuffer;

/**
 * This thing runs the fancy SNAP aligner. It aligns reads in either single or paired end mode,
 * and can create sorted, indexed bams. By default, this will produce sorted bams and expects 
 * exactly two fastqs in paired-end mode
 * @author brendan
 *
 */
public class SnapAlign extends IOOperator {

	public static final String SNAP_PATH = "snap.path";
	public static final String SNAP_INDEX = "snap.index";
	public static final String SORT = "sort";
	public static final String SINGLE_END = "single";
	public static final String SAMPLE = "sample";
	
	private String defaultRG = "${SAMPLE}\tPL:ILLUMINA";
	
	String samtoolsPath = null;
	String snapIndexPath = null;
	String snapPath = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		List<FileBuffer> inputBuffers = this.getAllInputBuffersForClass(FastQFile.class);
		
		FileBuffer outputBAMBuffer = this.getOutputBufferForClass(BAMFile.class);
		if (outputBAMBuffer == null) {
			throw new OperationFailedException("No output BAM file found", this);
		}

		String sortBAM = this.getAttribute(SORT);
		String sortOpt = " -so ";
		if (sortBAM != null) {
			if (! Boolean.parseBoolean(sortBAM)) {
				sortOpt = "";
			}
		}
		
		String singleAttr = this.getAttribute(SINGLE_END);
		String pairedOpt = " paired ";
		if (singleAttr != null) {
			if (Boolean.parseBoolean(singleAttr)) {
				pairedOpt = " single ";
			}
		}
		
		String sampleAttr = this.getAttribute(SAMPLE);
		if (sampleAttr == null) {
			throw new OperationFailedException("Must specify sample name as attribute.", this);
		}
		String readGroup = defaultRG.replace("${SAMPLE}", sampleAttr);
		
		String fastqs = "";
		
		//If paired, make sure we have exactly two fastqs
		if (pairedOpt.contains("paired")) {
			if (inputBuffers.size() != 2) {
				throw new OperationFailedException("For paired-end alignment exactly two fastq files must be provided, found " + inputBuffers.size(), this);
			}
			fastqs = inputBuffers.get(0).getAbsolutePath() + " " + inputBuffers.get(1).getAbsolutePath();			
		}
		else {
			//For single end alignment they can specify as many as they want
			for(FileBuffer inputFile : inputBuffers) {
				fastqs = fastqs + " " + inputFile.getAbsolutePath();
			}
		}
		

		
		int threads = this.getPipelineOwner().getThreadCount();
		
		String command1 = snapPath + " "
				+ pairedOpt + " "
				+ snapIndexPath + " "
				+ fastqs + " "
				+ sortOpt + " "
				+ " -M " //Use M instead of = in CIGARs, without this gatk and freebayes will break
				+ " -t " + threads
				+ " -o " + outputBAMBuffer.getAbsolutePath();
		String arg1 = " -rg \"" + readGroup + "\"";
		executeCommand(new String[]{"/bin/bash", " -c ", command1, arg1});
	}
	
	
	
	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String snapPathAttr = this.getAttribute(SNAP_PATH);
		if (snapPathAttr == null) {
			snapPathAttr = this.getPipelineProperty(SNAP_PATH);
		}
		if (snapPathAttr == null) {
			throw new IllegalArgumentException("No path to SNAP found, please specify " + SNAP_PATH);
		}
		if (! (new File(snapPathAttr).exists())) {
			throw new IllegalArgumentException("No file found at Snap path path : " + snapPath);
		}
		this.snapPath = snapPathAttr;
		
		
		String snapIndexAttr = this.getAttribute(SNAP_INDEX);
		if (snapIndexAttr == null) {
			snapIndexAttr = this.getPipelineProperty(SNAP_INDEX);
		}
		if (snapIndexAttr == null) {
			throw new IllegalArgumentException("No path to SNAP index found, please specify " + SNAP_INDEX);
		}
		if (! (new File(snapIndexAttr).exists())) {
			throw new IllegalArgumentException("No file found at Snap index path : " + snapIndexAttr);
		}
		this.snapIndexPath = snapIndexAttr;
			
	}



}
