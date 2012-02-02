package operator.annovar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import operator.OperationFailedException;
import buffer.variant.FileAnnotator;
import buffer.variant.VariantRec;

public class TKGAnnotator extends AnnovarAnnotator {

	protected double threshold = 0.0;
	

	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);
		
		String command = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype 1000g2010nov_all -maf 0 --buildver " + buildVer + " " + annovarInputFile.getAbsolutePath() + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";

		executeCommand(command);
		
		File resultsFile = new File(annovarPrefix + ".hg19_ALL.sites.2010_11_dropped");
		FileAnnotator annotator = new FileAnnotator(resultsFile, VariantRec.POP_FREQUENCY, 1, variants);
		try {
			annotator.annotateAll();
		} catch (IOException e) {
			throw new OperationFailedException("Error occurred during 1000G annotation: " + e.getMessage(), this);
		}
		
//		List<String> keys = new ArrayList<String>();
//		keys.add(VariantRec.POP_FREQUENCY);
//		keys.add(VariantRec.SIFT_SCORE);
//		keys.add(VariantRec.POLYPHEN_SCORE);
//		keys.add(VariantRec.MT_SCORE);
//		keys.add(VariantRec.PHYLOP_SCORE);
//		variants.listAll(System.out, keys);
	}
}
