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
	
	private boolean safeSetCalcFreq(VariantRec var, String  propertyKey, String alleleCount, String alleleNumber) {
		if (alleleCount==null || alleleNumber==null) {
			return false;
		}
		
		try {
			Double freq = Double.parseDouble(alleleCount)/Double.parseDouble(alleleNumber);
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
	
		String overallAlleleCount = valueForKey(infoToks, "AC_Adj");
		String overallAlleleNumber = valueForKey(infoToks, "AN_Adj");

		//Overall
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OVERALL_ALLELE_COUNT, overallAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OVERALL_ALLELE_NUMBER, overallAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OVERALL_HOM_COUNT, valueForKey(infoToks, "AC_Hom"), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_OVERALL_ALLELE_FREQ, overallAlleleCount, overallAlleleNumber);
		
		
		/*
			String alleleCountKey  = "AC_" + pop;
			String alleleNumberKey = "AN_" + pop;
			String homCountKey     = "Hom_" + pop;
		 */
		
		//African
		String africanAlleleCount = valueForKey(infoToks, "AC_AFR");
		String africanAlleleNumber = valueForKey(infoToks, "AN_AFR");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_AFRICAN_ALLELE_COUNT, africanAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_AFRICAN_ALLELE_NUMBER, africanAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_AFRICAN_HOM_COUNT, valueForKey(infoToks, "Hom_AFR"), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_AFRICAN_ALLELE_FREQ, africanAlleleCount, africanAlleleNumber);


		//American
		String americanAlleleCount = valueForKey(infoToks, "AC_AMR");
		String americanAlleleNumber = valueForKey(infoToks, "AN_AMR");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_AMERICAN_ALLELE_COUNT, americanAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_AMERICAN_ALLELE_NUMBER, americanAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_AMERICAN_HOM_COUNT, valueForKey(infoToks, "Hom_AMR"), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_AMERICAN_ALLELE_FREQ, americanAlleleCount, americanAlleleNumber);


		//East Asian
		String eastAsianAlleleCount = valueForKey(infoToks, "AC_EAS");
		String eastAsianAlleleNumber = valueForKey(infoToks, "AN_EAS");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EASTASIAN_ALLELE_COUNT, eastAsianAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EASTASIAN_ALLELE_NUMBER, eastAsianAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EASTASIAN_HOM_COUNT, valueForKey(infoToks, "Hom_EAS"), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_EASTASIAN_ALLELE_FREQ, eastAsianAlleleCount, eastAsianAlleleNumber);


		//Finnish
		String finnishAlleleCount = valueForKey(infoToks, "AC_FIN");
		String finnishAlleleNumber = valueForKey(infoToks, "AN_FIN");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_FINNISH_ALLELE_COUNT, finnishAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_FINNISH_ALLELE_NUMBER, finnishAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_FINNISH_HOM_COUNT, valueForKey(infoToks, "Hom_FIN"), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_FINNISH_ALLELE_FREQ, finnishAlleleCount, finnishAlleleNumber);


		//Non-Finnish Europeans
		String europeanAlleleCount = valueForKey(infoToks, "AC_NFE");
		String europeanAlleleNumber = valueForKey(infoToks, "AN_NFE");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EUROPEAN_ALLELE_COUNT, europeanAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EUROPEAN_ALLELE_NUMBER, europeanAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EUROPEAN_HOM_COUNT, valueForKey(infoToks, "Hom_NFE"), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_EUROPEAN_ALLELE_FREQ, europeanAlleleCount, europeanAlleleNumber);


		//South Asian
		String southAsianAlleleCount = valueForKey(infoToks, "AC_SAS");
		String southAsianAlleleNumber = valueForKey(infoToks, "AN_SAS");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_SOUTHASIAN_ALLELE_COUNT, southAsianAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_SOUTHASIAN_ALLELE_NUMBER, southAsianAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_SOUTHASIAN_HOM_COUNT, valueForKey(infoToks, "Hom_SAS"), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_SOUTHASIAN_ALLELE_FREQ, southAsianAlleleCount, southAsianAlleleNumber);


		//Other populations
		String otherAlleleCount = valueForKey(infoToks, "AC_OTH");
		String otherAlleleNumber = valueForKey(infoToks, "AN_OTH");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OTHER_ALLELE_COUNT, otherAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OTHER_ALLELE_NUMBER, otherAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OTHER_HOM_COUNT, valueForKey(infoToks, "Hom_OTH"), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_OTHER_ALLELE_FREQ, otherAlleleCount, otherAlleleNumber);
		
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
