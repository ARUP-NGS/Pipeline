package json;

import java.util.List;

import buffer.variant.VariantRec;

public class AnnotatedVarsJsonConverter {

	@SuppressWarnings("unchecked")
	
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
			varObj.put(key, var.getProperty(key));
		}
		
		for(String key : ensureKeys) {
			if (! varObj.has(key)) {
				varObj.put(key, "");
			}
		}
		
		return varObj;
	}
	

}
