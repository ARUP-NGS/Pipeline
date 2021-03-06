package operator.picard;

import org.w3c.dom.NodeList;

import buffer.BAMFile;
import buffer.FileBuffer;
import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.PipelineXMLConstants;

/*
* This goes through a SAM file and marks duplicates (default) or removes them, making a processed SAM file, without harming the original.
* David Nix's Translocation Pipeline uses Picardtools' MarkDuplicates with the option to remove them.
* @author daniel
*/
public class MarkDuplicates extends CommandOperator {
	
	public static final String PICARD_REMOVE_DUPLICATES = "picard.remove.duplicates";
	public static final String PICARD_MAKE_INDEX = "picard.make.index";
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	public static final String JVM_ARGS="jvmargs";
	protected String picardDir = defaultPicardDir;
	private String rmDup = "true"; //Defaults to true, remove the duplicates
	private String makeIndex = "false"; //Defaults to false

	protected String getCommand() throws OperationFailedException {
		FileBuffer inputBAM = this.getInputBufferForClass(BAMFile.class);
		FileBuffer outputBAM = this.getOutputBufferForClass(BAMFile.class);
		
		Object path = getPipelineProperty(PipelineXMLConstants.PICARD_PATH);
		if (path != null)
			picardDir = path.toString();
		
		if (picardDir.endsWith("/")) {
			picardDir = picardDir.substring(0, picardDir.length()-1);
		}
				
		String command = "java -Xmx16g -jar " + picardDir + "/picard.jar MarkDuplicates"
				+ " REMOVE_DUPLICATES=" + rmDup 
				+ " CREATE_INDEX="+ makeIndex
				+ " I=" + inputBAM.getAbsolutePath() 
				+ " METRICS_FILE=" + (inputBAM.getAbsolutePath()).substring(0, (inputBAM.getAbsolutePath()).lastIndexOf('.'))
				+ ".dupLog O="+ outputBAM.getAbsolutePath() + " ASSUME_SORTED=true VALIDATION_STRINGENCY=SILENT";
		
		return(command);
	}

	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		rmDup = this.getAttribute(PICARD_REMOVE_DUPLICATES).toLowerCase();
		if(rmDup == null) rmDup = "true";
		
		makeIndex = this.getAttribute(PICARD_MAKE_INDEX);
		if (makeIndex == null) makeIndex = "false";
		
	}
	
}

