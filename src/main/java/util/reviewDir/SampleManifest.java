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
public class SampleManifest {

	public static final String MANIFEST_FILENAME = "sampleManifest.txt";
	public static final String SAMPLE_NAME = "sample.name";
	public static final String ANNOTATED_VARS = "annotated.vars";
	public static final String ANALYSIS_TYPE = "analysis.type";
	public static final String VCF = "VCF";
	public static final String BAM = "BAM";
	public static final String BED = "BED";
	public static final String QC_JSON = "QC.JSON";
	public static final String LOG = "LOG";
	public static final String INPUT = "INPUT";
	
	protected Map<String, String> manifest = null;
	protected Map<String, File> files = null;
	private File reviewDirRoot;
	
	SampleManifest(File source, Map<String, String> manifest, Map<String, File> files) {
		this.reviewDirRoot = source;
		this.manifest = manifest;
		this.files = files;
	}
	
	public static SampleManifest create(String path, ManifestReader parser) throws ManifestParseException {
		return parser.readManifest(path);
	}
	
	public static SampleManifest create(String path) throws ManifestParseException {
		return (new DefaultManifestFactory()).readManifest(path);
	}
	
	public File getManifestFile() {
		return new File( getSourceFile().getAbsolutePath() + "/" + MANIFEST_FILENAME);
	}
	
	public String getProperty(String key) {
		return manifest.get(key);
	}
	
	public boolean hasProperty(String key) {
		return manifest.containsKey(key);
	}
	
	public File getSourceFile()  {
		return reviewDirRoot;
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
	
	public File getAnnotatedVars() {
		return files.get(ANNOTATED_VARS);
	}
	
}
