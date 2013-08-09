package buffer.variant;

/**
 * Adds additional annotations from a tokenized line to a variant record
 * @author brendan
 *
 */
public class CSVAnnotator {

	//Now we look for gene name as 10th line 
	public void addAnnotations(VariantRec rec, String[] toks) {
		if (toks.length > 9) {
			String gene = toks[9];
			rec.addAnnotation(VariantRec.GENE_NAME, gene);
		}
	}
}
