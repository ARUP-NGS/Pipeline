package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import pipeline.ObjectHandler;
import pipeline.Pipeline;

/**
 * This variant pool stores info for all OMIM entries stored as text files in a
 * directory (directory usually stored in .pipelineprops.xml). This variant pool
 * can then be quickly referenced to see if some query variant has a OMIM hit 
 * @author brendan
 *
 */
public class OMIMVariants extends VariantPool {

	public static final String OMIM_DIR = "omim.dir";
	File baseDir = null;
	
	public OMIMVariants(ObjectHandler handler) {
		this.setObjectHandler(handler);
		String omimDir = (String) getPipelineProperty(OMIM_DIR);
		if (omimDir == null || omimDir.length()==0) {
			throw new IllegalArgumentException("OMIM base directory not specified (use omim.dir in pipelineprops file)");	
		}
		
		baseDir = new File(omimDir);
		if (!baseDir.exists()) {
			throw new IllegalArgumentException("OMIM base directory " + baseDir.getAbsolutePath() + " does not exist");			
		}
		if (!baseDir.isDirectory()) {
			throw new IllegalArgumentException("OMIM base directory " + baseDir.getAbsolutePath() + " is not a directory");
		}
		
		readData();
	}

	/**
	 * Read in all data from base directory
	 */
	private void readData() {
		File[] files = baseDir.listFiles();
		for(int i=0; i<files.length; i++) {
			if (files[i].getName().endsWith(".gvf") && files[i].getName().startsWith("omim")) {
				try {
					readVariantsFromFile(files[i]);
				}  catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void readVariantsFromFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		while (line != null) {
			if (line.length()==0) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			String contig = toks[0].replace("chr", "");
			Integer start = Integer.parseInt(toks[3]);
			Integer end = Integer.parseInt(toks[4]);
			String idStr = toks[8].replace("ID=", "").replace(";","");
			
			VariantRec rec = new VariantRec(contig, start, end, "-", "-", 0.0, false);
			rec.addAnnotation(VariantRec.OMIM_ID, idStr);
			addRecordNoSort(rec);
			line = reader.readLine();
		}
	}
}
