package disease;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;

/**
 * Stores some information about diseases parsed from OMIM
 * @author brendan
 *
 */
public class DiseaseInfo {

	public static final String TITLE = "title";
	public static final String PHENOTYPES = "phenotypes";
	public static final String SYNONYMS = "synonyms";
	
	enum Inheritance { DOMINANT, RECESSIVE, XLINKED, UNKNOWN , BOTH };
	
	private String name = null;
	//private String[] synonyms = null;
	private List<String> phenotypes = new ArrayList<String>();
	private String omimID = null;
	private Inheritance inheritance = Inheritance.UNKNOWN;
		
	public DiseaseInfo(String id, JSONObject obj) throws JSONException {
		name = obj.getString(TITLE);
		omimID = id;
		
		try {
			JSONObject phenosObj = obj.getJSONObject(PHENOTYPES);
			Iterator phenotypeCat = phenosObj.keys();
			while(phenotypeCat.hasNext()) {
				String phenoCat = phenotypeCat.next().toString();
				JSONArray phenoObj = phenosObj.getJSONArray(phenoCat);
				if (phenoCat.contains("Inheritance") || phenoCat.contains("INHERITANCE")) {
					parseInheritance( phenoObj );
				}
				else {
					for(int i=0; i<phenoObj.length(); i++) {
						phenotypes.add(phenoObj.getString(i));
					}
				}
			}
		}
		catch(JSONException ex) {
			//not all diseases have phenotypes, that's ok
		}
	}

	/**
	 * Disease name
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * OMIM ID of disease
	 * @return
	 */
	public String getID() {
		return omimID;
	}
	
	
	/**
	 * Get phenotype list for this disease
	 * @return
	 */
	public List<String> getPhenotypes() {
		return phenotypes;
	}
	
	/**
	 * Return inferred inheritance pattern for this disease
	 * @return
	 */
	public String getInheritance() {
		return inheritance.toString();
	}
	
	/**
	 * Try to infer the inheritance pattern based on the description under the inheritance
	 * heading
	 * @param phenoObj
	 * @throws JSONException
	 */
	private void parseInheritance(JSONArray phenoObj) throws JSONException {
		inheritance = Inheritance.UNKNOWN;
		
		for(int i=0; i<phenoObj.length(); i++) {
			String str = phenoObj.getString(i);
			str = str.toLowerCase();
			if (str.contains("autosomal dominant")) {
				if (inheritance == Inheritance.UNKNOWN || inheritance == Inheritance.DOMINANT) {
					inheritance = Inheritance.DOMINANT;
				}
				else {
					inheritance = Inheritance.BOTH;
				}
			}
			
			if (str.contains("autosomal recessive")) {
				if (inheritance == Inheritance.UNKNOWN || inheritance == Inheritance.RECESSIVE) {
					inheritance = Inheritance.RECESSIVE;
				}
				else {
					inheritance = Inheritance.BOTH;
				}
			}
			
			if (str.contains("x-linked")) {
				if (inheritance == Inheritance.UNKNOWN || inheritance == Inheritance.XLINKED) {
					inheritance = Inheritance.XLINKED;
				}
				else {
					inheritance = Inheritance.BOTH;
				}
			}
			
			
		}
		
	}
	
}
