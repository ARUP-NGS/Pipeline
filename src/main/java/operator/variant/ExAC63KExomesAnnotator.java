package operator.variant;

import java.nio.file.Path;
import java.nio.file.Paths;

import buffer.variant.VariantRec;
import operator.OperationFailedException;

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
	
		String overallAlleleCount = valueForKeyAtIndex(infoToks, "AC_Adj", altIndex);
		String overallAlleleNumber = valueForKey(infoToks, "AN_Adj");

		Path p = Paths.get(searchForAttribute(EXAC_63K_PATH));
		String exacVersion = p.getFileName().toString().replace(".sites.vep.vcf.gz", "");
		var.addAnnotation(VariantRec.EXAC63K_VERSION, exacVersion);
		//Overall
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OVERALL_ALLELE_COUNT, overallAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OVERALL_ALLELE_NUMBER, overallAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OVERALL_HOM_COUNT, valueForKeyAtIndex(infoToks, "AC_Hom", altIndex), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_OVERALL_ALLELE_FREQ, overallAlleleCount, overallAlleleNumber);
		
		//African
		String africanAlleleCount = valueForKeyAtIndex(infoToks, "AC_AFR", altIndex);
		String africanAlleleNumber = valueForKey(infoToks, "AN_AFR");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_AFRICAN_ALLELE_COUNT, africanAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_AFRICAN_ALLELE_NUMBER, africanAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_AFRICAN_HOM_COUNT, valueForKeyAtIndex(infoToks, "Hom_AFR", altIndex), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_AFRICAN_ALLELE_FREQ, africanAlleleCount, africanAlleleNumber);


		//Latino
		String latinoAlleleCount = valueForKeyAtIndex(infoToks, "AC_AMR", altIndex);
		String latinoAlleleNumber = valueForKey(infoToks, "AN_AMR");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_LATINO_ALLELE_COUNT, latinoAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_LATINO_ALLELE_NUMBER, latinoAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_LATINO_HOM_COUNT, valueForKeyAtIndex(infoToks, "Hom_AMR", altIndex), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_LATINO_ALLELE_FREQ, latinoAlleleCount, latinoAlleleNumber);


		//East Asian
		String eastAsianAlleleCount = valueForKeyAtIndex(infoToks, "AC_EAS", altIndex);
		String eastAsianAlleleNumber = valueForKey(infoToks, "AN_EAS");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EASTASIAN_ALLELE_COUNT, eastAsianAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EASTASIAN_ALLELE_NUMBER, eastAsianAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EASTASIAN_HOM_COUNT, valueForKeyAtIndex(infoToks, "Hom_EAS", altIndex), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_EASTASIAN_ALLELE_FREQ, eastAsianAlleleCount, eastAsianAlleleNumber);



		//Finnish
		String finnishAlleleCount = valueForKeyAtIndex(infoToks, "AC_FIN", altIndex);
		String finnishAlleleNumber = valueForKey(infoToks, "AN_FIN");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EUR_FINNISH_ALLELE_COUNT, finnishAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EUR_FINNISH_ALLELE_NUMBER, finnishAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EUR_FINNISH_HOM_COUNT, valueForKeyAtIndex(infoToks, "Hom_FIN", altIndex), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_EUR_FINNISH_ALLELE_FREQ, finnishAlleleCount, finnishAlleleNumber);
	
	
		//Non-Finnish Europeans
		String europeanAlleleCount = valueForKeyAtIndex(infoToks, "AC_NFE", altIndex);
		String europeanAlleleNumber = valueForKey(infoToks, "AN_NFE");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_COUNT, europeanAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_NUMBER, europeanAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_EUR_NONFINNISH_HOM_COUNT, valueForKeyAtIndex(infoToks, "Hom_NFE", altIndex), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_FREQ, europeanAlleleCount, europeanAlleleNumber);


		//South Asian
		String southAsianAlleleCount = valueForKeyAtIndex(infoToks, "AC_SAS", altIndex);
		String southAsianAlleleNumber = valueForKey(infoToks, "AN_SAS");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_SOUTHASIAN_ALLELE_COUNT, southAsianAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_SOUTHASIAN_ALLELE_NUMBER, southAsianAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_SOUTHASIAN_HOM_COUNT, valueForKeyAtIndex(infoToks, "Hom_SAS", altIndex), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_SOUTHASIAN_ALLELE_FREQ, southAsianAlleleCount, southAsianAlleleNumber);


		//Other populations
		String otherAlleleCount = valueForKeyAtIndex(infoToks, "AC_OTH", altIndex);
		String otherAlleleNumber = valueForKey(infoToks, "AN_OTH");
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OTHER_ALLELE_COUNT, otherAlleleCount, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OTHER_ALLELE_NUMBER, otherAlleleNumber, 1.0);
		safeParseAndSetProperty(var, VariantRec.EXAC63K_OTHER_HOM_COUNT, valueForKeyAtIndex(infoToks, "Hom_OTH", altIndex), 1.0);
		safeSetCalcFreq(var, VariantRec.EXAC63K_OTHER_ALLELE_FREQ, otherAlleleCount, otherAlleleNumber);
		
		//We should also check if it is on the X chrom, and add hemi info..
		if (str.contains("AC_Hemi")) { //Has hemi info.
			safeParseAndSetProperty(var, VariantRec.EXAC63K_OVERALL_HEMI_COUNT, valueForKeyAtIndex(infoToks, "AC_Hemi", altIndex), 1.0);
			
			//populations
			safeParseAndSetProperty(var, VariantRec.EXAC63K_AFRICAN_HEMI_COUNT, valueForKeyAtIndex(infoToks, "Hemi_AFR", altIndex), 1.0);
			safeParseAndSetProperty(var, VariantRec.EXAC63K_LATINO_HEMI_COUNT, valueForKeyAtIndex(infoToks, "Hemi_AMR", altIndex), 1.0);
			safeParseAndSetProperty(var, VariantRec.EXAC63K_EASTASIAN_HEMI_COUNT, valueForKeyAtIndex(infoToks, "Hemi_EAS", altIndex), 1.0);
			safeParseAndSetProperty(var, VariantRec.EXAC63K_EUR_FINNISH_HEMI_COUNT, valueForKeyAtIndex(infoToks, "Hemi_FIN", altIndex), 1.0);
			safeParseAndSetProperty(var, VariantRec.EXAC63K_EUR_NONFINNISH_HEMI_COUNT, valueForKeyAtIndex(infoToks, "Hemi_NFE", altIndex), 1.0);
			safeParseAndSetProperty(var, VariantRec.EXAC63K_SOUTHASIAN_HEMI_COUNT, valueForKeyAtIndex(infoToks, "Hemi_SAS", altIndex), 1.0);
			safeParseAndSetProperty(var, VariantRec.EXAC63K_OTHER_HEMI_COUNT, valueForKeyAtIndex(infoToks, "Hemi_OTH", altIndex), 1.0);
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

	/**
	 * Given a list of INFO tokens (like AF=52, GT=67, AD=1324 ...), pull the one 
	 * with the key given (e.g. AD), extract the value, and return it as a string.  
	 * @param toks
	 * @param key
	 * @return
	 */
	private static String valueForKeyAtIndex(String[] toks, String key, int index) {
		for(int i=0; i<toks.length; i++) {
			if (toks[i].startsWith(key)) {
				String token = toks[i].replace(key, "").replace("=", "").replace(";", "").trim();
				return token.split(",")[index];
			}
		}
		return null;
	}
	
}
