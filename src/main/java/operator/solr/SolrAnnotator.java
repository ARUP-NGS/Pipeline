package operator.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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

	private String fieldKey = "1000G_freq";
	private int varsProcessed = 0; //Track number of variants queried, for error checking
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		
		//In order to batch calls to solr, we add each incoming variant to a list
		//here, then only call solr once the list gets bigger than 'batchSize' items
		
		
		
		String firstChr = queuedVars.size() > 0 
							? queuedVars.get(0).getContig() 
							: null;
		
		//Perform a lookup if a) there are some variants that have been queued AND
		// b) the current variant's contig doesn't match the one in the vars list OR
		//     
		if (queuedVars.size() > 0
				&& (queuedVars.size() >= batchSize 
				|| (!firstChr.equals(var.getContig())))) {
			try {
				//Actually perform lookup
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

		queuedVars.add(var);
	}
	
	/**
	 * Important! QueuedVars may still not be empty, in which case we need to do the last
	 * few variants
	 */
	protected void cleanup() throws OperationFailedException {
		
		//Somewhat weird logic to make sure we group variants by contig 
		//before giving them to performMultiQuery
		while(queuedVars.size()>0) {
			String contig = queuedVars.get(0).getContig();
			
			//tmpVars will contain all vars whose contig matches the first variant in the list
			List<VariantRec> tmpVars = new ArrayList<VariantRec>();
			for(VariantRec var : queuedVars) {
				if (var.getContig().equals(contig)) {
					tmpVars.add(var);
				}
			}
			
			try {
				
				performMultiQuery(tmpVars, fieldKey);
				
			} catch (IOException e) {
				e.printStackTrace();
				throw new OperationFailedException("Error annotating variants: " + e.getLocalizedMessage(), this);
			} catch (JSONException e) {
				e.printStackTrace();
				throw new OperationFailedException("Error annotating variants: " + e.getLocalizedMessage(), this);
			}
			
			//Now remove from queuedVars all variants that we just used
			for(VariantRec var : tmpVars) {
				queuedVars.remove(var);
			}
		}
		
		if (varsProcessed != variants.size()) {
			throw new OperationFailedException("Somehow did not process all variants in pool, only " + varsProcessed + " of " + variants.size(), this);
		}
	}
	
	protected void performMultiQuery(List<VariantRec> vars, String fieldKey) throws IOException, JSONException {
		String contig = vars.get(0).getContig();
		varsProcessed += vars.size();
		//Error check: All vars must have the same contig
		for(int i=1; i<vars.size(); i++) {
			if (! contig.equals(vars.get(i).getContig())) {
				throw new IllegalArgumentException("All variants in batch must have the same contig (found " + contig + " and " + vars.get(i).getContig());
			}
		}
		
		System.err.println("Annotating " + vars.size() + " variants from contig " + contig);
		Date start = new Date();
		
		//Create the url, adding in all positions of variants
		String url = SOLR_ADDRESS + "/solr/query?q=chr:" + contig + "+pos:(";
		for(VariantRec var : vars) {
			url = url + var.getStart() + "%20OR%20";
		}
		url = url.substring(0, url.length()-8) + ")&fl=chr,pos,ref,alt," + fieldKey;
		
		//Query solr, get response as a string
		String response = HttpUtils.HttpGet(url);

		Date end = new Date();
		long elapsedms = end.getTime() - start.getTime();
		System.err.println("Elaspsed ms for query: " + elapsedms);
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
