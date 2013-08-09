package operator;

import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;

/**
 * This operator performs the indel realignment target creation and actual realignment step
 * in parallel, by breaking the input bam into chromosome-sized chunks and running each chunk
 * independently. A threadpool is used to handle the parallelism.
 * After all realignments happen they are stitched back together into a single, realigned BAM  
 * @author brendan
 *
 */
public class ParallelRealign extends MultiOperator {

	protected BAMFile inputBam;
	protected ThreadPoolExecutor threadPool = null;
	public static final String JVM_ARGS="jvmargs";
	public static final String PATH = "path";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected String jvmARGStr = "";
	protected String knownIndelsPath = null;
	protected MultiFileBuffer multiBAM;
	
	public ParallelRealign() {
		
	}

	
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
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
		
		FileBuffer bedFile = getInputBufferForClass(BEDFile.class);
				
		String inputPath = inputBuffer.getAbsolutePath();
		String contig = inputBuffer.getContig();
				
		String rand = "" + (int)Math.round( 100000*Math.random() );
		String targetsPath;
		String realignedContigPath;
		String pathPrefix = (String) getPipelineProperty(Pipeline.PROJECT_HOME);
		if (pathPrefix == null)
			pathPrefix = "";
		
		if (contig != null) {
			targetsPath = pathPrefix + "targets_" + contig + "_" + rand + ".intervals";
			realignedContigPath = pathPrefix + "contig_" + contig + ".realigned.bam";
		}
		else {
			targetsPath = pathPrefix + "targets_" + rand + ".intervals";
			realignedContigPath = pathPrefix + inputBuffer.getFilename().replace(".bam", "") + ".realigned.bam";
		}
		
		
		String command = "java -Xmx2g " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference.getAbsolutePath() + 
				" -I " + inputPath + 
				" -T RealignerTargetCreator -o " + targetsPath;
	
		if (bedFile != null)
			command = command + " -L:intervals,BED " + bedFile.getAbsolutePath();
		
		if (bedFile == null && contig != null)
			command = command +	" -L " + contig;
		
//		if (contig != null && bedFile != null) {
//			command = command +	" -isr INTERSECTION ";
//		}
		
		
		if (knownIndelsPath != null) {
			command = command + " --known " + knownIndelsPath;
		}
		
		String command2 = "java -Xmx2g " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference.getAbsolutePath() + 
				" -I " + inputPath +
				" -T IndelRealigner " + 
				" -targetIntervals " + targetsPath + " -o " + realignedContigPath;
		if (contig != null)
				command2 = command2 +	" -L " + contig;
		if (knownIndelsPath != null) {
			command2 = command2 + " --known " + knownIndelsPath;
		}
		outputFiles.addFile(new BAMFile(new File(realignedContigPath), contig));
		return new String[]{command, command2};
	}
		

}
