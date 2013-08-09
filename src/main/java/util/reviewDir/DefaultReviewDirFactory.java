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
public class DefaultReviewDirFactory implements ReviewDirInfoFactory {

	public static final String SAMPLE_MANIFEST_NAME = "sampleManifest.txt";
	
	@Override
	public ReviewDirInfo constructInfo(String pathToReviewDir)
			throws ReviewDirParseException {
		
		File dir = new File(pathToReviewDir);
		if (! dir.exists()) {
			throw new ReviewDirParseException("File at path " + pathToReviewDir + " does not exist");
		}
		if (! dir.canRead()) {
			throw new ReviewDirParseException("File at path " + pathToReviewDir + " exists but is not readable");
		}
		if (! dir.isDirectory()) {
			throw new ReviewDirParseException("File at path " + pathToReviewDir + " is not a directory.");
		}
		
		Map<String, String> manifest = parseManifest(dir);
		Map<String, File> files = findFiles(dir);
		
		
		return new ReviewDirInfo(dir, manifest, files);
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
				files.put(ReviewDirInfo.BAM, bamFile);
			}
		}

		File vcfDir = fileByName(subdirs, "var");
		if (vcfDir != null) {
			File file = fileBySuffix(vcfDir.listFiles(), "vcf");
			if (file != null) {
				files.put(ReviewDirInfo.VCF, file);
			}
		}

		File logDir = fileByName(subdirs, "log");
		if (logDir != null) {
			File file = fileBySuffix(logDir.listFiles(), "txt");
			if (file != null) {
				files.put(ReviewDirInfo.LOG, file);
			}
		}

		File bedDir = fileByName(subdirs, "bed");
		if (bedDir != null) {
			File file = fileBySuffix(bedDir.listFiles(), "bed");
			if (file != null) {
				files.put(ReviewDirInfo.BED, file);
			}
		}


		File qcDir = fileByName(subdirs, "qc");
		if (qcDir != null) {
			File file = fileBySuffix(qcDir.listFiles(), "qc.json");
			if (file != null) {
				files.put(ReviewDirInfo.QC_JSON, file);
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
	 * @throws ReviewDirParseException
	 */
	private static Map<String, String> parseManifest(File rootDir) throws ReviewDirParseException {
		File manifestFile = new File(rootDir.getAbsolutePath() + System.getProperty("file.separator") + SAMPLE_MANIFEST_NAME);
		if (! manifestFile.exists()) {
			throw new ReviewDirParseException("Cannot find manifest");
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
			throw new ReviewDirParseException("Error reading manifest: " + e.getLocalizedMessage());
		}
		
		return manifestInfo;
	}
	
	
	public static void main(String[] args) throws ReviewDirParseException {
		ReviewDirInfo info = ReviewDirInfo.create("/home/brendan/MORE_DATA/clinical_exomes/2013-05-20/13052133816");
		
	}

}
