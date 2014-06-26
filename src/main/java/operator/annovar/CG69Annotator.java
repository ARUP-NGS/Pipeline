package operator.annovar;

import java.io.File;
import java.io.IOException;

import operator.OperationFailedException;
import buffer.variant.FileAnnotator;
import buffer.variant.VariantRec;

/**
 * Adds the Complete Genomics 69 genomes frequency (CG69) to variants
 * @author brendan
 *
 */
public class CG69Annotator extends AnnovarAnnotator {
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);
		
		String command = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype cg69 -maf 0 --buildver " + buildVer + " " + annovarInputFile.getAbsolutePath() + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";

		executeCommand(command);
		
		File resultsFile = new File(annovarPrefix + ".hg19_cg69_dropped");
		FileAnnotator annotator = new FileAnnotator(resultsFile, VariantRec.CG69_FREQUENCY, 1, 5, 6, variants);
		try {
			annotator.annotateAll();
		} catch (IOException e) {
			throw new OperationFailedException("Error occurred during CG69 annotation: " + e.getMessage(), this);
		}
		
	}

}
