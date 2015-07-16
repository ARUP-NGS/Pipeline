package operator.variant;

import operator.OperationFailedException;
import buffer.variant.VariantRec;

/**
 * Provides several 63K Exomes-based annotations from
 * a tabix-indexed "sites" file usually downloaded directly from : 
 * ftp://ftp.broadinstitute.org/pub/ExAC_release/release0.2/ExAC.r0.2.sites.vep.vcf.gz
 * to produce the annotations.
 * Release 0.2
 * This new version extends from AbstractTabixAnnotator, which handles initialization of the tabix reader,
 * normalizaton of the variants, and some error checking. 
 * 
 * @author daniel
 *
 */
public class ExAC63KExomesAnnotator extends AbstractTabixAnnotator {

	public static final String EXAC_63K_PATH = "63k.db.path";
		
	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(EXAC_63K_PATH);
	}
	
	private boolean safeParseAndSetProperty(VariantRec var, String propertyKey, String valToParse, Double divisor) {
		if (valToParse==null) {
			return false;
		}
		
		try {
			Double freq = Double.parseDouble(valToParse)/divisor;
			if(!freq.equals(Double.NaN)) {
				var.addProperty(propertyKey, freq);
			}
		} catch (NumberFormatException nfe) {
			//Don't worry about it, just don't set the property
			return false;
		}
		
		return true;
	}
	
	/**
	 * Parses several frequency-based annotations from the given string and 
	 * converts them to annotations (properties, actually) on the variant
	 * @param var The VariantRec to annotate
	 * @param str The VCF line corresponding to the variant - this gets parsed to find the annotations we want to attach to the string 
	 * @throws OperationFailedException
	 */
	@Override
	protected boolean addAnnotationsFromString(VariantRec var, String str, int altIndex) {
		String[] toks = str.split("\t");
				
		//The 7th column is the info column, which looks a little like AF=0.23;AF_AMR=0.123;AF_EUR=0.456...
		String[] infoToks = toks[7].split(";");
		String overallFreqStr = valueForKey(infoToks, "AF");
		if (overallFreqStr != null) {
			Double freq = Double.parseDouble(overallFreqStr);
			var.addProperty(VariantRec.EXOMES_63K_FREQ, freq);
		}
		
		//Total number of chromosomes assessed
		Double numCalledAlleles = Double.parseDouble(valueForKey(infoToks, "AN"));
	
		//Total number of samples, apparently, with genotype 1/1, so two alleles for each one
		
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_HOM_FREQ, valueForKey(infoToks, "AC_Hom"), numCalledAlleles/2.0);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_HOM_COUNT, valueForKey(infoToks, "AC_Hom"), 1.0);
		
		//Total number of samples with genotype 0/1, so one allele for each
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_HET_FREQ, valueForKey(infoToks, "AC_Het"), numCalledAlleles);

		
		
		Double numCalledAFR = Double.parseDouble(valueForKey(infoToks, "AN_AFR"));
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_AFR_FREQ, valueForKey(infoToks, "AC_AFR"), numCalledAFR);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_AFR_HOM, valueForKey(infoToks, "Hom_AFR"), numCalledAFR);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_AFR_HET, valueForKey(infoToks, "Het_AFR"), numCalledAFR);
				
		
		//American
		
		Double numCalledAMR = Double.parseDouble(valueForKey(infoToks, "AN_AMR"));
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_AMR_FREQ, valueForKey(infoToks, "AC_AMR"), numCalledAMR);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_AMR_HOM, valueForKey(infoToks, "Hom_AMR"), numCalledAMR);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_AMR_HET, valueForKey(infoToks, "Het_AMR"), numCalledAMR);
		
		
		
		//East Asian
		
		Double numCalledEAS = Double.parseDouble(valueForKey(infoToks, "AN_EAS"));
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_EAS_FREQ, valueForKey(infoToks, "AC_EAS"), numCalledEAS);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_EAS_HOM, valueForKey(infoToks, "Hom_EAS"), numCalledEAS);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_EAS_HET, valueForKey(infoToks, "Het_EAS"), numCalledEAS);
		
		
		
		//Finnish
		
		Double numCalledFIN = Double.parseDouble(valueForKey(infoToks, "AN_FIN"));
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_FIN_FREQ, valueForKey(infoToks, "AC_FIN"), numCalledFIN);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_FIN_HOM, valueForKey(infoToks, "Hom_FIN"), numCalledFIN);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_FIN_HET, valueForKey(infoToks, "Het_FIN"), numCalledFIN);
		
		
		//Non-Finnish Europeans
		
		Double numCalledNFE = Double.parseDouble(valueForKey(infoToks, "AN_NFE"));
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_NFE_FREQ, valueForKey(infoToks, "AC_NFE"), numCalledNFE);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_NFE_HOM, valueForKey(infoToks, "Hom_NFE"), numCalledNFE);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_NFE_HET, valueForKey(infoToks, "Het_NFE"), numCalledNFE);
		
		
		//Other populations
		
		Double numCalledOTH = Double.parseDouble(valueForKey(infoToks, "AN_OTH"));
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_OTH_FREQ, valueForKey(infoToks, "AC_OTH"), numCalledOTH);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_OTH_HOM, valueForKey(infoToks, "Hom_OTH"), numCalledOTH);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_OTH_HET, valueForKey(infoToks, "Het_OTH"), numCalledOTH);
		
		
		//South Asian
		Double numCalledSAS = Double.parseDouble(valueForKey(infoToks, "AN_SAS"));
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_SAS_FREQ, valueForKey(infoToks, "AC_SAS"), numCalledSAS);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_SAS_HOM, valueForKey(infoToks, "Hom_SAS"), numCalledSAS);
		safeParseAndSetProperty(var, VariantRec.EXOMES_63K_SAS_HET, valueForKey(infoToks, "Het_SAS"), numCalledSAS);
		
		
		//Compute 'overall' het / hom frequency
		
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
