package operator.variant;

import java.io.IOException;
import java.util.List;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import operator.OperationFailedException;
import operator.Operator;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.HttpUtils;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class VariantUploader extends Operator {

	public static final String VARIANT_UPLOAD_URL = "variant.upload.url";
	protected VariantPool variants = null;
	
	protected String uploadURL = "http://ngs-webapp-dev/Variant/UploadVariants";
//	protected String uploadURL = "http://localhost:9172/Variant/UploadVariants";
	protected final String success = "\"Success\"";
	
	@Override
	public void performOperation() throws OperationFailedException {
		if(variants == null)
			throw new OperationFailedException("No variant pool specified", this);
		
		List<VariantRec> vars = variants.toList();
		JSONObject json = new JSONObject();
		try {
			String sampleId = properties.get("sampleID");
			Logger.getLogger(Pipeline.primaryLoggerName).info("Uploading variants for sample " + sampleId);
			json.put("sample.id", Integer.parseInt(sampleId));
		
			JSONArray list = new JSONArray();
			for(VariantRec r: vars){
				JSONObject row = new JSONObject();
				row.put("chr", r.getContig());
				row.put("pos", r.getStart());
				row.put("ref", r.getRef());
				row.put("alt", r.getAlt());
				int count = 0;
				if (r.isHetero()) {
					count = 1;
				}
				else {
					count = 2;		
				}
				row.put("AlleleCount", count);
				list.put(row);
			}
				
			json.put("variant.list", list);
			String result = HttpUtils.HttpPostJSON(uploadURL, json);
			Logger.getLogger(Pipeline.primaryLoggerName).info("Uploading " + vars.size() + " variants to " + uploadURL);
			if(!result.equals(success)){
				//Not clear if we should fail here or what.. should we continue with future operations even if we 
				//can't communicate with .NET?
				//	throw new OperationFailedException("Failed to post variant list to .NET service: " + result, this);
				Logger.getLogger(Pipeline.primaryLoggerName).warn("Error uploading variants : " + result);
			}
		} catch (NumberFormatException e){
			throw new OperationFailedException("Failed to upload a JSON list of variants: " + e.getMessage(), this);
		} catch (JSONException e) {
			throw new OperationFailedException("Failed to upload a JSON list of variants: " + e.getMessage(), this);
		} catch (IOException e) {
			throw new OperationFailedException("Failed to upload a JSON list of variants: " + e.getMessage(), this);
		}
		
		
	}

	@Override
	public void initialize(NodeList children) {
		
		uploadURL = this.getPipelineProperty(VARIANT_UPLOAD_URL);
		if (uploadURL == null) {
			uploadURL = this.getAttribute(VARIANT_UPLOAD_URL);
		}
		if (uploadURL == null) {
			throw new IllegalArgumentException("VariantUploader required variant.upload.url to be specified");
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
	}

}
