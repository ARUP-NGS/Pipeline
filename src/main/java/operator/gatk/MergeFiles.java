package operator.gatk;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.GlobFileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

public class MergeFiles extends CommandOperator {

	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	public static final String PATH = "path";
	public static final String JVM_ARGS="jvmargs";
	
	private String finalVCFPath = null; //For use with JobWrangler / WranglerStatus writer
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String getCommand() throws OperationFailedException {
		ReferenceFile reference = (ReferenceFile) getInputBufferForClass(ReferenceFile.class);
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		List<String> filesToMerge = new ArrayList<String>();
		
		boolean hasBAMs = false;
		boolean hasVCFs = false;
		
		for(FileBuffer buffer : inputBuffers) {
			if (buffer instanceof ReferenceFile) {
				reference = (ReferenceFile) buffer;
				continue;
			}
			if ((buffer instanceof MultiFileBuffer) || (buffer instanceof GlobFileBuffer)) {
				MultiFileBuffer multiFile = (MultiFileBuffer)buffer;
				for(int i=0; i<multiFile.getFileCount(); i++) {
					FileBuffer mFile = multiFile.getFile(i);
					filesToMerge.add( mFile.getAbsolutePath());
					if (mFile instanceof BAMFile)
						hasBAMs = true;
					if (mFile instanceof VCFFile)
						hasVCFs = true;
				}
				
			}
			else {
				filesToMerge.add( buffer.getAbsolutePath() );
				if (buffer instanceof BAMFile) 
					hasBAMs = true;
				if (buffer instanceof VCFFile) {
					hasVCFs = true;
				}
			}
		}
		
		if (hasBAMs && hasVCFs) {
			throw new OperationFailedException("Cannot merge files of different types (found both BAM and VCF)", this);
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
				
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		FileBuffer bedFile = getInputBufferForClass(BEDFile.class);
		
		
		String outputPath = outputBuffers.get(0).getAbsolutePath();

		if (!hasBAMs && !hasVCFs) {
			logger.warning("Found neither BAM nor VCF files for MergeFiles, cannot merge.");
			return null;
		}
		
		String command = "X?";
		
		if (hasBAMs) {
			command = "java -Xms1g -Xmx8g " + jvmARGStr + " -jar " + gatkPath;
			command = command + " -R " + reference.getAbsolutePath() +
					" -T PrintReads ";
			for(String filePath : filesToMerge) {
				command = command + " -I " + filePath;
			}
			if (bedFile != null) {
				command = command + " -L:intervals,BED " + bedFile.getAbsolutePath() + " ";
			}
			command = command + " -o " + outputPath;
		}
		
		if (hasVCFs) {
			command = "java -Xms1g -Xmx8g " + jvmARGStr + " -jar " + gatkPath;
			command = command + " -R " + reference.getAbsolutePath() +
					" -T CombineVariants ";
			for(String filePath : filesToMerge) {
				command = command + " --variant " + filePath;
			}
			if (bedFile != null) {
				command = command + " -L:intervals,BED " + bedFile.getAbsolutePath() + " ";
			}
			command = command + " -o " + outputPath;
			finalVCFPath = outputPath;
		}
		
		return command;
	}

	public String getFinalVCFPath() {
		return finalVCFPath;
	}
}
