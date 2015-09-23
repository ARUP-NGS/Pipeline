package operator.variant;

import buffer.variant.VariantRec;

/**
 * This annotator computes the ESP 6500 exomes frequency, and adds it as the VariantRec.EXOMES_FREQ
 * annotation. The raw data is looked up in a VCF file that can be obtained from :
 * http://evs.gs.washington.edu/EVS/
 * The VCF files (one for each chromosome) must be merged into a single VCF, and then tabix compressed and 
 * indexed.
 * @author brendan
 *
 */
public class ESP6500Annotator extends AbstractTabixAnnotator {

	public static final String ESP_PATH = "esp.path";


	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(ESP_PATH);
	}

	protected boolean addAnnotationsFromString(VariantRec var, String dbline, int altIndex) {
		String[] toks = dbline.split("\t");
		String[] infoToks = toks[7].split(";");
				
		Double totOverall = 0.0;
		Double homOverall = 0.0;
		
		int homRefIndex = -1;
		int hetIndex = -1;
		int homAltIndex = -1;
		
		//Start by getting the indexes of our allele combinations of interest. We will use these indexes to pull the freqs from EA_GTC etc.
		for(int i=0; i<infoToks.length; i++) {
			String tok = infoToks[i];
			if (tok.startsWith("GTS")) {
				tok = tok.replace("GTS=", "");
				String[] vals = tok.split(",");
				//We just want to grab the index we are looking for.
				
				String altString = "A" + String.valueOf(altIndex);
				homRefIndex = getGTSIndex(vals, "RR");
				hetIndex = getGTSIndex(vals, altString+"R"); // i.e. A1R
				homAltIndex = getGTSIndex(vals, altString+altString); // i.e. A1A1
			}
		}
		
		for(int i=0; i<infoToks.length; i++) {
			String tok = infoToks[i];
			if (tok.startsWith("MAF=")) {
				tok = tok.replace("MAF=", "");
				String[] vals = tok.split(",");
				try {
					Double maf = Double.parseDouble(vals[vals.length-1]);
					Double mafEA = Double.parseDouble(vals[0]);
					Double mafAA = Double.parseDouble(vals[1]);
					var.addProperty(VariantRec.EXOMES_FREQ, maf/100);
					var.addProperty(VariantRec.EXOMES_FREQ_EA, mafEA/100);
					var.addProperty(VariantRec.EXOMES_FREQ_AA, mafAA/100);
				}
				catch(NumberFormatException ex) {
					//Don't worry about it, no annotation though
				}
			}
			//KB fix mutli-alts
			//Do we want to use these three columns in the case of a non homozygous different alt.
			
			if (tok.startsWith("EA_GTC=")) {
				tok = tok.replace("EA_GTC=", "");
				String[] vals = tok.split(",");
				try {
					
					Double homRef = Double.parseDouble(vals[homRefIndex]);
					Double het = Double.parseDouble(vals[hetIndex]);
					Double homAlt = Double.parseDouble(vals[homAltIndex]);
					
					double tot = homRef + het + homAlt;
					var.addProperty(VariantRec.EXOMES_EA_HOMREF, homRef / tot);
					var.addProperty(VariantRec.EXOMES_EA_HET, het/tot);
					var.addProperty(VariantRec.EXOMES_EA_HOMALT, homAlt/ tot);
					totOverall += tot;
					homOverall += homAlt;
				}
				catch(NumberFormatException ex) {
					//Don't worry about it, no annotation though
				}
			}

			if (tok.startsWith("AA_GTC=")) {
				tok = tok.replace("AA_GTC=", "");
				String[] vals = tok.split(",");
				try {
					Double homRef = Double.parseDouble(vals[homRefIndex]);
					Double het = Double.parseDouble(vals[hetIndex]);
					Double homAlt = Double.parseDouble(vals[homAltIndex]);
					
					double tot = homRef + het + homAlt;
					var.addProperty(VariantRec.EXOMES_AA_HOMREF, homRef / tot);
					var.addProperty(VariantRec.EXOMES_AA_HET, het/tot);
					var.addProperty(VariantRec.EXOMES_AA_HOMALT, homAlt/ tot);
					totOverall += tot;
					homOverall += homAlt;
				}
				catch(NumberFormatException ex) {
					//Don't worry about it, no annotation though
				}
			}
		}

		if (totOverall > 0) {
			var.addProperty(VariantRec.EXOMES_HOM_FREQ, homOverall / totOverall);
		} else {
			var.addProperty(VariantRec.EXOMES_HOM_FREQ, 0.0);	
		}

		return true;
	}

	
	/** Given the GTS string this will find the index of the query string. This index will be used to access the correct frequency in
	 * other info fields.
	 * 
	 * @param GTS
	 * @param combination
	 * @return
	 */
	private int getGTSIndex(String[] GTS, String combination) {
		for(int i =0; i < GTS.length; i++) {
			if ( GTS[i].equals(combination) ) {
				return i;
			}
		}
		return -1;
	}
}
