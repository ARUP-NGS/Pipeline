package util.reviewDir;

import java.io.File;
import java.util.Map;

/**
 * Contains information about a single reviewDir. Use the static method create(String path/to/dir) to
 * create one of these - we do this since we want the ability to swap in different construction methods
 * if/ when we decide to change the layout or format of these dirs. 
 * 
 * @author brendan
 *
 */
public class ReviewDirInfo {

	public static final String SAMPLE_NAME = "sample.name";
	public static final String ANALYSIS_TYPE = "analysis.type";
	public static final String VCF = "VCF";
	public static final String BAM = "BAM";
	public static final String BED = "BED";
	public static final String QC_JSON = "QC.JSON";
	public static final String LOG = "LOG";
	public static final String INPUT = "INPUT";
	
	private Map<String, String> manifest = null;
	private Map<String, File> files = null;
	private File source;
	
	ReviewDirInfo(File source, Map<String, String> manifest, Map<String, File> files) {
		this.source = source;
		this.manifest = manifest;
		this.files = files;
	}
	
	public static ReviewDirInfo create(String path, ReviewDirInfoFactory parser) throws ReviewDirParseException {
		return parser.constructInfo(path);
	}
	
	public static ReviewDirInfo create(String path) throws ReviewDirParseException {
		return (new DefaultReviewDirFactory()).constructInfo(path);
	}
	
	public File getSourceFile()  {
		return source;
	}
	
	public String getSampleName() {
		return manifest.get(SAMPLE_NAME);
	}
	
	public String getAnalysisType() {
		return manifest.get(ANALYSIS_TYPE);
	}
	
	public File getVCF() {
		return files.get(VCF);
	}
	
	public File getBAM() {
		return files.get(BAM);
	}
	
	public File getBED() {
		return files.get(BED);
	}
	
	public File getLog() {
		return files.get(LOG);
	}
	
	public File getQCJSON() {
		return files.get(QC_JSON);
	}
	
}
