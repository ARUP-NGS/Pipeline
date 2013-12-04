package operator.variant;

import java.io.IOException;
import java.util.List;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import operator.OperationFailedException;
import operator.Operator;
import pipeline.PipelineObject;
import util.HttpUtils;

public class VariantUploader extends Operator {

	protected VariantPool variants = null;
//	protected final String uploadURL = "http://ngs-webapp-dev/Variant/UploadVariants";
	protected final String uploadURL = "http://localhost:9172/Variant/UploadVariants";
	protected final String success = "\"Success\"";
	
	@Override
	public void performOperation() throws OperationFailedException {
		if(variants == null)
			throw new OperationFailedException("No variant pool specified", this);
		
		List<VariantRec> vars = variants.toList();
		JSONObject json = new JSONObject();
		try {
			json.put("sample.id", Integer.parseInt(properties.get("sampleID")));
		
			JSONArray list = new JSONArray();
			for(VariantRec r: vars){
				JSONObject row = new JSONObject();
				row.put("chr", r.getContig());
				row.put("pos", r.getStart());
				row.put("ref", r.getRef());
				row.put("alt", r.getAlt());
				list.put(row);
			}
	
			json.put("variant.list", list);
			String result = HttpUtils.HttpPostJSON(uploadURL, json); 
			if(!result.equals(success)){
				throw new OperationFailedException("Failed to post variant list to .NET service: " + result, this);
				
			}
		} catch (JSONException | IOException e) {
			throw new OperationFailedException("Failed to upload a JSON list of variants: " + e.getMessage(), this);
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
