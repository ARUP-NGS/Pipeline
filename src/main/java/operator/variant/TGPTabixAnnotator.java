package operator.variant;

import operator.OperationFailedException;
import buffer.variant.VariantRec;

/**
 * Provides several 1000-Genomes based annotations, using the new 11/23/2010, version 3 calls
 * In contrast to previous 1000 Genomes annotator which parsed Annovar output, this uses 
 * a tabix-indexed "sites" file usually downloaded directly from : 
 * ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/release/20110521/ALL.wgs.phase1_release_v3.20101123.snps_indels_sv.sites.vcf.gz
 * 
 * to produce the annotations.
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
	 * @param var
	 * @param str
	 * @throws OperationFailedException
	 */
	protected boolean addAnnotationsFromString(VariantRec var, String str) {
		String[] toks = str.split("\t");
				
		String[] formatToks = toks[7].split(";");
		String overallFreqStr = valueForKey(formatToks, "AF");
		if (overallFreqStr != null) {
			Double freq = Double.parseDouble(overallFreqStr);
			var.addProperty(VariantRec.POP_FREQUENCY, freq);
		}
		
		
		String freqStr = valueForKey(formatToks, "AMR_AF");
		if (freqStr != null) {
			Double freq = Double.parseDouble(freqStr);
			var.addProperty(VariantRec.AMR_FREQUENCY, freq);
		}
		
		String afrFreqStr = valueForKey(formatToks, "AFR_AF");
		if (afrFreqStr != null) {
			Double freq = Double.parseDouble(afrFreqStr);
			var.addProperty(VariantRec.AFR_FREQUENCY, freq);
		}
		
		String eurFreqStr = valueForKey(formatToks, "EUR_AF");
		if (eurFreqStr != null) {
			Double freq = Double.parseDouble(eurFreqStr);
			var.addProperty(VariantRec.EUR_FREQUENCY, freq);
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
