package operator.variant;

import java.util.logging.Logger;

import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * This operator runs the GaTK's variant evaluation module, in a limited form for now. We use it
 * to compute a few basic summary statistics for a variant file, most importantly number of snp's
 * found in DBSnp, and the transition to transversion ratio for all, known, and novel snps. 
 * Note that supplying dbSNP is actually optional. If supplied, the SECOND variant file is assumed to
 * be the dbSNP file, the first is always the input variants. The reference file can be anywhere, as
 * can a BED file describing the regions of interest.  
 * @author brendan
 *
 */
public class VariantEval extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	@Override
	public boolean requiresReference() {
		return true;
	}

	@Override
	protected String getCommand() throws OperationFailedException {
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
		
		ReferenceFile referenceFile = (ReferenceFile) getInputBufferForClass(ReferenceFile.class);
		BEDFile intervalsFile = null;
		FileBuffer bedBuf = getInputBufferForClass(BEDFile.class);
		if (bedBuf != null) {
			intervalsFile = (BEDFile) bedBuf;
		}
		
		VCFFile inputVariants = null;
		VCFFile dbSNPFile = null;

		for(int i=0; i<inputBuffers.size(); i++) {
			FileBuffer buf = inputBuffers.get(i);
			if (buf instanceof VCFFile) {
				if (inputVariants == null)
					inputVariants = (VCFFile)buf;
				else {
					if (dbSNPFile == null) {
						dbSNPFile = (VCFFile) buf;
						logger.info("VariantEval found dbsnp file : " + dbSNPFile.getAbsolutePath());
					}
					else {
						throw new OperationFailedException("Too many input VCFs, need either one or two.", this);
					}
				}
				
			}
		}
		
		if (inputVariants == null)
			throw new OperationFailedException("No input variant file found", this);
		
		String inputPath = inputVariants.getAbsolutePath();
		
		FileBuffer outputFile = outputBuffers.get(0);
		String outputPath = outputFile.getAbsolutePath();
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + referenceFile.getAbsolutePath() + 
				" -T VariantEval " + 
				" --eval " + inputPath +
				" -o " + outputPath;
		if (dbSNPFile != null)
				command = command + " --dbsnp " + dbSNPFile.getAbsolutePath();
		if (intervalsFile != null)
			command = command + " -L:intervals,BED " + intervalsFile.getAbsolutePath();
		
		return command ;
	}
	
	

}
