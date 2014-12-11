package operator.variant;

import java.io.IOException;
import operator.OperationFailedException;
import buffer.variant.VariantRec;


/**
 * Provides allele frequency counts from the UK10K project (2010-2013)
 *
 * Uses a tabix-indexed "sites" file usually downloaded directly from : 
 * ftp://ngs.sanger.ac.uk/production/uk10k/UK10K_COHORT/REL-2012-06-02/UK10K_COHORT.20140722.sites.vcf.gz
 * to produce the annotations.
 * 
 * 
 * UK10K project, through genome-wide sequencing of 10,000 genomes, aims to provide sequence variation
 * information for future studies and uncover rare variants associated with disease, among other goals.
 * 
 * www.uk10k.org
 * 
 * @author brendan, edited by chrisk
 *
 */
public class UK10KAnnotator extends AbstractTabixAnnotator {

	public static final String UK10K_PATH = "UK10K.path";
	
	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(UK10K_PATH);
	}


	/**
	 * Parses allele frequency annotation from the given string and 
	 * converts it to a property on the variant
	 * @param var
	 * @param str
	 * @throws OperationFailedException
	 */

	@Override
	protected boolean addAnnotationsFromString(VariantRec var, String val) {
		String[] toks = val.split("\t");
	

		String[] formatToks = toks[7].split(";");
		String overallFreqStr = valueForKey(formatToks, "AF=");
		if (overallFreqStr != null) {
			Double af = Double.parseDouble(overallFreqStr);
			var.addProperty(VariantRec.UK10K_ALLELE_FREQ, af);
		}
		
		return true;
	}
	
	
	private static String valueForKey(String[] toks, String key) {
		for(int i=0; i<toks.length; i++) {
			if (toks[i].startsWith(key)) {
				return toks[i].replace(key, "").replace("=", "").replace(";", "").trim();
			}
		}
		return null;
	}

	
}


