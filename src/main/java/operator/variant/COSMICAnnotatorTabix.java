package operator.variant;

import java.io.IOException;

import operator.OperationFailedException;
import buffer.variant.VariantRec;

/**
 * 
 * Parses the tabixed CosmicCodingMuts.gz file for the Cosmic IDs and counts
 * 
 * @author brendan, edited by chrisk
 *
 */

public class COSMICAnnotatorTabix extends AbstractTabixAnnotator {

	public static final String COSMICDB_PATH = "cosmic.db.path";
	
	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(COSMICDB_PATH);
	}


	/**
	 * @param var
	 * @param str
	 * @throws OperationFailedException
	 */

	@Override
	protected boolean addAnnotationsFromString(VariantRec var, String val, int altIndex) {
		String[] toks = val.split("\t");
		String[] infoTok=toks[7].split(";");
		String IDTok = toks[2];
		String overallFreqStr = valueForKey(infoTok, "CNT=");
		if (overallFreqStr != null) {
			Double cnt = Double.parseDouble(overallFreqStr);
			var.addProperty(VariantRec.COSMIC_COUNT, cnt);
		}
		if( IDTok != null){
			var.addAnnotation(VariantRec.COSMIC_ID, IDTok);
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



