package operator.variant;

import buffer.variant.VariantRec;


public class MITOMAP_rRNAtRNA extends AbstractTabixAnnotator {

	/**
	 * This annotator works on the MM_rRNAtRNA.csv.gz file, adding disease phenotype to mitochondrial SNPs/Indels
	 * 
	 * 
	 * IN ORDER FOR THIS TO WORK USING THE TABIX ANNOTATOR:
	 * -----the MM_rRNAtRNA.csv file had to be manually edited to resemble a VCF file format (then bgzipped and tabixed)
	 * 
	 * what was done?
	 * 		CHR field was added, along with splitting of the "allele" (T3258C) field into POS-3258, REF-T, and ALT-C separate columns
	 * 		Next, the other columns were rearranged into the INFO field, and QUAL/FILTER columns were added (fields left blank)
	 * 		"disease=" was added to the beginning of the disease phenotype for clarity, and heterplasmy presence was changed to "heteroplasmy" (from "+")
	 * 
	 * 
	 * In the future, hopefully MitoMap.org will simply create VCFs, but for now manual editing is necessary
	 * 
	 * author chrisk
	 */

	public static final String MITOMAP_PATH = "MITOMAP.rRNAtRNA.path";
	
	
	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(MITOMAP_PATH);
	}

	@Override
	public boolean addAnnotationsFromString(VariantRec var, String val) {
		String[] toks = val.split("\t");
			String info = toks[7];
			String[] infoTok= info.split(";");
			String disease = valueForKey(infoTok, "disease=");

			
			if(disease !=null)
			{
				var.addAnnotation(VariantRec.MITOMAP_DIS_CODING, disease);
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
