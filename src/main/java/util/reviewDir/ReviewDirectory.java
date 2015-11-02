package util.reviewDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import buffer.VCFFile;
import buffer.variant.CSVLineReader;
import buffer.variant.JSONtoVariantPool;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantPool;
import json.JSONException;
import json.JSONObject;
import json.JSONTokener;
/**
 * Encapsulates a ReviewDIrectory and provides easy access to variants, manifest, etc.
 * @author brendan
 *
 */
public class ReviewDirectory {

	private File sourceDir;
	private SampleManifest manifest;
	private String[] fastqNames = null;

	public ReviewDirectory(String path) throws IOException, ManifestParseException {
		sourceDir = new File(path);
		if ((!sourceDir.exists()) || (!sourceDir.isDirectory())) {
			throw new IOException("Can't read source directory at " + sourceDir.getAbsolutePath());
		}

		manifest = SampleManifest.create(path);
	}

	public String getSourceDirPath() {
		return sourceDir.getAbsolutePath();
	}

	public SampleManifest getSampleManifest() {
		return manifest;
	}

	public String getSourceDir() {
		return sourceDir.toString();
	}

	public String getSampleName() {
		return manifest.getSampleName();
	}

	public File getBAMFile() {
		return new File(sourceDir + "/" + manifest.getProperty("bam.file"));
	}

	public File getBEDFile() {
		return manifest.getBED();
	}

	public String getAnalysisType() {
		return manifest.getAnalysisType();
	}

	public VariantPool getVariantsFromVCF() throws IOException {
		VCFFile vcf = new VCFFile( manifest.getVCF());
		return new VariantPool(vcf);
	}

	public VariantPool getVariantsFromJSON() throws IOException, JSONException {
		return  JSONtoVariantPool.toVariantPool(manifest.getJSONVars().getAbsolutePath());
	}

	public VariantPool getVariantsFromCSV() throws IOException {
		File csv =  manifest.getAnnotatedVars();
		VariantLineReader csvReader = new CSVLineReader(csv);
		return new VariantPool(csvReader);
	}

	/** Returns an array of length 2 of the names of fastq1 and fastq2 of a normal paired-end pipeline run. Requires parsing these out of the log files
	 *  which isn't foolproof.
	 * @return
	 */
	public String[] getFastqNames() {
		if (fastqNames == null) {
			PipeInstanceLog log = new PipeInstanceLog(manifest.getLog().getAbsolutePath());
			try {
				fastqNames = log.getFastqNames();
			} catch (Exception e) {
				System.out.println("Error parsing fastq file names from log file: " + log.getLogFileName());
				e.printStackTrace();
			}
		}
		return fastqNames;
	}

	/**
	 * Open the given file and read it's gzipped contents into a json object
	 * @param gzFile File pointed to gzip-compressed json data
	 * @return JSONObject read from the data
	 * @throws IOException
	 * @throws JSONException
	 */
	private JSONObject readGzippedJSON(File gzFile) throws IOException, JSONException {
		InputStream istream = new GZIPInputStream(new FileInputStream(gzFile));
		JSONTokener jk = new JSONTokener(istream);

		JSONObject jobj = new JSONObject(jk);
		istream.close();
		return jobj;
	}
}
