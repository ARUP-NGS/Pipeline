package operator.annovar;

import java.io.File;
import java.io.IOException;

import operator.OperationFailedException;
import buffer.variant.FileAnnotator;
import buffer.variant.VariantRec;

public class GERPAnnotator extends AnnovarAnnotator {
	
	protected double threshold = 0.0;
	

	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);
		
		String command = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype generic --buildver " + buildVer + " " + annovarInputFile.getAbsolutePath() + " --genericdbfile hg19_ljb_gerp++.txt --outfile " + annovarPrefix + " " + annovarPath + "humandb/";

		executeCommand(command);
		
		File file = new File(annovarPrefix + ".hg19_generic_dropped"); 
		FileAnnotator annotator = new FileAnnotator(file, VariantRec.GERP_SCORE, 1, 5, 6, variants);
		try {
			annotator.annotateAll();
		} catch (IOException e) {
			throw new OperationFailedException("Error occurred during gerp++ annotation: " + e.getMessage(), this);
		}
	}

}
