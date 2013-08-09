package operator.gatk;

import java.util.logging.Logger;

import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.FileBuffer;
import buffer.GlobFileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

public class VariantRecalibrator extends CommandOperator {

	public static final String MAX_GAUSSIANS="max.gaussians";
	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	public static final String ADDITIONAL_TAGS="additional.tags";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	protected int maxGaussians = 5;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String[] getCommands() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
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
		
		String additionalTagsStr = properties.get(ADDITIONAL_TAGS);
		String[] additionalTags = new String[]{};
		if (additionalTagsStr != null) {
			additionalTags = additionalTagsStr.split(",");
			logger.info("Using additional tags : " + additionalTagsStr);
		}
		
		String maxGaussianStr = properties.get(MAX_GAUSSIANS);
		if (maxGaussianStr != null) {
			maxGaussians = Integer.parseInt(maxGaussianStr);
			logger.info("Setting max.gaussians to : " + maxGaussians);
		}
		
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		FileBuffer inputFile = inputBuffers.get(1);
		FileBuffer hapmapFile = inputBuffers.get(2);
		FileBuffer genomesFile = inputBuffers.get(3);
		FileBuffer dbsnpFile = inputBuffers.get(4);
		FileBuffer backgroundFile = null;
		if (inputBuffers.size()==6) {
			backgroundFile = inputBuffers.get(5);
		}
			
		String[] inputFilePaths;
		if (backgroundFile != null) {
			if (! (backgroundFile instanceof GlobFileBuffer)) {
				throw new OperationFailedException("Input file #6, if it exists, must be a glob file buffer", this);
			}
			
			GlobFileBuffer backgroundFiles = (GlobFileBuffer) backgroundFile;
			if (backgroundFiles.getFileCount()==0) {
				throw new OperationFailedException("No files found in background VCFs glob, this is probably an error.", this);
			}
			inputFilePaths = new String[backgroundFiles.getFileCount()+1];
			inputFilePaths[0] = inputFile.getAbsolutePath();
			for(int i=0; i<backgroundFiles.getFileCount(); i++) {
				inputFilePaths[i+1] = backgroundFiles.getFile(i).getAbsolutePath();
			}
		}
		else {
			inputFilePaths = new String[1];
			inputFilePaths[0] = inputFile.getAbsolutePath();
		}

		FileBuffer outputVCF = getOutputBufferForClass(VCFFile.class);
		
		String projHome = getProjectHome();
		String recalFilePath = projHome + "vqsr.output.recal";
		String tranchesPath = projHome + "vqsr.output.tranches";
		String rScriptPath = projHome + "vqsr.output.plots.R";
		
				
		String recalCommand = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		recalCommand = recalCommand + " -R " + reference + " -T VariantRecalibrator ";
		for(int i=0; i<inputFilePaths.length; i++) {
			recalCommand = recalCommand + " -input " + inputFilePaths[i] + " ";
		}
		recalCommand = recalCommand + "-resource:hapmap,known=false,training=true,truth=true,prior=15.0 " + hapmapFile.getAbsolutePath() + " ";
		recalCommand = recalCommand + "-resource:omni,known=false,training=true,truth=false,prior=12.0 " + genomesFile.getAbsolutePath() + " ";
		recalCommand = recalCommand + "-resource:dbsnp,known=true,training=false,truth=false,prior=8.0 " + dbsnpFile.getAbsolutePath() + " ";
		recalCommand = recalCommand + "-an QD -an HaplotypeScore -an MQRankSum -an ReadPosRankSum ";
		for(int i=0; i<additionalTags.length; i++) 
			recalCommand = recalCommand + "-an " + additionalTags[i] + " ";
		recalCommand = recalCommand + " --maxGaussians " + maxGaussians + " ";
		recalCommand = recalCommand + " -recalFile " + recalFilePath + " ";
		recalCommand = recalCommand + " -tranchesFile " + tranchesPath + " ";
		//recalCommand = recalCommand + "-rscriptFile " + rScriptPath + " "; 
				   
		
		
		String applyCommand = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		applyCommand = applyCommand + " -R " + reference + " -input " + inputFile.getAbsolutePath() + " -T ApplyRecalibration ";	
		applyCommand = applyCommand + " -recalFile " + recalFilePath + " ";
		applyCommand = applyCommand + " -tranchesFile " + tranchesPath + " ";
		applyCommand = applyCommand + " -o " + outputVCF.getAbsolutePath();
		
		return new String[]{recalCommand, applyCommand};
	}

	@Override
	protected String getCommand() throws OperationFailedException {
		//Nothing to do, we've overridden getCommands()
		return null;
	}
	
}
