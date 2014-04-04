package operator;


import org.w3c.dom.NodeList;

import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;

/**
 * This class uses a thread to read binary data from an input stream and write it to an output stream
 * Its used in Pipeline to read the data emitted to stdout (or stderr) by a process and write it
 * to a file. Without this running as a separate thread, buffers used to store data from stdout will
 * fill up and the process generating the data may hang. 
 * @author brendan
 *
 */
public class FixBAMIndex extends CommandOperator {
	
	public static final String PICARD_PATH = "picard.path";
	String picardDir = "";
	FileBuffer InputBAM = this.getInputBufferForClass(BAMFile.class);


	protected String getCommand() throws OperationFailedException {
		
		if (picardDir.endsWith("/")) {
			picardDir = picardDir.substring(0, picardDir.length()-1);
		}
		String command = "java -classpath " + picardDir + "/sam-1.99.jar net.sf.samtools.FixBAMFile" + InputBAM.getAbsolutePath() +
				"$(basename inputB .bam).dupLog O=$(basename " + InputBAM.getAbsolutePath() + " .bam)" + "fixed.bam";
		return(command);
	}

	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String picardDir = this.getAttribute(PICARD_PATH);
		if(picardDir == null) {
			picardDir = this.getPipelineProperty(PICARD_PATH);
		}
		if(picardDir == null) {
			picardDir = "~/picard-tools-1.55/";
		}
		
	}
}
