package operator.gatk;

import java.util.List;

import operator.CommandOperator;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

/**
 * Uses the GATK to count the covariates
 * @author brendan
 *
 */
public class CountCovariates extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String CREATE_INDEX = "createindex";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected int threads = 4;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String getCommand() {
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
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
		if (!jvmARGStr.contains("java.io.tmpdir"))
			jvmARGStr =jvmARGStr + " -Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir");
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		List<FileBuffer> knownSitesFiles = getAllInputBuffersForClass(VCFFile.class);
		
		StringBuffer knownSitesStr = new StringBuffer();
		for(FileBuffer buff : knownSitesFiles) {
			knownSitesStr.append("-knownSites " + buff.getAbsolutePath() + " ");
		}
		
		if (knownSitesFiles.size() == 0) {
			throw new IllegalArgumentException("You must list some known sites files for CountCovariates to work");
		}
		
		String csvOutput = outputBuffers.get(0).getAbsolutePath();
		
		String covariateList = "-cov QualityScoreCovariate -cov CycleCovariate -cov DinucCovariate -cov MappingQualityCovariate -cov HomopolymerCovariate ";
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference + 
				" -I " + inputFile + 
				" -T CountCovariates " + 
				covariateList + " "	+ 
				knownSitesStr.toString() +
				" -recalFile " + csvOutput;
		return command;
	}

}
