package operator.variant;

import buffer.variant.VariantRec;

public class MITOMAPcoding extends AbstractTabixAnnotator {

	/**
	 *This annotator works on the MitoMap_coding.csv.gz file, adding disease phenotype to mitochondrial SNPs/Indels
	 * 
	 * 
	 * IN ORDER FOR THIS TO WORK USING THE TABIX ANNOTATOR:
	 * -----the MitoMap_coding.csv file had to be manually edited to resemble a VCF file format (then bgzipped and tabixed)
	 * 
	 * author chrisk
	 */
	
	
	public static final String MITOMAP_PATH = "MITOMAP.coding.path";
	
	
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
			System.out.println(disease);
			
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
