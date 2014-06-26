/**
 * 
 */
package operator.variant;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import operator.OperationFailedException;
import operator.Operator;

import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;

import util.HttpUtils;

/**
 * @author markebbert
 *
 */
public class TorrentVariantUploader extends Operator {

	public static final String VARIANT_UPLOAD_URL = "variant.upload.url";
	public static final String ORDERABLE = "orderable";
	public static final String ACCESSION = "accession";

    /* A tab-delimited file with called alleles. See
     * /results/analysis/output/Home/
     * Auto_user_SN2-96-Clinical_Solid_Tumor_Panel-13Nov2013-316v2_chip_150_155/
     * plugin_out/ARUPAnnotate_out/IonXpress_004_alleles.calls_only.txt
     * for an example. 
     */
	public static final String VARIANTS_FILE = "variants.file"; 

	protected final String success = "\"Success\"";
	protected String uploadURL = "http://ngs-webapp-dev/Variant/UploadVariants";
	protected String orderable, varsFile, accession;

	/**
	 * 
	 */
	public TorrentVariantUploader() {
		return;
	}

	/* (non-Javadoc)
	 * @see operator.Operator#performOperation()
	 */
	@Override
	public void performOperation() throws OperationFailedException {
		
		String success = "\"Success\"";
		JSONObject sample = readVarsFile(varsFile);
		String result;
		try {
            result = HttpUtils.HttpPostJSON(uploadURL, sample);
            Logger.getLogger(Pipeline.primaryLoggerName).info("Uploading sample '" + accession + "' variants to " + uploadURL);
            if(!success.equals(result)){
                Logger.getLogger(Pipeline.primaryLoggerName).warn("Error uploading variants: " + result);
            }
		} catch (IOException e) {
			throw new OperationFailedException("Failed to upload a JSON list of variants: " + e.getMessage(), this);
		}
	}

	/* (non-Javadoc)
	 * @see pipeline.PipelineObject#initialize(org.w3c.dom.NodeList)
	 */
	@Override
	public void initialize(NodeList children) {
		
		uploadURL = this.getPipelineProperty(VARIANT_UPLOAD_URL);
		if (uploadURL == null) {
			uploadURL = this.getAttribute(VARIANT_UPLOAD_URL);
		}
		if (uploadURL == null) {
			throw new IllegalArgumentException("TorrentVariantUploader requires 'variant.upload.url' to be specified");
		}
		
		orderable = this.getAttribute(ORDERABLE);
		if(orderable == null){
			throw new IllegalArgumentException("TorrentVariantUploader requires 'orderable' to be specified");
		}
		
		accession = this.getAttribute(ACCESSION);
		if(accession == null){
			throw new IllegalArgumentException("TorrentVariantUploader requires 'accession' to be specified");
		}
		
		varsFile = this.getAttribute(VARIANTS_FILE);
		if(varsFile == null){
			throw new IllegalArgumentException("TorrentVariantUploader requires 'varsFile' to be specified");
		}
		
	}
	
	private JSONObject readVarsFile(String varsFile) throws OperationFailedException{
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(varsFile));
			String line;
			int count = 0;
            JSONObject var, sample = new JSONObject();
            JSONArray vars = new JSONArray();
            
            sample.put("accession", accession);
            sample.put("result", orderable);

			while((line = reader.readLine()) != null){
				count++;
				if(count == 1){
					continue; // ignore header line
				}
				String[] toks = line.split("\t");
				var = new JSONObject();
				var.put("chr", toks[0].replaceAll("chr", ""));
				var.put("pos", toks[1]);
				var.put("ref", toks[2]);
				var.put("alt", toks[3]);
				var.put("AlleleFrequency", toks[6]);
				vars.put(var);
			}
			
			sample.put("variant.list", vars);
			reader.close();
			return sample;

		} catch (FileNotFoundException e) {
			throw new OperationFailedException("Could not read variant file: " + e.getMessage(), this);
		} catch (IOException e) {
			throw new OperationFailedException("Could not read variant file: " + e.getMessage(), this);
		} catch (JSONException e) {
			throw new OperationFailedException("Invalid JSON: " + e.getMessage(), this);
		}
	}
	
	public static void main(String[] args){
		TorrentVariantUploader tvu = new TorrentVariantUploader();
		tvu.uploadURL = "http://10.211.55.6:9172/Variant/UploadVariants";
		tvu.orderable = "2006338";
		tvu.accession = "12282100007";
		tvu.varsFile = "input_files/IonXpress_004_alleles.calls_only.txt";
		try {
			tvu.performOperation();
		} catch (OperationFailedException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

}
