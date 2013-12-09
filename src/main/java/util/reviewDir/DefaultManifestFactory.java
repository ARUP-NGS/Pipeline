package util.reviewDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Stateless parser for review directories from the file system. Currently this is the only implementation
 * of ReviewDirInfoFactory. 
 *  
 * @author brendan
 *
 */
public class DefaultManifestFactory implements ManifestReader {

	public static final String SAMPLE_MANIFEST_NAME = "sampleManifest.txt";
	
	@Override
	public SampleManifest readManifest(String pathToReviewDir)
			throws ManifestParseException {
		
		File dir = new File(pathToReviewDir);
		if (! dir.exists()) {
			throw new ManifestParseException("File at path " + pathToReviewDir + " does not exist");
		}
		if (! dir.canRead()) {
			throw new ManifestParseException("File at path " + pathToReviewDir + " exists but is not readable");
		}
		if (! dir.isDirectory()) {
			throw new ManifestParseException("File at path " + pathToReviewDir + " is not a directory.");
		}
		
		Map<String, String> manifest = parseManifest(dir);
		Map<String, File> files = findFiles(dir);
		
		
		return new SampleManifest(dir, manifest, files);
	}
	
	/**
	 * Search for BAM, BED, VCF, and other file types and return all of those found in a Map
	 * @param dir
	 * @return
	 */
	private static Map<String, File> findFiles(File dir) {
		Map<String, File> files = new HashMap<String, File>();
		File[] subdirs = dir.listFiles();

		File bamDir = fileByName(subdirs, "bam");
		if (bamDir != null) {
			File bamFile = fileBySuffix(bamDir.listFiles(), "bam");
			if (bamFile != null) {
				files.put(SampleManifest.BAM, bamFile);
			}
		}

		File vcfDir = fileByName(subdirs, "var");
		if (vcfDir != null) {
			File file = fileBySuffix(vcfDir.listFiles(), "vcf");
			if (file != null) {
				files.put(SampleManifest.VCF, file);
			}
		}

		File logDir = fileByName(subdirs, "log");
		if (logDir != null) {
			File file = fileBySuffix(logDir.listFiles(), "txt");
			if (file != null) {
				files.put(SampleManifest.LOG, file);
			}
		}

		File bedDir = fileByName(subdirs, "bed");
		if (bedDir != null) {
			File file = fileBySuffix(bedDir.listFiles(), "bed");
			if (file != null) {
				files.put(SampleManifest.BED, file);
			}
		}


		File qcDir = fileByName(subdirs, "qc");
		if (qcDir != null) {
			File file = fileBySuffix(qcDir.listFiles(), "qc.json");
			if (file != null) {
				files.put(SampleManifest.QC_JSON, file);
			}
		}
			
		return files;
	}
	
	private static File fileByName(File[] files, String name) {
		for(int i=0; i<files.length; i++) {
			if (files[i].getName().equals(name)) {
				return files[i];
			}
		}
		return null;
	}
	
	private static File fileBySuffix(File[] files, String suffix) {
		for(int i=0; i<files.length; i++) {
			if (files[i].getName().endsWith(suffix)) {
				return files[i];
			}
		}
		return null;
	}

	/**
	 * Return contents of sampleManifest as key=value pairs in a Map
	 * @param rootDir
	 * @return
	 * @throws ManifestParseException
	 */
	private static Map<String, String> parseManifest(File rootDir) throws ManifestParseException {
		File manifestFile = new File(rootDir.getAbsolutePath() + System.getProperty("file.separator") + SAMPLE_MANIFEST_NAME);
		if (! manifestFile.exists()) {
			throw new ManifestParseException("Cannot find manifest");
		}
		
		Map<String, String> manifestInfo = new HashMap<String, String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(manifestFile));
			String line = reader.readLine();
			while (line != null) {
				if (line.length() > 0 && (! line.startsWith("#"))) {
					if (line.contains("=")) {
						String key = line.substring(0, line.indexOf("=")).trim();
						String value = line.substring(line.indexOf("=")+1).trim();
						manifestInfo.put(key, value);
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					//ignore this? 
				}
			}
			throw new ManifestParseException("Error reading manifest: " + e.getLocalizedMessage());
		}
		
		return manifestInfo;
	}
	

}
