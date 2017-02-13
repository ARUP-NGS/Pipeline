package json;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.vcfParser.VCFParser.GTType;
import buffer.variant.VariantRec;

/**
 * Some tools to easily convert a VariantRec into a json object
 * @author brendan
 *
 */
public class AnnotatedVarsJsonConverter {
	
	//If set, all variants will 
	private List<String> ensureKeys = null;
	private Set<String> excludeKeys = new HashSet<String>(); //These annotations will not be included
	
	private String NO_VALUE = ""; //Characters written when no value is associated with an annotation
	
	/**
	 * When set, all json objects will specify this key, even if not every variant has
	 * a property or annotation associated with the key
	 * @param keys
	 */
	public void setKeys(List<String> keys) {
		this.ensureKeys = keys;
	}
	
	public void setExcludeKeys(List<String> excludes) {
		//excludeKeys = new HashSet<String>();
		for(String key : excludes) {
			this.excludeKeys.add(key);
		}
	}
	
	/**
	 * Obtain a json representation of the given variant 
	 * @param var
	 * @return
	 * @throws JSONException
	 */
	public JSONObject toJSON(VariantRec var) throws JSONException {
		JSONObject varObj = new JSONObject();
		
		for(String key : var.getAnnotationKeys()) {
			if (!excludeKeys.contains(key)) {
				if (key.equals(VariantRec.GENOTYPE_QUALITY) && var.getAnnotation(key).equals(".")) {
					varObj.put(key, "0");
				} else {
					varObj.put(key, var.getAnnotation(key));
				}
			}
		}
		
		for(String key : var.getJsonobjKeys()) {
			if (!excludeKeys.contains(key)) {
				varObj.put(key, var.getjsonProperty(key));
			}
		}
		
		for(String key : var.getIntKeys()) {
			if (!excludeKeys.contains(key)) {
				varObj.put(key, var.getPropertyInt(key));
			}
		}
		
		for(String key : var.getPropertyKeys()) {
			if (excludeKeys.contains(key)) {
				continue;
			}
			
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
			if (("" + val).equals(".")) {
				varObj.put(key, "0.0");
			}
			if (val.isInfinite() || val.isNaN()) {
				//extra-special case that pipey does not like
				if (("" + val).equals(".")) {
					varObj.put(key, "0.0");
				} else {
					varObj.put(key, "" + val);
				}
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
		
		//Variant frequency defaults to 0, but gets computed from the DEPTH and VAR_DEPTH properties, if available
		double varFreq = 0.0;
		if (var.getProperty(VariantRec.DEPTH) != null && var.getProperty(VariantRec.DEPTH) > 0 && var.getProperty(VariantRec.VAR_DEPTH) != null) {
			varFreq = var.getProperty(VariantRec.VAR_DEPTH) / var.getProperty(VariantRec.DEPTH);
		}
		varObj.put("var.freq", varFreq);

		String zyg = "";
		if (var.getZygosity() == GTType.HET) {
			zyg = "het";
		} else if (var.getZygosity() == GTType.HOM) {
			zyg = "hom";
		} else if (var.getZygosity() == GTType.HEMI) {
			zyg = "hemi";
		} else if (var.getZygosity() == GTType.UNKNOWN) {
			zyg = "unknown";
		}
		varObj.put("zygosity", zyg);
		
		//a pipey special fix
		if (varObj.has(VariantRec.HGMD_HIT)) {
			if (varObj.getString(VariantRec.HGMD_HIT).length() > 5) {
				varObj.put(VariantRec.HGMD_HIT, "true");
			} else {
				varObj.put(VariantRec.HGMD_HIT, "");
			}
		}
		
		for(String key : ensureKeys) {
			if (! varObj.has(key)) {
				varObj.put(key, NO_VALUE);
			}
		}
		
		return varObj;
	}

	public String getNO_VALUE() {
		return NO_VALUE;
	}

	public void setNO_VALUE(String val) {
		NO_VALUE = val;
	}
	

}
