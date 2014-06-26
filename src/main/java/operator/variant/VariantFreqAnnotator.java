package operator.variant;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import json.JSONTokener;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import util.HttpUtils;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class VariantFreqAnnotator extends Operator {
	protected VariantPool variants = null;
	protected String downloadURL = "http://localhost:9172/Variant/XXX";

	public VariantFreqAnnotator() {
		return;
	}
	
	/**
	 * If true, we write some progress indicators to system.out
	 * @return
	 */
	protected boolean displayProgress() {
		return false;
	}
	
	/**
	 * Annotate the VariantRec 'var' with all existing frequencies in 'freqs'.
	 * 'freqs' will have a bunch of frequencies for each analysis type the
	 * variant has frequencies for (e.g., exome, aortopathies, etc.).
	 * @param var
	 * @param freqs
	 * @throws JSONException
	 */
	private void annotateVariant(VariantRec var, JSONObject freqs) throws JSONException{
		@SuppressWarnings("rawtypes")
		Iterator freqKeys = freqs.keys();
		String freqKey;
		Double freq;
		while(freqKeys.hasNext()){
			freqKey = (String) freqKeys.next();
			freq = freqs.getDouble(freqKey);
			var.addAnnotation(freqKey + "-freq", freq.toString());
		}
	}

	@Override
	public void performOperation() throws OperationFailedException {
		DecimalFormat formatter = new DecimalFormat("#0.00");

		if (variants == null)
			throw new OperationFailedException("No variant pool specified", this);

		JSONObject json = new JSONObject();
		String result;
        JSONArray varFreqs;
        JSONObject jo, freqs;
        String chr, pos, ref, alt;
        String chrKey = "chr", posKey = "pos", refKey = "ref",
                freqsKey = "freqs", altKey = "alt";
        VariantRec rec;
		try {
			/*
			 * Post to .NET controller to get frequencies for all variants
			 * 
			 * TODO: what should the JSONOBject 'json' contain, if anything? What
			 * is the controller name to put in the 'downloadURL'
			 */
			result = HttpUtils.HttpPostJSON(downloadURL, json);
            varFreqs = new JSONArray(new JSONTokener(result));

            int tot = variants.size();
            int varsAnnotated = 0;
            
            /* For each variant record in varFreqs, get the chr,
             * pos, ref, alt, and list of frequencies for each
             * analysis type. Then lookup the appropriate variant
             * in the VariantPool and annotate it.
             */
            for(int i = 0; i < varFreqs.length(); i++){
                jo = varFreqs.getJSONObject(i);
                chr = jo.getString(chrKey);
                pos = jo.getString(posKey);
                ref = jo.getString(refKey);
                alt = jo.getString(altKey);
                freqs = jo.getJSONObject(freqsKey);
                
                /* lookup the variant record */
                rec = variants.findRecord(chr, Integer.parseInt(pos), ref, alt);
                
                annotateVariant(rec, freqs);
				varsAnnotated++;
				double prog = 100 * (double)varsAnnotated  / (double) tot;
				if (displayProgress() && varsAnnotated % 2000 == 0) {
					System.out.println("Annotated " + varsAnnotated + " of " + tot + " variants  (" + formatter.format(prog) + "% )");	
				}
            }
		} catch (IOException e) {
			throw new OperationFailedException("Failed to get variant frequencies from " + downloadURL + ": " + e.getMessage(), this);
		} catch (JSONException e) {
			throw new OperationFailedException("Error parsing variant frequencies: " + e.getMessage(), this);
		}
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VariantPool) {
					variants = (VariantPool)obj;
				}

			}
		}
	}

}
