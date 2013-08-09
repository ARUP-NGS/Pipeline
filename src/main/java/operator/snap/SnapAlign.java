package operator.snap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

public class SnapAlign extends IOOperator {

	public static final String SNAP_PATH = "snap.path";
	public static final String SNAP_INDEX = "snap.index";
	public static final String SAMTOOLS_PATH = "samtools.path";
	String samtoolsPath = null;
	String snapIndexPath = null;
	String snapPath = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		throw new OperationFailedException("Yo, this operator isn't finished yet since snap insists on making an enormous sam file and won't write it to std out", this);
		
//		List<FileBuffer> inputBuffers = this.getAllInputBuffersForClass(FastQFile.class);
//		
//		FileBuffer outputBAMBuffer = this.getOutputBufferForClass(BAMFile.class);
//		if (outputBAMBuffer == null) {
//			throw new OperationFailedException("No output BAM file found", this);
//		}
//		
//		if (inputBuffers.size() != 2) {
//			throw new OperationFailedException("Exactly two fastq files must be provided to SnapAlign, found " + inputBuffers.size(), this);
//		}
//		
//		int threads = this.getPipelineOwner().getThreadCount();
//		
//		String command = snapPath 
//				+ " paired "
//				+ snapIndexPath
//				+ inputBuffers.get(0).getAbsolutePath()
//				+ inputBuffers.get(1).getAbsolutePath()
//				+ " -so "
//				+ " -t " + threads
//				+ " | " + samtoolsPath + " view -Sb - > " + outputBAMBuffer.getAbsolutePath();
//		
//		executeBASHCommand(command);
	}
	
	private void executeBASHCommand(String command) throws OperationFailedException {
		String filename = this.getProjectHome() + "/snapcommand-" + ((1000000.0*Math.random())+"").substring(0, 6).replace(".", "") + ".sh";
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			writer.write(command + "\n");
			writer.close();
		} catch (IOException e) {
			throw new OperationFailedException("IO Error writing snap command file : " + e.getMessage(), this);
		}
		
		
		executeCommand("/bin/bash " + filename);
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
		
		String samtoolsAttr = this.getAttribute(SAMTOOLS_PATH);
		if (samtoolsAttr == null) {
			samtoolsAttr = this.getPipelineProperty(SAMTOOLS_PATH);
		}
		this.samtoolsPath = samtoolsAttr;
		
		
	}



}
