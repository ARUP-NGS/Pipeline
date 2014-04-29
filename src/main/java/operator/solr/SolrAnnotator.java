package operator.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import operator.OperationFailedException;
import operator.annovar.Annotator;
import util.HttpUtils;
import buffer.variant.VariantRec;

/**
 * This is just for testing purposes now
 * @author brendan
 *
 */
public class SolrAnnotator extends Annotator {

	protected String SOLR_ADDRESS = "http://genseqac01.aruplab.net:8080";
	
	private List<VariantRec> queuedVars = new ArrayList<VariantRec>();
	private int batchSize = 5;

	String fieldKey = "1000G_freq";
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		
		//In order to batch calls to solr, we add each incoming variant to a list
		//here, then only call solr once the list gets bigger than 'batchSize' items
		queuedVars.add(var);
		
		
		if (queuedVars.size()%batchSize==0) {
			try {
				//Acually do query
				performMultiQuery(queuedVars, fieldKey);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
			queuedVars.clear();
		}

		
	}
	
	/**
	 * Important! QueuedVars may still not be empty, in which case we need to do the last
	 * few variants
	 */
	protected void cleanup() throws OperationFailedException {
		if (queuedVars.size()>0) {
			try {
				performMultiQuery(queuedVars, fieldKey);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
			queuedVars.clear();
		}
	}
	
	protected void performMultiQuery(List<VariantRec> vars, String fieldKey) throws IOException, JSONException {
		String contig = vars.get(0).getContig();
		
		//Create the url, adding in all positions of variants
		String url = SOLR_ADDRESS + "/solr/query?q=chr:" + contig + "+pos:(";
		for(VariantRec var : vars) {
			url = url + var.getStart() + "%20OR%20";
		}
		url = url.substring(0, url.length()-8) + ")&fl=chr,pos,ref,alt," + fieldKey;
		
		//Query solr, get response as a string
		String response = HttpUtils.HttpGet(url);

		
		JSONObject json = new JSONObject(response);
		JSONObject responseJson = json.getJSONObject("response");
		JSONArray docs = responseJson.getJSONArray("docs");
		
		//Iterate over returned documents, for each once see if a) it has the field we're looking for
		//and b) if it has an exact match in the variants list we're dealing with. 
		for(int i=0; i<docs.length(); i++) {
			JSONObject doc = docs.getJSONObject(i);
			if (doc.has(fieldKey)) {
			VariantRec var = findVariant(vars, doc.getString("chr"), doc.getInt("pos"), doc.getString("ref"), doc.getString("alt"));
				if (var != null) {
					var.addAnnotation("pop.freq", doc.get(fieldKey).toString());
				}
			}
		}
		
	}
	
	protected static VariantRec findVariant(List<VariantRec> vars, String chr, int pos, String ref, String alt) {
		for(VariantRec var : vars) {
			if (var.getContig().equals( chr)
					&& var.getStart() == pos
					&& var.getRef().equals(ref)
					&& var.getAlt().equals(alt)) {
				return var;
			}
		}
		return null;
	}
	
	protected String performQuery(VariantRec var, String fieldKey) throws IOException, JSONException {

		String url = SOLR_ADDRESS + "/solr/query?q=chr:" + var.getContig() + "+pos:" + var.getStart();

		String response = HttpUtils.HttpGet(url);

		JSONObject json = new JSONObject(response);
		JSONObject responseJson = json.getJSONObject("response");
		JSONArray docs = responseJson.getJSONArray("docs");

		//find the document with the exact match to the variant we looked for...
		for(int i=0; i<docs.length(); i++) {
			JSONObject doc = docs.getJSONObject(i);
			if (var.getContig().equals( doc.get("chr"))
					&& var.getStart() == doc.getInt("pos")
					&& var.getRef().equals(doc.get("ref"))
					&& var.getAlt().equals(doc.get("alt"))) {
				//It's a match, so return the field we're looking for
				if (doc.has(fieldKey)) {
					String value = doc.get(fieldKey).toString();
					return value;
				}
				else {
					return null;
				}
				
			}
		}
		return null;

	}
}
