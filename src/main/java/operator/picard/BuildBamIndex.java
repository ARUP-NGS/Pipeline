package operator.picard;

import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class BuildBamIndex extends CommandOperator {

	public static final String PATH = "path";
	public static final String CREATE_INDEX = "createindex";
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	public static final String JVM_ARGS="jvmargs";
	protected String picardDir = defaultPicardDir;
	protected boolean defaultCreateIndex = true;
	protected boolean createIndex = defaultCreateIndex;
	
	@Override
	protected String getCommand() {
	
		Object path = getPipelineProperty(PipelineXMLConstants.PICARD_PATH);
		if (path != null)
			picardDir = path.toString();
		
		//User can override path specified in properties
		String userPath = properties.get(PATH);
		if (userPath != null) {
			 picardDir = userPath;
		}
		
		if (picardDir.endsWith("/")) {
			picardDir = picardDir.substring(0, picardDir.length()-1);
		}
		
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) getPipelineProperty(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
		
		String command = "java -Xms2g -Xmx16g " + jvmARGStr + " -jar " + picardDir + "/BuildBamIndex.jar" + " INPUT=" + inputPath + " VALIDATION_STRINGENCY=LENIENT";
		return command;
	}

}
