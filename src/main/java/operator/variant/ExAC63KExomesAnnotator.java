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
			var.addProperty(VariantRec.EXOMES_63K_FREQ, freq);
		}
		
		Double NumCalledAlleles = Double.parseDouble(valueForKey(infoToks, "AN"));
	
		String homCountStr = valueForKey(infoToks, "AC_Hom");
		if(homCountStr != null) {
				Double freq = Double.parseDouble(homCountStr)/NumCalledAlleles;
				var.addProperty(VariantRec.EXOMES_63K_AC_HOM, freq);
		}

		String hetCountStr = valueForKey(infoToks, "AC_Het");
		if(hetCountStr != null) {
				Double freq = Double.parseDouble(hetCountStr)/NumCalledAlleles;
				var.addProperty(VariantRec.EXOMES_63K_AC_HET, freq);
		}

		//African
		
		Double NumCalledAFR = Double.parseDouble(valueForKey(infoToks, "AN_AFR"));
		
		String afrCountStr = valueForKey(infoToks, "AC_AFR");
		if (afrCountStr != null) {
			Double freq = Double.parseDouble(afrCountStr)/NumCalledAFR;
			var.addProperty(VariantRec.EXOMES_63K_AFR_FREQ, freq);
		}
		
		String afrHomStr = valueForKey(infoToks, "Hom_AFR");
		if (afrHomStr != null) {
			Double freq = Double.parseDouble(afrHomStr)/NumCalledAFR;
			var.addProperty(VariantRec.EXOMES_63K_AFR_HOM, freq);
		}
		
		String afrHetStr = valueForKey(infoToks, "Het_AFR");
		if (afrHetStr != null) {
			Double freq = Double.parseDouble(afrHetStr)/NumCalledAFR;
			var.addProperty(VariantRec.EXOMES_63K_AFR_HET, freq);
		}		
		
		//American
		
		Double NumCalledAMR = Double.parseDouble(valueForKey(infoToks, "AN_AMR"));
		
		String amrCountStr = valueForKey(infoToks, "AC_AMR");
		if (amrCountStr != null) {
			Double freq = Double.parseDouble(amrCountStr)/NumCalledAMR;
			var.addProperty(VariantRec.EXOMES_63K_AMR_FREQ, freq);
		}
		
		String amrHomStr = valueForKey(infoToks, "Hom_AMR");
		if (amrHomStr != null) {
			Double freq = Double.parseDouble(amrHomStr)/NumCalledAMR;
			var.addProperty(VariantRec.EXOMES_63K_AMR_HOM, freq);
		}
		
		String amrHetStr = valueForKey(infoToks, "Het_AMR");
		if (amrHetStr != null) {
			Double freq = Double.parseDouble(amrHetStr)/NumCalledAMR;
			var.addProperty(VariantRec.EXOMES_63K_AMR_HET, freq);
		}
		
		//East Asian
		
		Double NumCalledEAS = Double.parseDouble(valueForKey(infoToks, "AN_EAS"));
		
		String easCountStr = valueForKey(infoToks, "AC_EAS");
		if (easCountStr != null) {
			Double freq = Double.parseDouble(easCountStr)/NumCalledEAS;
			var.addProperty(VariantRec.EXOMES_63K_EAS_FREQ, freq);
		}
		
		String easHomStr = valueForKey(infoToks, "Hom_EAS");
		if (easHomStr != null) {
			Double freq = Double.parseDouble(easHomStr)/NumCalledEAS;
			var.addProperty(VariantRec.EXOMES_63K_EAS_HOM, freq);
		}
		
		String easHetStr = valueForKey(infoToks, "Het_EAS");
		if (easHetStr != null) {
			Double freq = Double.parseDouble(easHetStr)/NumCalledEAS;
			var.addProperty(VariantRec.EXOMES_63K_EAS_HET, freq);
		}
		
		//Finnish
		
		Double NumCalledFIN = Double.parseDouble(valueForKey(infoToks, "AN_FIN"));
		
		String finCountStr = valueForKey(infoToks, "AC_FIN");
		if (finCountStr != null) {
			Double freq = Double.parseDouble(finCountStr)/NumCalledFIN;
			var.addProperty(VariantRec.EXOMES_63K_FIN_FREQ, freq);
		}
		
		String finHomStr = valueForKey(infoToks, "Hom_FIN");
		if (finHomStr != null) {
			Double freq = Double.parseDouble(finHomStr)/NumCalledFIN;
			var.addProperty(VariantRec.EXOMES_63K_FIN_HOM, freq);
		}
		
		String finHetStr = valueForKey(infoToks, "Het_FIN");
		if (finHetStr != null) {
			Double freq = Double.parseDouble(finHetStr)/NumCalledFIN;
			var.addProperty(VariantRec.EXOMES_63K_FIN_HET, freq);
		}
		
		//Non-Finnish Europeans
		
		Double NumCalledNFE = Double.parseDouble(valueForKey(infoToks, "AN_NFE"));
		
		String nfeCountStr = valueForKey(infoToks, "AC_NFE");
		if (nfeCountStr != null) {
			Double freq = Double.parseDouble(nfeCountStr)/NumCalledNFE;
			var.addProperty(VariantRec.EXOMES_63K_NFE_FREQ, freq);
		}
		
		String nfeHomStr = valueForKey(infoToks, "Hom_NFE");
		if (nfeHomStr != null) {
			Double freq = Double.parseDouble(nfeHomStr)/NumCalledNFE;
			var.addProperty(VariantRec.EXOMES_63K_NFE_HOM, freq);
		}
		
		String nfeHetStr = valueForKey(infoToks, "Het_NFE");
		if (nfeHetStr != null) {
			Double freq = Double.parseDouble(nfeHetStr)/NumCalledNFE;
			var.addProperty(VariantRec.EXOMES_63K_NFE_HET, freq);
		}
	
		//Other populations
		
		Double NumCalledOTH = Double.parseDouble(valueForKey(infoToks, "AN_OTH"));
		
		String othCountStr = valueForKey(infoToks, "AC_OTH");
		if (othCountStr != null) {
			Double freq = Double.parseDouble(othCountStr)/NumCalledOTH;
			var.addProperty(VariantRec.EXOMES_63K_OTH_FREQ, freq);
		}
		
		String othHomStr = valueForKey(infoToks, "Hom_OTH");
		if (othHomStr != null) {
			Double freq = Double.parseDouble(othHomStr)/NumCalledOTH;
			var.addProperty(VariantRec.EXOMES_63K_OTH_HOM, freq);
		}
		
		String othHetStr = valueForKey(infoToks, "Het_OTH");
		if (othHetStr != null) {
			Double freq = Double.parseDouble(othHetStr)/NumCalledOTH;
			var.addProperty(VariantRec.EXOMES_63K_OTH_HET, freq);
		}
		
		//South Asian
		
		Double NumCalledSAS = Double.parseDouble(valueForKey(infoToks, "AN_SAS"));
		
		String sasCountStr = valueForKey(infoToks, "AC_SAS");
		if (sasCountStr != null) {
			Double freq = Double.parseDouble(sasCountStr)/NumCalledSAS;
			var.addProperty(VariantRec.EXOMES_63K_SAS_FREQ, freq);
		}
		
		String sasHomStr = valueForKey(infoToks, "Hom_SAS");
		if (sasHomStr != null) {
			Double freq = Double.parseDouble(sasHomStr)/NumCalledSAS;
			var.addProperty(VariantRec.EXOMES_63K_SAS_HOM, freq);
		}
		
		String sasHetStr = valueForKey(infoToks, "Het_SAS");
		if (sasHetStr != null) {
			Double freq = Double.parseDouble(sasHetStr)/NumCalledSAS;
			var.addProperty(VariantRec.EXOMES_63K_SAS_HET, freq);
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
