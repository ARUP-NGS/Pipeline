package json;

import java.util.List;

import util.vcfParser.VCFParser.GTType;
import buffer.variant.VariantRec;

public class AnnotatedVarsJsonConverter {
	
	//If set, all variants will 
	private List<String> ensureKeys = null;
	
	//When set, all json objects will specify this key, even if not every variant has
	//a property or annotation associated with the key
	public void setKeys(List<String> keys) {
		this.ensureKeys = keys;
	}
	
	public JSONObject toJSON(VariantRec var) throws JSONException {
		JSONObject varObj = new JSONObject();
		
		for(String key : var.getAnnotationKeys()) {
			varObj.put(key, var.getAnnotation(key));
		}
		
		for(String key : var.getPropertyKeys()) {
			//See if we can parse an int first.
			try {
				int val = Integer.parseInt("" + var.getProperty(key));
				varObj.put(key, val);
				continue;
			}
			catch (NumberFormatException nex) {
				//Expected, ignore this
			}
			
			Double val = var.getProperty(key);
			//JSON can't handle infinite or NaN's, so convert to a string here
			if (val.isInfinite() || val.isNaN()) {
				varObj.put(key, "" + val);
			}
			else {
				varObj.put(key, var.getProperty(key));
			}
		}

		//A few special cases:
		varObj.put("chr", var.getContig());
		varObj.put("pos", var.getStart());
		varObj.put("quality", var.getQuality());
		varObj.put("ref", var.getRef());
		varObj.put("alt", var.getAlt());
		String zyg = "";
		if (var.isHetero() == GTType.HET) {
			zyg = "het";
		} else if (var.isHetero() == GTType.HOM) {
			zyg = "hom";
		} else if (var.isHetero() == GTType.HEMI) {
			zyg = "hemi";
		} else if (var.isHetero() == GTType.UNKNOWN) {
			zyg = "unknown";
		}
		varObj.put("zygosity", zyg);
		

		for(String key : ensureKeys) {
			if (! varObj.has(key)) {
				varObj.put(key, "");
			}
		}
		
		return varObj;
	}
	

}
