package operator.variant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.HttpUtils;
import util.vcfParser.VCFParser.GTType;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class VariantUploader extends Operator {

	public static final String VARIANT_UPLOAD_URL = "variant.upload.url";
	public static final String SAMPLEID = "sampleID";
	public static final String ACCN = "accession";
	public static final String RESULTDTA = "resultableDTA";
	protected VariantPool variants = null;
	
	protected String uploadURL = null;
//	protected String uploadURL = "http://ngs-webapp-dev/Variant/UploadVariants";
//	protected String uploadURL = "http://localhost:9172/Variant/UploadVariants";
	protected final String success = "\"Success\"";
	protected String sampleId = "0";
	protected String accession = "";
	protected String resultDTA = "";
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
	
		if(variants == null) {
			throw new OperationFailedException("No variant pool specified", this);
		}
		
        List<VariantRec> vars;
		if(variants.size() == 0){
			vars = new ArrayList<VariantRec>();
		}
		else{
            vars = variants.toList();
		}
		JSONObject json = new JSONObject();
		try {        
			logger.info("Uploading " + variants.size() + " variants for sample " + sampleId);
			
			json.put("sample.id", sampleId);
			json.put("accession", accession);
			json.put("result", resultDTA);
		
			JSONArray list = new JSONArray();
			for(VariantRec r: vars) {
				JSONObject row = new JSONObject();
				row.put("chr", r.getContig());
				row.put("pos", r.getStart());
				row.put("ref", r.getRef());
				row.put("alt", r.getAlt());
				int count = 0;
				if (r.getZygosity() == GTType.HET) {
					count = 1;
				}
				else {
					count = 2;		
				}
				double varFreq = r.getProperty(VariantRec.VAR_DEPTH) / r.getProperty(VariantRec.DEPTH);
				row.put("AlleleFrequency", varFreq);
				row.put("AlleleCount", count);
				list.put(row);
			}
			
			json.put("variant.list", list);
			System.out.println("\n\n" + json + "\n\n");
			String result = HttpUtils.HttpPostJSON(uploadURL, json);
  					
			
			logger.info("Uploading " + vars.size() + " variants to " + uploadURL);

			if(!result.equals(success)){
				//Not clear if we should fail here or what.. should we continue with future operations even if we 
				//can't communicate with .NET?
				//	throw new OperationFailedException("Failed to post variant list to .NET service: " + result, this);
				logger.warning("Error uploading variants : " + result);
				throw new OperationFailedException("ERROR: Failed to upload a JSON list of variants to '" + uploadURL + "'", this);

			} else {
				logger.info("Uploading " + vars.size() + " variants to " + uploadURL);	
			}
		} catch (NumberFormatException e){
			throw new OperationFailedException("Failed to upload a JSON list of variants (NumberFormatException) (SampleID=" + sampleId + ": " + e.getMessage(), this);
		} catch (JSONException e) {
			throw new OperationFailedException("Failed to upload a JSON list of variants (JSONException): " + e.getMessage(), this);
		} catch (IOException e) {
			throw new OperationFailedException("Failed to upload a JSON list of variants (IOException): " + e.getMessage(), this);
		}
			
	}
	

	@Override
	public void initialize(NodeList children) {
		
		uploadURL = this.getAttribute(VARIANT_UPLOAD_URL);
		if (uploadURL == null) {
			uploadURL = this.getPipelineProperty(VARIANT_UPLOAD_URL);
		}
		if (uploadURL == null) {
			throw new IllegalArgumentException("VariantUploader requires variant.upload.url to be specified");
		}
		
		sampleId = this.getAttribute(SAMPLEID);
		accession = this.getAttribute(ACCN);
		resultDTA = this.getAttribute(RESULTDTA);
			
		if (sampleId == null || sampleId.length() == 0) {
			//If no sampleID specified, user must specify both accession and result dta
			if (accession == null || accession.length() == 0) {
				throw new IllegalArgumentException("VariantUploader requires the accession to be specified");
			}
			
			if (resultDTA == null || resultDTA.length() == 0) {
				throw new IllegalArgumentException("VariantUploader requires the resultableDTA to be specified");
			}
		}
		
		if (accession==null) {
			accession = "";
		}
		if (resultDTA==null) {
			resultDTA="";
		}
		
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
		
		if (variants == null) {
			throw new IllegalArgumentException("No variant pool specified!");
		}
	}

}
