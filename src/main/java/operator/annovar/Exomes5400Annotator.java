package operator.annovar;

import java.io.File;
import java.io.IOException;

import operator.OperationFailedException;
import buffer.variant.FileAnnotator;
import buffer.variant.VariantRec;

/**
 * Annotates variants using the 5400 exomes database 
 * @author brendan
 *
 */

public class Exomes5400Annotator extends AnnovarAnnotator {

	protected double threshold = 0.0;
	

	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);
		
		String command = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype generic --score_threshold " + threshold + " -genericdbfile hg19_esp5400_all.txt --buildver " + buildVer + " " + annovarInputFile.getAbsolutePath() + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";

		executeCommand(command);
		
		File resultsFile = new File(annovarPrefix + ".hg19_generic_dropped");
		FileAnnotator annotator = new FileAnnotator(resultsFile, VariantRec.EXOMES_FREQ, 1, 5, 6, variants);
		try {
			annotator.annotateAll();
		} catch (IOException e) {
			throw new OperationFailedException("Error occurred during 1000G annotation: " + e.getMessage(), this);
		}
		resultsFile.deleteOnExit();
	}
}
