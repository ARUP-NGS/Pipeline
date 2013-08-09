package operator.picard;

import java.io.File;
import java.util.logging.Logger;

import buffer.BAMFile;
import buffer.FileBuffer;
import operator.MultiOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class MultiCoordSort extends MultiOperator {

	public static final String PATH = "path";
	public static final String CREATE_INDEX = "createindex";
	public static final String JVM_ARGS="jvmargs";
	public static final String MAX_RECORDS="maxrecords"; //Max records to keep in RAM
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	protected String picardDir = defaultPicardDir;
	protected boolean defaultCreateIndex = true;
	protected boolean createIndex = defaultCreateIndex;
	protected int defaultMaxRecords = 1000000; //5x picard default
	
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
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
//		String jvmARGStr = properties.get(JVM_ARGS);
//		if (jvmARGStr == null || jvmARGStr.length()==0) {
//			jvmARGStr = (String) Pipeline.getPropertyStatic(JVM_ARGS);
//		}
		
		//Make a temporary directory with a unique name in proj-home for java temp io
		String projHome = getProjectHome();
		String tmpDirName = projHome + ".io.tmp." + (int)Math.round( (1e9)*Math.random());
		File tmpDir = new File(tmpDirName);
		while (tmpDir.exists()) {
			tmpDirName = ".io.tmp." + (int)Math.round( (1e9)*Math.random());
			tmpDir = new File(tmpDirName);
		}
		boolean ok = tmpDir.mkdir();
		if (!ok) {
			logger.warning("Could not create tmp directory " + tmpDir.getAbsolutePath() + " resorting to default java.io.tmpdir");
			tmpDir = new File(projHome);
			tmpDir.mkdir();
		}
		
		String jvmARGStr = " -Djava.io.tmpdir=" + tmpDir.getAbsolutePath(); 
		System.out.println("MultiCoordSort setting jvm args to : " + jvmARGStr);
		logger.info("MultiCoordSort setting jvm args to : " + jvmARGStr);
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
				
		String inputPath = inputBuffer.getAbsolutePath();
		int index = inputPath.lastIndexOf(".");
		String prefix = inputPath;
		if (index>0)
			prefix = inputPath.substring(0, index);
		String outputPath = prefix + ".sorted.bam";
		
		BAMFile outputBAM = new BAMFile(new File(outputPath), inputBuffer.getContig());
		addOutputFile(outputBAM);
		
		String command = "java -Xms1g -Xmx8g " + jvmARGStr + " -jar " + picardDir + "/SortSam.jar" + " INPUT=" + inputPath + " OUTPUT=" + outputPath + " SORT_ORDER=coordinate VALIDATION_STRINGENCY=LENIENT CREATE_INDEX=" + createIndex + " MAX_RECORDS_IN_RAM=" + maxRecords + " ";
		return new String[]{command};
	}
}
