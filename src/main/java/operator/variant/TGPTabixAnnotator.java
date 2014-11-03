package operator.variant;

import operator.OperationFailedException;
import buffer.variant.VariantRec;

/**
 * Provides several 1000-Genomes based annotations, using the new 11/23/2010, version 3 calls
 * In contrast to previous 1000 Genomes annotator which parsed Annovar output, this uses 
 * a tabix-indexed "sites" file usually downloaded directly from : 
 * ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/release/20110521/ALL.wgs.phase1_release_v3.20101123.snps_indels_sv.sites.vcf.gz
 * to produce the annotations.
 * 
 * This new version extends from AbstractTabixAnnotator, which handles initialization of the tabix reader,
 * normalizaton of the variants, and some error checking. 
 * 
 * @author brendan
 *
 */
public class TGPTabixAnnotator extends AbstractTabixAnnotator {

	public static final String TGP_SITES_PATH = "tgp.sites.path";
		
	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(TGP_SITES_PATH);
	}
	
	/**
	 * Parses several frequency-based annotations from the given string and 
	 * converts them to annotations (properties, actually) on the variant
	 * @param var The VariantRec to annotate
	 * @param str The VCF line corresponding to the variant - this gets parsed to find the annotations we want to attach to the string 
	 * @throws OperationFailedException
	 */
	protected boolean addAnnotationsFromString(VariantRec var, String str) {
		String[] toks = str.split("\t");
				
		//The 7th column is the info column, which looks a little like AF=0.23;AF_AMR=0.123;AF_EUR=0.456...
		String[] infoToks = toks[7].split(";");
		String overallFreqStr = valueForKey(infoToks, "AF");
		if (overallFreqStr != null) {
			Double freq = Double.parseDouble(overallFreqStr);
			var.addProperty(VariantRec.POP_FREQUENCY, freq);
		}
		
		
		String freqStr = valueForKey(infoToks, "AMR_AF");
		if (freqStr != null) {
			Double freq = Double.parseDouble(freqStr);
			var.addProperty(VariantRec.AMR_FREQUENCY, freq);
		}
		
		String afrFreqStr = valueForKey(infoToks, "AFR_AF");
		if (afrFreqStr != null) {
			Double freq = Double.parseDouble(afrFreqStr);
			var.addProperty(VariantRec.AFR_FREQUENCY, freq);
		}
		
		String eurFreqStr = valueForKey(infoToks, "EUR_AF");
		if (eurFreqStr != null) {
			Double freq = Double.parseDouble(eurFreqStr);
			var.addProperty(VariantRec.EUR_FREQUENCY, freq);
		}
		return true;
	}

	/**
	 * Given a list of INFO tokens (like AF=52, GT=67, AD=1324 ...), pull the one 
	 * with the key given (e.g. AD), extract the value, and return it as a string.  
	 * @param toks
	 * @param key
	 * @return
	 */
	private static String valueForKey(String[] toks, String key) {
		for(int i=0; i<toks.length; i++) {
			if (toks[i].startsWith(key)) {
				return toks[i].replace(key, "").replace("=", "").replace(";", "").trim();
			}
		}
		return null;
	}

	
}
