package operator.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.NodeList;

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

	public static final String SOLR_ADDR = "solr.address";
	public static final String ANNOTATIONS = "annotations";
	
    /* Annotation keys defined in by pipeline. The user may specify these
	 * or those defined in SOLR itself.
	 * 
	 * The corresponding SOLR field name is commented beside.
	 */
	private static final String PIPE_POP_FREQUENCY = "pop.freq";                 // == G1000_freq
	private static final String PIPE_AMR_FREQUENCY = "amr.freq";                 // == AMR_freq
	private static final String PIPE_EUR_FREQUENCY = "eur.freq";                 // == EUR_freq
	private static final String PIPE_AFR_FREQUENCY = "afr.freq";                 // == AFR_freq
	private static final String PIPE_ASN_FREQUENCY = "asn.freq";                 // == ASN_freq
	private static final String PIPE_SIFT_SCORE = "sift.score";                  // == sift_score
	private static final String PIPE_POLYPHEN_SCORE = "pp.score";                // == Polyphen2_HDIV_score
	private static final String PIPE_POLYPHEN_HVAR_SCORE = "pp.hvar.score";      // == Polyphen2_HVAR_score from DBNSF:
	private static final String PIPE_MT_SCORE = "mt.score";                      // == MutationTaster_score
	private static final String PIPE_GERP_NR_SCORE = "gerp.nr.score";            // == GERP_NR
	private static final String PIPE_GERP_SCORE = "gerp.score";                  // == GERP_RS
	private static final String PIPE_SLR_TEST = "slr.score";                     // == SLR_test_statistic
	private static final String PIPE_PHYLOP_SCORE = "phylop.score";              // == phyloP
	private static final String PIPE_LRT_SCORE = "lrt.score";                    // == LRT_score
	private static final String PIPE_SIPHY_SCORE = "siphy.score";                // == way29_logOdds
	private static final String PIPE_RSNUM = "rsnum";                            // == rs from dbSNP
	private static final String PIPE_EXOMES_FREQ = "exomes5400.frequency";       // == MAFinPercent_EA_AA_All (ESP_Exomes)
	private static final String PIPE_HGMD_HIT = "hgmd.hit";                      // == if 'type' == 'DM' or 'DM?'
	private static final String PIPE_HOTSPOT_ID = "hotspot.id";                  // == mutation_id from COSMIC
	
	/* SOLR keys */
	private static final String SOLR_G1000_Freq = "G1000_freq";
	private static final String SOLR_AMR_Freq = "AMR_freq";
	private static final String SOLR_EUR_Freq = "EUR_freq";
	private static final String SOLR_AFR_Freq = "AFR_freq";
	private static final String SOLR_ASN_Freq = "ASN_freq";
	private static final String SOLR_SIFT_SCORE = "sift_score";
	private static final String SOLR_POLYPHEN_SCORE = "Polyphen2_HDIV_score";
	private static final String SOLR_POLYPHEN_HVAR_SCORE = "Polyphen2_HVAR_score";
	private static final String SOLR_MT_SCORE = "MutationTaster_score";
	private static final String SOLR_GERP_NR_SCORE = "GERP_NR";
	private static final String SOLR_GERP_SCORE = "GERP_RS";
	private static final String SOLR_SLR_TEST = "SLR_test_statistic";
	private static final String SOLR_PHYLOP_SCORE = "phyloP";
	private static final String SOLR_LRT_SCORE = "LRT_score";
	private static final String SOLR_SIPHY_SCORE = "way29_logOdds";
	private static final String SOLR_RSNUM = "rs";
	private static final String SOLR_EXOMES_FREQ = "MAFinPercent_EA_AA_All";
	private static final String SOLR_HGMD_HIT = "hgmd.hit";
	private static final String SOLR_HOTSPOT_ID = "mutation_id";
	

	/* Define lists of pipeline keys and solr keys in the order they should
	 * be mapped (e.g., PIPE_POP_FREQUENCY is the same as SOLR_G1000_Freq.
	 */
	private static final List<String> pipelineKeys = Arrays.asList(PIPE_POP_FREQUENCY, PIPE_AMR_FREQUENCY, PIPE_EUR_FREQUENCY,
			PIPE_AFR_FREQUENCY, PIPE_ASN_FREQUENCY, PIPE_SIFT_SCORE, PIPE_POLYPHEN_SCORE, PIPE_POLYPHEN_HVAR_SCORE,
			PIPE_MT_SCORE, PIPE_GERP_NR_SCORE, PIPE_GERP_SCORE, PIPE_SLR_TEST, PIPE_PHYLOP_SCORE, PIPE_LRT_SCORE,
			PIPE_SIPHY_SCORE, PIPE_RSNUM, PIPE_EXOMES_FREQ, PIPE_HGMD_HIT, PIPE_HOTSPOT_ID);
	
	private static final List <String> solrKeys = Arrays.asList(SOLR_G1000_Freq, SOLR_AMR_Freq, SOLR_EUR_Freq,
			SOLR_AFR_Freq, SOLR_ASN_Freq, SOLR_SIFT_SCORE, SOLR_POLYPHEN_SCORE, SOLR_POLYPHEN_HVAR_SCORE, SOLR_MT_SCORE,
			SOLR_GERP_NR_SCORE, SOLR_GERP_SCORE, SOLR_SLR_TEST, SOLR_PHYLOP_SCORE, SOLR_LRT_SCORE, SOLR_SIPHY_SCORE,
			SOLR_RSNUM, SOLR_EXOMES_FREQ, SOLR_HGMD_HIT, SOLR_HOTSPOT_ID);
	

	/* Create maps from pipeline keys to solr keys and vice-versa */
    private static final Map<String, String> pipeToSOLRMap = createImmutableMap(pipelineKeys, solrKeys);
    private static final Map<String, String> SOLRToPipeMap = createImmutableMap(solrKeys, pipelineKeys);

    /* Default solrAddress */
	protected String solrAddress = "http://genseqac01.aruplab.net:8080";
	
	private List<VariantRec> queuedVars = new ArrayList<VariantRec>();
	private int batchSize = 5;

	/* Set default field keys, if not provided in template */
	private List<String> fieldKeys = solrKeys;

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
				JSONArray queryJSONDocs = performMultiQuery(queuedVars, fieldKeys);
				
				//Iterate over returned documents, for each one see if a) it has the field we're looking for
				//and b) if it has an exact match in the variants list we're dealing with. 
				for(int i=0; i<queryJSONDocs.length(); i++) {
					JSONObject doc = queryJSONDocs.getJSONObject(i);
					for(String field : fieldKeys){
		                if (doc.has(field)) {
		                    VariantRec tmpVar = findVariant(queuedVars, doc.getString("chr"), doc.getInt("pos"), doc.getString("ref"), doc.getString("alt"));
		                    if (tmpVar != null) {

		                    	/* Add annotation, but must add using the expected pipeline key */
		                        tmpVar.addAnnotation(SOLRToPipeMap.get(field), doc.get(field).toString());
		                    }
		                }
					}
				}
				
			} catch (IOException e) {
				throw new OperationFailedException("Error annotating variants: " + e.getMessage(), this);
			} catch (JSONException e) {
				throw new OperationFailedException("Error annotating variants: " + e.getMessage(), this);
			}
			queuedVars.clear();
		}

		queuedVars.add(var);
	}
	
	
	public void initialize(NodeList children){
		super.initialize(children);
		
		/* Get theh fields to annotate from the template and
		 * replace pipeline annotation keys with the appropriate
		 * SOLR field names if necessary.
		 */
		String fieldKeysString = properties.get(ANNOTATIONS);
		if(fieldKeysString != null){
            fieldKeys = new ArrayList<String>();
			String[] fields = fieldKeysString.split(",");
			
			for(String field : fields){
				
				/* If the defined field is a valid SOLR field,
				 * add it to fieldKeys
				 */
				if(solrKeys.contains(field)){
					fieldKeys.add(field);
				}
				/* If the defined field is a pipeline key,
				 * get the appropriate SOLR field
				 */
				else if(pipelineKeys.contains(field)){
					fieldKeys.add(pipeToSOLRMap.get(field));
				}
				else{
					throw new IllegalArgumentException("ERROR: Received invalid annotation field to SolrAnnotator!");
				}
			}
		}
		
		//User can override solr address
		String solrAddrString = properties.get(SOLR_ADDR);
		if (solrAddrString != null) {
			solrAddress = solrAddrString;
		}
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
				
				performMultiQuery(tmpVars, fieldKeys);
				
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
	
	/**
	 * Perform query on vars and return chr, pos, ref, alt, and all fieldKeys
	 * @param vars
	 * @param fieldKeys
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	protected JSONArray performMultiQuery(List<VariantRec> vars, List<String> fieldKeys) throws IOException, JSONException {
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
		String url = solrAddress + "/solr/query?q=chr:" + contig + "%20AND%20pos:(";
		for(VariantRec var : vars) {
			url = url + var.getStart() + "%20OR%20";
		}
		url = url.substring(0, url.length()-8) + ")&fl=chr,pos,ref,alt," + StringUtils.join(fieldKeys, ',');
		
		//Query solr, get response as a string
		String response = HttpUtils.HttpGet(url);

		Date end = new Date();
		long elapsedms = end.getTime() - start.getTime();
		System.err.println("Elaspsed ms for query: " + elapsedms);
		JSONObject json = new JSONObject(response);
		JSONObject responseJson = json.getJSONObject("response");
		JSONArray docs = responseJson.getJSONArray("docs");
		return docs;
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

		String url = solrAddress + "/solr/query?q=chr:" + var.getContig() + "%20AND%20pos:" + var.getStart();

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
	
    /**
     * Create an immutable map from list1 to list2
     * @return
     * @throws OperationFailedException 
     */
    private static Map<String, String> createImmutableMap(List<String> list1, List<String> list2) {
        Map<String, String> map = new HashMap<String, String>();
        
        if(list1.size() != list2.size()){
        	throw new RuntimeException("ERROR: cannot make map between lists of unequal sizes!");
        }
        
        for(int i = 0; i < list1.size(); i++){
        	map.put(list1.get(i), list2.get(i));
        }

        return Collections.unmodifiableMap(map);
    }

}
