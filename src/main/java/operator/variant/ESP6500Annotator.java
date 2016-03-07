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
	private boolean isYChromVariant        = false;
	String[] GTSStringArray = null;

	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(ESP_PATH);
	}

	private boolean hasHaploidCalls() {
		for(int j = 0; j < GTSStringArray.length; j++) {
			if (GTSStringArray[j].length() == 1) {
				return true;
			}
		}
		return false;
	}
	
	
	protected boolean addAnnotationsFromString(VariantRec var, String dbline, int altIndex) {
		String[] toks = dbline.split("\t");
		String[] infoToks = toks[7].split(";");
		System.out.println(var.toSimpleString());
		Double totOverall = 0.0;
		Double homOverall = 0.0;

		int homRefIndex = -1;
		int hetIndex = -1;
		int homAltIndex = -1;
		
		
		int haploidRefIndex = -1;		
		int haploidAltIndex = -1;
		
		//Start by getting the indexes of our allele combinations of interest. We will use these indexes to pull the freqs from EA_GTC etc.
		for(int i=0; i<infoToks.length; i++) {
			String tok = infoToks[i];
			if (tok.startsWith("GTS")) {
				String homRefString = null;
				String hetString    = null;
				String homAltString = null;
				
				String haploidRefString = null;
				String haploidAltString = null;
				
				tok = tok.replace("GTS=", "");
				GTSStringArray = tok.split(",");
				
				if (var.getContig().equals("Y")) { //Y Chrom.
					isYChromVariant = true;
				} else {
					//Grab the indexes for the homref, het, and homalt.
					homRefString = var.getRef() + var.getRef();
					hetString    = var.getAlt() + var.getRef();
					homAltString = var.getAlt() + var.getAlt();
				}
				
				if (tok.contains("R")) { //Overwrite strings because indels are special.
					String altString = "A" + String.valueOf(altIndex+1); //Base 1 ie A1 in DB, but altindex is 0 based.
					if (isYChromVariant) {
						haploidRefString = "R";
						haploidAltString = altString;
					} else {
						homRefString = "RR";
						hetString = altString+"R"; // i.e. A1R
						homAltString = altString+altString; // i.e. A1A1
					}
					
				} else if (hasHaploidCalls()) { //X or Y variant
					//Need to handle X chrom SNPs which could look like this (See GTS field):
					// X	154158158	rs371159191	T	C	.	PASS	DBSNP=dbSNP_138;EA_AC=1,6726;AA_AC=0,3835;TAC=1,10561;
					//MAF=0.0149,0.0,0.0095;
					//GTS=CC,CT,C,TT,T;
					//EA_GTC=0,0,1,2428,1870;AA_GTC=0,0,0,1632,571;GTC=0,0,1,4060,2441;
					
					// Y       14898094        rs13305774      A       G       .       PASS    DBSNP=dbSNP_121;EA_AC=1190,682;AA_AC=124,447;TAC=1314,1129;
					//MAF=36.4316,21.7163,46.2137;
					//GTS=G,A;
					//EA_GTC=1190,682;AA_GTC=124,447;GTC=1314,1129;DP=43;GL=USP9Y;CP=0.0;CG=1.7;AA=A;CA=.;EXOME_CHIP=no;GWAS_PUBMED=.;FG=NM_004654.3:intron;HGVS_CDNA_VAR=NM_004654.3:c.3152-43A>G;HGVS_PROTEIN_VAR=.;CDS_SIZES=NM_004654.3:7668;GS=.;PH=.;EA_AGE=.;AA_AGE=.

					
					hasHaploidObservations = true; //We observe a single variant called, this is only X or Y.
					haploidRefString = var.getRef();
					haploidAltString = var.getAlt();
				}
				
				//Collect the indexes.
				haploidRefIndex  = getGTSIndex(GTSStringArray, haploidRefString);
				haploidAltIndex = getGTSIndex(GTSStringArray, haploidAltString);
				
				homRefIndex = getGTSIndex(GTSStringArray, homRefString);
				hetIndex    = getGTSIndex(GTSStringArray, hetString);
				homAltIndex = getGTSIndex(GTSStringArray, homAltString);
				
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
					int total = getTotalCounts(vals);
					Double homRef;
					Double het;
					Double homAlt;

					if (isYChromVariant) {
						homRef = Double.parseDouble(vals[haploidRefIndex]);
						homAlt = Double.parseDouble(vals[haploidAltIndex]);	
					} else {
						homRef = Double.parseDouble(vals[homRefIndex]);
						homAlt = Double.parseDouble(vals[homAltIndex]);
						if (hasHaploidObservations) {
							homRef = homRef + Double.parseDouble(vals[haploidRefIndex]);
							homAlt = homAlt + Double.parseDouble(vals[haploidAltIndex]);
						}
						
						het = Double.parseDouble(vals[hetIndex]);
						var.addProperty(VariantRec.EXOMES_EA_HET, het/total);
					}

					var.addProperty(VariantRec.EXOMES_EA_HOMREF, homRef / total);
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
					int total = getTotalCounts(vals);
					
					Double homRef;
					Double het;
					Double homAlt;

					if (isYChromVariant) {
						homRef = Double.parseDouble(vals[haploidRefIndex]);
						homAlt = Double.parseDouble(vals[haploidAltIndex]);	
					} else {
						homRef = Double.parseDouble(vals[homRefIndex]);
						homAlt = Double.parseDouble(vals[homAltIndex]);
						if (hasHaploidObservations) {
							homRef = homRef + Double.parseDouble(vals[haploidRefIndex]);
							homAlt = homAlt + Double.parseDouble(vals[haploidAltIndex]);
						}
						
						het = Double.parseDouble(vals[hetIndex]);
						var.addProperty(VariantRec.EXOMES_AA_HET, het/total);
					}

					var.addProperty(VariantRec.EXOMES_AA_HOMREF, homRef / total);
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
	private int getTotalCounts(String[] infoField) {
		int sum = 0;
		for(int i =0; i < infoField.length; i++) {
			sum += Integer.valueOf(infoField[i]);
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
