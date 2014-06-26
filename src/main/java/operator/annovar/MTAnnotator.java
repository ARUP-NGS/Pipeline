package operator.annovar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import operator.OperationFailedException;
import buffer.variant.FileAnnotator;
import buffer.variant.VariantRec;

public class MTAnnotator extends AnnovarAnnotator {

	protected double threshold = 0.0;
	

	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);
		
		String command = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_mt --buildver " + buildVer + "  " +  annovarInputFile.getAbsolutePath() + " --outfile " + annovarPrefix + " -score_threshold " + threshold + " " + annovarPath + "humandb/";

		executeCommand(command);
		
		File resultFile = new File(annovarPrefix + ".hg19_ljb_mt_dropped"); 
		FileAnnotator annotator = new FileAnnotator(resultFile, VariantRec.MT_SCORE, 1, 5, 6, variants);
		try {
			annotator.annotateAll();
		} catch (IOException e) {
			throw new OperationFailedException("Error occurred during polyphen annotation: " + e.getMessage(), this);
		}
		
	}
}