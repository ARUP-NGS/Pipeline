package operator.picard;

import java.util.logging.Logger;

import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Uses Picard to coordinate to sort a BAM file
 * Runs with the new picard.jar style of execution not the /picardxxx/SortSam.jar
 * @author brendan, Nix
 *
 */
public class CoordinateSortBigJar extends CommandOperator {

	
	public static final String PATH = "path";
	public static final String CREATE_INDEX = "createindex";
	public static final String JVM_ARGS="jvmargs";
	public static final String MAX_RECORDS="maxrecords"; //Max records to keep in RAM
	public static final String MEMORY_RANGE = "memory.range";
	protected String defaultPicardDir = "~/picard-tools-1.134/";
	protected String picardDir = defaultPicardDir;
	protected boolean defaultCreateIndex = true;
	protected boolean createIndex = defaultCreateIndex;
	protected int defaultMaxRecords = 2500000; //5x picard default
	protected String memoryRange = " -Xms2G -Xmx8G ";//Default memory string
	
	
	@Override
	protected String getCommand() {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);	
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
	
		String memoryAttr = this.getAttribute(MEMORY_RANGE);
		if(memoryAttr != null){
			memoryRange=memoryAttr;
			logger.info("Default memory options overridden. New value: " + memoryRange);
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
		
		String command = "java " + memoryRange + " " + jvmARGStr + " -jar " + picardDir + "/picard.jar SortSam" + " INPUT=" + inputPath + " OUTPUT=" + outputPath + " SORT_ORDER=coordinate VALIDATION_STRINGENCY=SILENT CREATE_INDEX=" + createIndex + " MAX_RECORDS_IN_RAM=" + maxRecords + " ";
		return command;
	}

}
