package operator.picard;

import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Uses Picard to coorindate sort a BAM file
 * @author brendan
 *
 */
public class CoordinateSort extends CommandOperator {

	
	public static final String PATH = "path";
	public static final String CREATE_INDEX = "createindex";
	public static final String JVM_ARGS="jvmargs";
	public static final String MAX_RECORDS="maxrecords"; //Max records to keep in RAM
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	protected String picardDir = defaultPicardDir;
	protected boolean defaultCreateIndex = true;
	protected boolean createIndex = defaultCreateIndex;
	protected int defaultMaxRecords = 2500000; //5x picard default
	
	
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
		
		String recordsStr = properties.get(MAX_RECORDS);
		int maxRecords = defaultMaxRecords;
		if (recordsStr != null) {
			maxRecords = Integer.parseInt(recordsStr);
		}
		
		String createIndxStr = properties.get(CREATE_INDEX);
		if (createIndxStr != null) {
			Boolean b = Boolean.parseBoolean(createIndxStr);
			createIndex = b;
		}
		
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) getPipelineProperty(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
				
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String command = "java -Xms2g -Xmx8g " + jvmARGStr + " -jar " + picardDir + "/SortSam.jar" + " INPUT=" + inputPath + " OUTPUT=" + outputPath + " SORT_ORDER=coordinate VALIDATION_STRINGENCY=LENIENT CREATE_INDEX=" + createIndex + " MAX_RECORDS_IN_RAM=" + maxRecords + " ";
		return command;
	}

}
