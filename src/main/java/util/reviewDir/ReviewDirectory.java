package util.reviewDir;

import java.io.File;
import java.io.IOException;

import buffer.VCFFile;
import buffer.variant.CSVLineReader;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantPool;

/**
 * Encapsulates a ReviewDIrectory and provides easy access to variants, manifest, etc.
 * @author brendan
 *
 */
public class ReviewDirectory {
	
	private File sourceDir;
	private SampleManifest manifest;
	
	public ReviewDirectory(String path) throws IOException, ManifestParseException {
		sourceDir = new File(path);
		if ((!sourceDir.exists()) || (!sourceDir.isDirectory())) {
			throw new IOException("Can't read source directory at " + sourceDir.getAbsolutePath());
		}
		
		manifest = SampleManifest.create(path);
	}

	public String getSampleName() {
		return manifest.getSampleName();
	}
	
	public VariantPool getVariantsFromVCF() throws IOException {
		VCFFile vcf = new VCFFile( manifest.getVCF());
		return new VariantPool(vcf);
	}
	
	public VariantPool getVariantsFromCSV() throws IOException {
		File csv =  manifest.getAnnotatedVars();
		VariantLineReader csvReader = new CSVLineReader(csv);
		return new VariantPool(csvReader);
	}
	
}
