package operator.annovar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import buffer.variant.FileAnnotator;
import buffer.variant.VariantRec;

import operator.OperationFailedException;
import pipeline.PipelineObject;

/**
 * Use annovar to annotate the variants in a variant pool with sift scores
 * @author brendan
 *
 */
public class SiftAnnotator extends AnnovarAnnotator {

	protected double siftThreshold = 0.0;
	

	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);
		
		String command = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype avsift --buildver " + buildVer + " --sift_threshold " + siftThreshold + "  " + annovarInputFile.getAbsolutePath() + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";

		executeCommand(command);
		
		File siftFile = new File(annovarPrefix + ".hg19_avsift_dropped"); 
		FileAnnotator siftAnnotator = new FileAnnotator(siftFile, VariantRec.SIFT_SCORE, 1, 5, 6, variants);
		try {
			siftAnnotator.annotateAll();
		} catch (IOException e) {
			throw new OperationFailedException("Error occurred during sift annotation: " + e.getMessage(), this);
		}
	}
	
	
}
