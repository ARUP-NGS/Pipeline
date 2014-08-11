package operator.picard;

import java.util.List;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;

/*
 * This operator implements Picard's MergeTools
 *
 * @author daniel
 */
public class MergeSam extends IOOperator {

	public static final String JVM_ARGS = "jvmargs";
	public static final String MEMORY_RANGE = "memory.range";
	String jvmargs = "-Djava.io.tmpdir=/mounts/tmp/";// Set default jvmargs
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	protected String picardDir = defaultPicardDir;
	protected String memoryRange = " -Xms2G -Xmx8G ";

	public void performOperation() throws OperationFailedException {

		List<FileBuffer> inputBuffers = this
				.getAllInputBuffersForClass(BAMFile.class);
		FileBuffer outBam = this.getOutputBufferForClass(BAMFile.class);
		String fileList = "";
		Logger.getLogger(Pipeline.primaryLoggerName).info(
				"PicardTools is merging the input files.");
		for (FileBuffer bam : inputBuffers) {
			fileList += " I=" + bam.getAbsolutePath();
		}

		String memoryAttr = getAttribute(MEMORY_RANGE);
		if (memoryAttr != null) {
			memoryRange = memoryAttr;
			Logger.getLogger(Pipeline.primaryLoggerName).info("Default memory range overridden. New value: " + memoryRange + ".");
		}

		// Get Picard Path, JVM attributes
		Object path = getPipelineProperty(PipelineXMLConstants.PICARD_PATH);
		if (path != null) {
			picardDir = path.toString();
		}

		if (picardDir.endsWith("/")) {
			picardDir = picardDir.substring(0, picardDir.length() - 1);
		}

		String jvmAttr = this.getAttribute(JVM_ARGS);
		if (jvmAttr == null) {
			jvmAttr = this.getPipelineProperty(JVM_ARGS);
		}
		if (jvmAttr != null) {
			this.jvmargs = jvmAttr;
		}
		String command = "java -Xms2G -Xmx20G " + jvmargs + " -jar "
				+ picardDir + "/MergeSamFiles.jar " + fileList + " O="
				+ outBam.getAbsolutePath()
				+ " USE_THREADING=true CREATE_INDEX=true MSD=true";

		Logger.getLogger(Pipeline.primaryLoggerName).info(
				this.getObjectLabel() + " is executing: " + command);

		executeCommand(command);

	}

}