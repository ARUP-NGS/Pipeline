package operator.qc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import json.JSONException;
import json.JSONObject;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.BAMFile;
import buffer.BAMMetrics;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.DOCMetrics;
import buffer.TextBuffer;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Writes various QC info bits to a JSON formatted output file
 * @author brendan
 *
 */
public class QCtoJSON extends Operator {

	DOCMetrics rawCoverageMetrics = null;
	DOCMetrics finalCoverageMetrics = null;
	BAMMetrics rawBAMMetrics = null;
	BAMMetrics finalBAMMetrics = null;
	VariantPool variantPool = null;
	CSVFile noCallCSV = null;
	BEDFile captureBed = null;
	TextBuffer jsonFile = null;
	
	/**
	 * Get the file to which the JSON output is written
	 * @return
	 */
	public TextBuffer getOutputFile() {
		return jsonFile;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (jsonFile == null) {
			throw new OperationFailedException("Output file is null", this);
		}
		
		JSONObject qcObj = new JSONObject();
		if (rawCoverageMetrics != null) {
			try {
				qcObj.put("raw.coverage.metrics", new JSONObject(rawCoverageMetrics.toJSONString()));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (finalCoverageMetrics != null) {
			try {
				qcObj.put("final.coverage.metrics", new JSONObject(finalCoverageMetrics.toJSONString()));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		if (rawBAMMetrics != null) {
			try {
				qcObj.put("raw.bam.metrics", new JSONObject(rawBAMMetrics.toJSONString()));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (finalBAMMetrics != null) {
			try {
				qcObj.put("final.bam.metrics", new JSONObject(finalBAMMetrics.toJSONString()));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			qcObj.put("variant.metrics", new JSONObject(variantPoolToJSON(variantPool)));
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			qcObj.put("nocalls", new JSONObject(noCallsToJSON(noCallCSV)));
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			qcObj.put("capture.bed", captureBed.getFilename());
			qcObj.put("capture.extent", captureBed.getExtent());
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile.getFile()));
			writer.write( qcObj.toString() );
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OperationFailedException("Could not write to output file " + jsonFile.getAbsolutePath(), this);
		}
		
	}

	private String noCallsToJSON(CSVFile noCallCSV) throws JSONException {
		JSONObject obj = new JSONObject();
		if (noCallCSV == null) {
			obj.put("error", "no no-call file specified");
			return obj.toString();
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(noCallCSV.getAbsolutePath()));
			String line = reader.readLine();
			while(line != null) {
				String[] toks = line.split(" ");
				if (toks.length == 4) {
					if (! toks[3].equals("CALLABLE")) {
						obj.put(toks[0] + ":" + toks[1] + "-" + toks[2], toks[3]);
					}
				}
				line = reader.readLine();
			}
			
			reader.close();
		}
		catch(Exception ex) {
			
		}
		return obj.toString();
	}
	
	private String variantPoolToJSON(VariantPool vp) throws JSONException {
		JSONObject obj = new JSONObject();
		if (vp == null) {
			obj.put("error", "no variant pool specified");
			return obj.toString();
		}

		safePutJSON(obj, "total.vars", vp.size());
		safePutJSON(obj, "total.tt.ratio", vp.computeTTRatio());
		safePutJSON(obj, "total.snps", vp.countSNPs());
		safePutJSON(obj, "total.insertions", vp.countInsertions());
		safePutJSON(obj,"total.deletions", vp.countDeletions());
		int knowns = countKnownVars(vp);
		safePutJSON(obj,"total.known", knowns);
		double[] ttRatios = computeTTForKnownsNovels(vp);
		safePutJSON(obj,"known.tt", ttRatios[0]);
		safePutJSON(obj,"novel.tt", ttRatios[1]);
		safePutJSON(obj,"total.het.vars", vp.countHeteros());

		return obj.toString();
	}

	/**
	 * Adds the given value to the JSON object, catching all JSON exceptions that are thrown.
	 * If the value is added, return true, otherwise false. 
	 * @param obj
	 * @param key
	 * @param value
	 * @return
	 */
	private boolean safePutJSON(JSONObject obj, String key, double value) {
		try {
			obj.put(key, value);
			return true;
		}
		catch (JSONException ex) {
			return false;
		}
	}
	
	private boolean safePutJSON(JSONObject obj, String key, int value) {
		try {
			obj.put(key, value);
			return true;
		}
		catch (JSONException ex) {
			return false;
		}
	}

	
	/**
	 * Compute TT ratio in known and novel snps, 
	 * @param vp
	 * @return
	 */
	private double[] computeTTForKnownsNovels(VariantPool vp) {
		VariantPool knowns = new VariantPool();
		VariantPool novels= new VariantPool();
		for(String contig : vp.getContigs()) {
			for(VariantRec var : vp.getVariantsForContig(contig)) {
				Double tgpFreq = var.getProperty(VariantRec.POP_FREQUENCY);
				Double espFreq = var.getProperty(VariantRec.EXOMES_FREQ);
				if ( (tgpFreq != null && tgpFreq > 0) || (espFreq != null && espFreq > 0)) {
					knowns.addRecordNoSort(var);
				}
				else {
					novels.addRecordNoSort(var);
				}
			}
		}
		
		knowns.sortAllContigs();
		novels.sortAllContigs();
		double[] ttRatios = new double[2];
		if (knowns.countSNPs()>0) {
			ttRatios[0] = knowns.computeTTRatio();
		}
		if (novels.countSNPs()>0) {
			ttRatios[1] = novels.computeTTRatio();
		}
		return ttRatios;
	}


	/**
	 * Compute number of variants previously seen in 1000 Genomes
	 * @param vp
	 * @return
	 */
	private int countKnownVars(VariantPool vp) {
		int knowns = 0;
		for(String contig : vp.getContigs()) {
			for(VariantRec var : vp.getVariantsForContig(contig)) {
				Double tgpFreq = var.getProperty(VariantRec.POP_FREQUENCY);
				Double espFreq = var.getProperty(VariantRec.EXOMES_FREQ);
				if ( (tgpFreq != null && tgpFreq > 0) || (espFreq != null && espFreq > 0)) {
					knowns++;
				}
			}
		}
		return knowns;
	}


	@Override
	public void initialize(NodeList children) {
		
		
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof BAMFile) {
					throw new IllegalArgumentException("Please supply a BamMetrics object, not a BAMFile object to the qc report (offending object:" + obj.getObjectLabel() +")");
				}
				
				if (obj instanceof BAMMetrics ) {
					if (rawBAMMetrics == null) {
						rawBAMMetrics = (BAMMetrics) obj;
					}
					else {
						if (finalBAMMetrics == null)
							finalBAMMetrics = (BAMMetrics) obj;
						else
							throw new IllegalArgumentException("Too many BAM metrics objects specified, must be exactly 2");
					}
					
				}
				
				if (obj instanceof DOCMetrics) {
					if (rawCoverageMetrics == null)
						rawCoverageMetrics = (DOCMetrics) obj;
					else {
						finalCoverageMetrics = (DOCMetrics) obj;
					}
				}
				if (obj instanceof VCFFile) {
					throw new IllegalArgumentException("Got a straight-up VCF file as input to QC metrics, this now needs to be a variant pool.");
				}
				if (obj instanceof VariantPool) {
					variantPool = (VariantPool)obj;
				}
				if (obj instanceof TextBuffer) {
					jsonFile = (TextBuffer) obj;
				}
				if (obj instanceof BEDFile) {
					captureBed = (BEDFile) obj;
				}
				if (obj instanceof CSVFile) {
					noCallCSV = (CSVFile)obj;
				}
				
				// ?
			}
		}
		
		if (rawBAMMetrics == null) {
			throw new IllegalArgumentException("No raw BAM metrics objects specified");
		}
		
		if (finalBAMMetrics == null) {
			throw new IllegalArgumentException("No final BAM metrics objects specified");
		}
		
	}

}
