package operator.picard;

import operator.OperationFailedException;
import operator.PipedCommandOp;

import org.w3c.dom.NodeList;

import pipeline.PipelineXMLConstants;
import buffer.FileBuffer;
import buffer.SAMFile;

/*
* This goes through a SAM file and marks duplicates (default) or removes them, making a processed SAM file, without harming the original.
* David Nix's Translocation Pipeline uses Picardtools' MarkDuplicates with the option to remove them.
* @author daniel
*/
public class MarkDuplicates extends PipedCommandOp {
	
	public static final String PICARD_REMOVE_DUPLICATES = "picard.remove.duplicates";
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	public static final String JVM_ARGS="jvmargs";
	protected String picardDir = defaultPicardDir;
	String rmDup = "true"; //Defaults to true, remove the duplicates

	protected String getCommand() throws OperationFailedException {
		FileBuffer inputSAM = this.getInputBufferForClass(SAMFile.class);
		
		Object path = getPipelineProperty(PipelineXMLConstants.PICARD_PATH);
		if (path != null)
			picardDir = path.toString();
		
		if (picardDir.endsWith("/")) {
			picardDir = picardDir.substring(0, picardDir.length()-1);
		}
		String dupStat="";
		if(rmDup == "true") {
			dupStat = "DupFree";
		}
		else {
			dupStat = "DupMarked";
		}
		String command = "java -jar Xmx16G " + picardDir + " REMOVE_DUPLICATES=" + rmDup 
				+ " I=" + inputSAM.getAbsolutePath() + " METRICS_FILE=$(basename " + inputSAM.getAbsolutePath() +
				" .sam).dupLog O=$(basename " + inputSAM.getAbsolutePath() + " .sam)" + dupStat + ".sam";
		return(command);

	}

	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String rmDup = this.getAttribute(PICARD_REMOVE_DUPLICATES);
		if(rmDup == null) {
			rmDup = this.getPipelineProperty(PICARD_REMOVE_DUPLICATES);
		}
		if(rmDup == null) {
			rmDup = "true";
		}
		
	}
	
}

