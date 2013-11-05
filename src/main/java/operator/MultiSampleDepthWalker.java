package operator;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NodeList;

import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

public class MultiSampleDepthWalker extends CommandOperator {

	public static final String PATH = "path";
	protected String kitBed = "/home/locovmulti_test_data/halo_immune_v2.bed";
	protected String hgmdSNP_vcf = "/home/locovmulti_test_data/hgmd_snp_test2.vcf";
	protected String hgmdIND_vcf = "/home/locovmulti_test_data/hgmd_indels_test2.vcf";
	protected String sizeFile = "/home/locovmulti_test_data/human.b37.genome";
	protected String lowcovPath = "/home/locovmulti_test_data/lowcovmulti.py";
	protected String gatkPath ="/usr/prog/GenomeAnalysisTK-2.6-5/GenomeAnalysisTK.jar";
	
	public static final String BAM_FILES = "bam.files";
	public static final String PANEL_BED = "panel.bed";
	public static final String OutPrefix = "out.prefix";
	public static final String LowCovCountMin="lowCovCountMin";
	public static final String NoCovCountMin="nowCovCountMin";
	public static final String MIN_DEPTH="min.depth";
		
	protected List<BAMFile> inputBams = null;
	protected BEDFile targets = null;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String getCommand() throws OperationFailedException {
		
		Object propsPath = getPipelineProperty("pylowcov.PATH"); 
		if (propsPath != null)
			lowcovPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			lowcovPath = path;
		}
		
		
		//Don't get bams from attributes, get them as objects given to this operator 
		//String bamfiles=properties.get(BAM_FILES);
		
		//For each input bam add its absolute path to the string of paths
		String bamFilePaths = "";
		for(BAMFile bam : inputBams) {
			bamFilePaths = bamFilePaths + bam.getAbsolutePath() + " ";
		}
		
		
		
		String outputPrefix=(OutPrefix);
		
		String lowCovCountMin="5";
		String lowCovCountMinAttr = properties.get(LowCovCountMin);
		if(lowCovCountMinAttr != null){
			lowCovCountMin = lowCovCountMinAttr;
		}
		
		String noCovCountMin="5";
		String noCovCountMinAttr = properties.get(NoCovCountMin);
		if(noCovCountMinAttr != null){
			noCovCountMin = noCovCountMinAttr;
		}
		
		String minDepth="15";
		String minDepthAttr = properties.get(MIN_DEPTH);
		if(minDepthAttr != null){
			minDepth = minDepthAttr;
		}
		
		propsPath = getPipelineProperty("chromosize.path");
		if (propsPath != null)
			sizeFile = propsPath.toString();
		
		propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		propsPath = getPipelineProperty("hgmdSNP.vcf");
		if (propsPath != null)
			hgmdSNP_vcf = propsPath.toString();
		
		propsPath = getPipelineProperty("hgmdIND.vcf");
		if (propsPath != null)
			hgmdIND_vcf = propsPath.toString();
		
		propsPath = getPipelineProperty("kitBed");
		if (propsPath != null)
			kitBed = propsPath.toString();		
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();

		String command = "python " + lowcovPath + " " + bamFilePaths + " " + targets.getAbsolutePath();
		command = command + " " + outputPrefix + " -l " + lowCovCountMin + " -n " + noCovCountMin;
		command = command + " -d " + minDepth + " -g " + sizeFile + " -ref " + reference;
		command = command + " -gatk " + gatkPath + " -SNP " + hgmdSNP_vcf + " -IND " + hgmdIND_vcf;

		return command;
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		List<FileBuffer> bams = super.getAllInputBuffersForClass(BAMFile.class);
		inputBams = new ArrayList<BAMFile>();
		for(FileBuffer bam : bams) {
			inputBams.add( (BAMFile)bam);
		}
		
		targets = (BEDFile) super.getInputBufferForClass(BEDFile.class);
	}

}