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
	private boolean hasHaploidObservations = false;
	String[] GTSStringArray = null;

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
				GTSStringArray = tok.split(",");
				//We just want to grab the index we are looking for.
				if (tok.contains("R")) { //Indels only do this.
					String altString = "A" + String.valueOf(altIndex+1); //Base 1 ie A1 in DB, but altindex is 0 based.
					homRefIndex = getGTSIndex(GTSStringArray, "RR");
					hetIndex = getGTSIndex(GTSStringArray, altString+"R"); // i.e. A1R
					homAltIndex = getGTSIndex(GTSStringArray, altString+altString); // i.e. A1A1
				} else { //Otherwise it is a SNP.
					//Need to handle X chrom SNPs which could look like this (Note GTS field):
					// X	154158158	rs371159191	T	C	.	PASS	DBSNP=dbSNP_138;EA_AC=1,6726;AA_AC=0,3835;TAC=1,10561;
					//MAF=0.0149,0.0,0.0095;GTS=CC,CT,C,TT,T;EA_GTC=0,0,1,2428,1870;AA_GTC=0,0,0,1632,571;GTC=0,0,1,4060,2441;
					homRefIndex = getGTSIndex(GTSStringArray, var.getRef() + var.getRef());
					hetIndex = getGTSIndex(GTSStringArray, var.getAlt() + var.getRef());
					homAltIndex = getGTSIndex(GTSStringArray, var.getAlt() + var.getAlt());
					for(int j = 0; j < GTSStringArray.length; j++) {
						if (GTSStringArray[j].length() == 1) {
							hasHaploidObservations = true; //We observe a single variant called, this should only be in X chrom for ESP6500.
							break;
						}
					}
				}
				break;
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

			if (tok.startsWith("EA_GTC=")) {
				tok = tok.replace("EA_GTC=", "");
				String[] vals = tok.split(",");
				try {
					int total = getTotalCounts(vals, hasHaploidObservations);
					Double homRef = Double.parseDouble(vals[homRefIndex]);
					Double het = Double.parseDouble(vals[hetIndex]);
					Double homAlt = Double.parseDouble(vals[homAltIndex]);

					var.addProperty(VariantRec.EXOMES_EA_HOMREF, homRef / total);
					var.addProperty(VariantRec.EXOMES_EA_HET, het/total);
					var.addProperty(VariantRec.EXOMES_EA_HOMALT, homAlt/ total);
					totOverall += total;
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
					int total = getTotalCounts(vals, hasHaploidObservations);
					Double homRef = Double.parseDouble(vals[homRefIndex]);
					Double het = Double.parseDouble(vals[hetIndex]);
					Double homAlt = Double.parseDouble(vals[homAltIndex]);

					var.addProperty(VariantRec.EXOMES_AA_HOMREF, homRef / total);
					var.addProperty(VariantRec.EXOMES_AA_HET, het/total);
					var.addProperty(VariantRec.EXOMES_AA_HOMALT, homAlt/ total);
					totOverall += total;
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


	/** Given a certain frequency count info field (list of comma seperated ints), this will calculate the sum of those counts. Somewhat takes into account
	 * X chromosome SNPs which sometimes have haploid observations by just ignoring these and only using diploid calls for the frequency calculation.
	 *
	 * @param infoField
	 * @return sum
	 */
	private int getTotalCounts(String[] infoField, boolean hasHaploidObservations) {
		int sum = 0;
		for(int i =0; i < infoField.length; i++) {
			if (!hasHaploidObservations) {
				sum += Integer.valueOf(infoField[i]);
			} else if (hasHaploidObservations && GTSStringArray[i].length() !=1) { //Only consider non haploid calls in the freq calc.
				sum += Integer.valueOf(infoField[i]);
			}
		}
		return sum;
	}

	/** Given the GTS string this will find the index of the query string. This index will be used to access the correct frequency in
	 * other info fields.
	 *
	 * @param GTS
	 * @param match
	 * @return
	 */
	private int getGTSIndex(String[] GTS, String match) {
		for(int i =0; i < GTS.length; i++) {
			if ( GTS[i].equals(match) ) {
				return i;
			}
		}
		return -1;
	}

	/* Would be nice to implement this as maps for each of these frequency fields of GTS to the given frequency line.
	private Map<String, Double> combineListsIntoOrderedMap (List<String> keys, List<Double> values) {
		if (keys.size() != values.size())
			throw new IllegalArgumentException ("GTS field and given frequency field do not contain the same number of elements.");
		Map<String,Double> map = new LinkedHashMap<String,Double>();
		for (int i=0; i<keys.size(); i++) {
			map.put(keys.get(i), values.get(i));
		}
		return map;
	}
	*/
}
