package operator;

import org.w3c.dom.NodeList;

import pipeline.PipelineXMLConstants;
import buffer.BEDFile;
import buffer.ReferenceFile;
import buffer.TextBuffer;

public class MultiSampleDepthWalker extends CommandOperator {

	public static final String PATH = "path";
	protected String kitBed = "/home/locovmulti_test_data/halo_immune_v2.bed";
	protected String hgmdSNP_vcf = "/home/locovmulti_test_data/hgmd_snp_test2.vcf";
	protected String hgmdIND_vcf = "/home/locovmulti_test_data/hgmd_indels_test2.vcf";
	protected String sizeFile = "/home/locovmulti_test_data/human.b37.genome";
	protected String lowcovPath = "/home/locovmulti_test_data/lowcovmulti.py";
	protected String gatkPath ="/usr/prog/GenomeAnalysisTK-2.6-5/GenomeAnalysisTK.jar";
	
	public static final String OutPrefix = "out.prefix";
	public static final String LowCovCountMin="lowCovCountMin";
	public static final String NoCovCountMin="noCovCountMin";
	public static final String MIN_DEPTH="min.depth";
		
	protected TextBuffer inputBamsFileList = null;
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
		
	
		String outputPrefix=(OutPrefix);
		
		String lowCovCountMin="1";
		String lowCovCountMinAttr = properties.get(LowCovCountMin);
		if(lowCovCountMinAttr != null){
			lowCovCountMin = lowCovCountMinAttr;
		}
		
		String noCovCountMin="1";
		String noCovCountMinAttr = properties.get(NoCovCountMin);
		if(noCovCountMinAttr != null){
			noCovCountMin = noCovCountMinAttr;
		}
		
		String minDepth="5";
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

		String command = "python " + lowcovPath;
		command = command + " -l " + lowCovCountMin + " -n " + noCovCountMin + " -d " + minDepth + " -g " + sizeFile;
		command = command + " -kbed " + kitBed + " -ref " + reference + " -gatk " + gatkPath + " -SNP " + hgmdSNP_vcf;
		command = command + " -InD " + hgmdIND_vcf + " " + inputBamsFileList.getAbsolutePath();
		command = command + " " + targets.getAbsolutePath() + " " + outputPrefix ;

		return command;
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		inputBamsFileList = (TextBuffer) super.getInputBufferForClass(TextBuffer.class);
		targets = (BEDFile) super.getInputBufferForClass(BEDFile.class);
	}

}