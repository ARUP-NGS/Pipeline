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
	
	protected boolean addAnnotationsFromString(VariantRec var, String val) {
		String[] toks = val.split("\t");
		String[] infoToks = toks[7].split(";");
		
		Double totOverall = 0.0;
		Double homOverall = 0.0;
		
		for(int i=0; i<infoToks.length; i++) {
			String tok = infoToks[i];
			if (tok.startsWith("MAF=")) {
				tok = tok.replace("MAF=", "");
				String[] vals = tok.split(",");
				try {
					Double maf = Double.parseDouble(vals[vals.length-1]);
					Double mafEA = Double.parseDouble(vals[0]);
					Double mafAA = Double.parseDouble(vals[1]);
					var.addProperty(VariantRec.EXOMES_FREQ, maf);
					var.addProperty(VariantRec.EXOMES_FREQ_EA, mafEA);
					var.addProperty(VariantRec.EXOMES_FREQ_AA, mafAA);
				}
				catch(NumberFormatException ex) {
					//Don't worry about it, no annotation though
				}
			}
			if (tok.startsWith("EA_GTC=")) {
				tok = tok.replace("EA_GTC=", "");
				String[] vals = tok.split(",");
				try {
					Double homRef = Double.parseDouble(vals[vals.length-1]);
					Double het = Double.parseDouble(vals[1]);
					Double homAlt = Double.parseDouble(vals[0]);
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
					Double homRef = Double.parseDouble(vals[vals.length-1]);
					Double het = Double.parseDouble(vals[1]);
					Double homAlt = Double.parseDouble(vals[0]);
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

	
}
