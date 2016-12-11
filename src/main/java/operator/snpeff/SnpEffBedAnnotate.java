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
	
	private File outputFile = new File(this.getProjectHome() + "/snpeff_output.bed");
	
	
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

	public void setUpdownStreamLength(int updownStreamLength) {
		this.updownStreamLength = updownStreamLength;
	}

	public void setSpliceSiteSize(int spliceSiteSize) {
		this.spliceSiteSize = spliceSiteSize;
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
		
		//make snpEff command line (bed input and bed output options)
		String command = javaHome + " -Xmx16g -jar " + snpEffDir + "/snpEff.jar -c " + snpEffDir + 
				"/snpEff.config " + snpEffGenome + " -i bed -o bed -hgvs -nostats -ud " + updownStreamLength + 
				" -spliceSiteSize " + spliceSiteSize + " " + nocallBed;
		
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
		
		/*
		if (annos == null) {
			throw new OperationFailedException("Map not initialized", this);
			
		}
		
		String varStr = convertVar(var);
		
		String[] allAlts = varStr.split("\n");
		
		//crashes if one of the alts has no annotations
		for(String alt : allAlts) {
			List<SnpEffInfo> annoList = annos.get(alt);
			if (annoList == null) {
				throw new OperationFailedException("No annotation info found for " + varStr, this);
			}
		
			//use annos from any of the alts to choose highest ranking
			try {
				annotateFromList(var, annoList);
			} catch (IOException e) {
				throw new OperationFailedException(e.getMessage(), this);
			}
		}
	*/	
	}


	@Override
	public void initialize(NodeList children) {
		/*
		super.initialize(children);
		
		altJavaHome = this.getAttribute(ALT_JAVA_HOME);
		if (altJavaHome == null) {
			javaHome = "java";
		}
		else {
			javaHome = altJavaHome;
		}
			
		snpEffDir = this.getPipelineProperty(SNPEFF_DIR);
		if (snpEffDir == null) {
			snpEffDir = this.getAttribute(SNPEFF_DIR);
			if (snpEffDir == null) {
				throw new IllegalArgumentException("No path to snpEff dir specified, use " + SNPEFF_DIR);
			}
		}
		
		snpEffGenome = this.getAttribute(SNPEFF_GENOME);
		if (snpEffGenome == null) {
			throw new IllegalArgumentException("No snpEff genome specified, use attribute " + SNPEFF_GENOME);
		}
		
		String updownStreamStr = this.getAttribute(UPDOWNSTREAM_LENGTH);
		if (updownStreamStr != null) {
			updownStreamLength = Integer.parseInt(updownStreamStr);
		}

		String spliceSiteStr = this.getAttribute(SPLICESITE_SIZE);
		if (spliceSiteStr != null) {
			spliceSiteSize = Integer.parseInt(spliceSiteStr);
		}
		*/
	}
}
