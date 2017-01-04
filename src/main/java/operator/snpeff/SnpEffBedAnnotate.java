package operator.snpeff;

import java.io.File;
import java.util.logging.Logger;
import org.w3c.dom.NodeList;
import buffer.variant.VariantRec;
import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;


/**
 * Uses SnpEff to provide gene annotations for bed file
 * @author jacobd
 *
 */
public class SnpEffBedAnnotate extends Annotator{

	public static final String SNPEFF_DIR = "snpeff.dir";
	public static final String SNPEFF_GENOME = "snpeff.genome";
	public static final String PERFORM_MITO_SUB = "perform.mito.sub";	
	public static final String UPDOWNSTREAM_LENGTH = "updownstream.length";
	public static final String SPLICESITE_SIZE = "spliceSite.size";
	public static final String ALT_JAVA_HOME = "alt.java.home";
	public static final String NOCALL_BED = "nocall.bed";

	protected String javaHome = null;
	protected String altJavaHome = null;
	protected String snpEffDir = null;
	protected String snpEffGenome = null;
	protected int updownStreamLength = 0;
	protected int spliceSiteSize = 0;
	protected String nocallBed = null;
	protected File outputFile = null;
	protected Boolean canon = false;
	
	
	// Use setters and getters since this class will be instantiated from another operator

	public void setJavaHome(String javaHome) {
		this.javaHome = javaHome;
	}

	public void setSnpEffDir(String snpEffDir) {
		this.snpEffDir = snpEffDir;
	}

	public void setSnpEffGenome(String snpEffGenome) {
		this.snpEffGenome = snpEffGenome;
	}

	public void setNocallBed(String nocallBed) {
		this.nocallBed = nocallBed;
	}

	public void setCanon(Boolean canon) {
		this.canon = canon;
	}

	public void setUpdownStreamLength(int updownStreamLength) {
		this.updownStreamLength = updownStreamLength;
	}

	public void setSpliceSiteSize(int spliceSiteSize) {
		this.spliceSiteSize = spliceSiteSize;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	public File getOutputFile() {
		return outputFile;
	}


	public void runSnpEff() {

		//make sure non-default inputs have been passed in
		if (this.javaHome == null) throw new IllegalArgumentException("No javaHome specified, use setter (default is java)");
		if (this.snpEffDir == null) throw new IllegalArgumentException("No snpEff directory specified, use setter");
		if (this.snpEffGenome == null) throw new IllegalArgumentException("No snpEff genome specified, use setter");
		if (this.nocallBed == null) throw new IllegalArgumentException("No nocallBed file specified, use setter");
		
		//make string for only canonical transcripts
		String canonOpt = "";
		if (canon) {
			canonOpt = " -canon ";
		}
		
		//make snpEff command line (bed input and bed output options)
		String command = javaHome + " -Xmx16g -jar " + snpEffDir + "/snpEff.jar closest " + canonOpt +
				" -bed -ss 0 -spliceRegionExonSize 0 -spliceRegionIntronMax 0 -ud 0 -c " +
				snpEffDir + "/snpEff.config " + snpEffGenome + " " + nocallBed;
		
		//run snpEff
		Logger.getLogger(Pipeline.primaryLoggerName).info("Executing command: " + command);
		try {
			executeCommandCaptureOutput(command, outputFile);
		} catch (OperationFailedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		
		// empty method to extend abstract Annotator class 
	}

	@Override
	public void initialize(NodeList children) {
		
		// empty method to extend abstract Annotator class 
	}
}
