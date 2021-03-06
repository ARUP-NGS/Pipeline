package operator.qc;

import gene.ExonLookupService;
import gene.ExonLookupService.FeatureDescriptor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
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

	public static final String NM_DEFS = "nm.Definitions";
	public static final String STRICT_NMS = "strict.nm";
	public static final String MIN_LENGTH = "min.length";
	public static final int DEFAULT_MIN_LENGTH = 2;
	private Map<String, String> nms = null;
	
	private int minNoCallLength = DEFAULT_MIN_LENGTH;
	
	DOCMetrics rawCoverageMetrics = null;
	DOCMetrics finalCoverageMetrics = null;
	BAMMetrics rawBAMMetrics = null;
	BAMMetrics finalBAMMetrics = null;
	VariantPool variantPool = null;
	CSVFile noCallCSV = null;
	BEDFile captureBed = null;
	TextBuffer jsonFile = null;
	
	
	//If true, do not report regions that do not have a preferred NM specified.
	private boolean strictNM = true;
	
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
				JSONObject finalBAMJson = new JSONObject(finalBAMMetrics.toJSONString());
				qcObj.put("final.bam.metrics", finalBAMJson);
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
			//qcObj.put("nocalls", new JSONObject(noCallsToJSON(noCallCSV)));
			qcObj.put("nocalls", new JSONObject(noCallsToJSONWithAnnotations(noCallCSV)));
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	
	
	private String noCallsToJSONWithAnnotations(CSVFile noCallCSV) throws JSONException, IOException {	
		JSONObject obj = new JSONObject();
		JSONArray allRegions = new JSONArray();
		obj.put("regions", allRegions);
		
		if (noCallCSV == null) {
			obj.put("error", "no no-call file specified");
			return obj.toString();
		}
		
		//Use the feature lookup service to find which features correspond to particular low coverage intervals
		ExonLookupService featureLookup = null;
			try {
				featureLookup = new ExonLookupService();
				String featureFile = getPipelineProperty("feature.file");
				if(featureFile == null){
					throw new IOException("PipelineProperty 'feature.file' not defined.");
				}
				File features = new File(featureFile);
				if (!features.exists()) {
					throw new IOException("Feature file " + features.getAbsolutePath() + " does not exist!");
				}
				featureLookup.setPreferredNMs(nms);
				featureLookup.buildExonMapWithCDSInfo(features, strictNM);
			}
			catch (IOException ex) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Error opening feature file, can't compute features for low coverage regions. " + ex.getLocalizedMessage());
				obj.put("error", "Error reading exon features file");
				return obj.toString();
			}
			
			BufferedReader reader = new BufferedReader(new FileReader(noCallCSV.getAbsolutePath()));
			String line = reader.readLine();
			int noCallIntervals = 0;
			int noCallPositions = 0;
			
			List<List<String>> regions = new ArrayList<List<String>>();
			
			while(line != null) {
				line = line.replace(" ", "\t");
					String[] toks = line.split("\t");
					
					if (toks.length > 3 && (! toks[3].equals("CALLABLE"))) {
						JSONObject region = new JSONObject();
						
						try {
							String contig = toks[0];
							long startPos = Long.parseLong(toks[1]);
							long endPos = Long.parseLong(toks[2]);
							long length = endPos - startPos;
							double cov = -1;
							
							if (length < minNoCallLength) {
								line = reader.readLine();
								continue;
							}
						
							
							String cause = toks[3];
							cause = cause.toLowerCase();
							cause  = ("" + cause.charAt(0)).toUpperCase() + cause.substring(1);
							if (toks.length > 4) {
								cov = Double.parseDouble(toks[4]);
							}
							
							Object[] features = new String[]{};
							if (featureLookup != null) {
								features = featureLookup.getIntervalObjectsForRange(contig, (int)startPos, (int)endPos);							
							}
							List<FeatureDescriptor> fds = new ArrayList<FeatureDescriptor>();
							for(Object o : features) {
								fds.add((FeatureDescriptor)o);
							}
							String featureStr = ExonLookupService.mergeFeatures(fds);
							if (length > 1 && (featureStr.contains("exon"))) {
								regions.add(Arrays.asList(new String[]{"chr" + toks[0] + ":" + toks[1] + " - " + toks[2], "" + length, cause, featureStr}) );
							}
							
							
							noCallPositions += length;
							noCallIntervals++;
							
							region.put("gene", featureStr);
							region.put("size", length);
							region.put("reason", cause);
							region.put("chr", toks[0]);
							region.put("start", Integer.parseInt(toks[1]));
							region.put("end", Integer.parseInt(toks[2]));
							if (cov > -1) {
								region.put("mean.coverage", cov);
							}
							allRegions.put(region);
						} catch (NumberFormatException nfe) {
							//dont stress it
						}
				}
				line = reader.readLine();
			}
			
			reader.close();
			
			obj.put("interval.count", noCallIntervals);
			obj.put("no.call.extent", noCallPositions);
		
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
		int novel = vp.size()-knowns;
		
		//chrisk
		safePutJSON(obj,"novel.vars", novel);

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
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing to JSON for key '" + key + "', given value '" + value + "' (expected double).");
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
				//System.out.println("var: "+var.getContig()+" "+var.getStart()+" "+var.getRef()+" "+var.getAlt());
				Double tgpFreq = var.getProperty(VariantRec.POP_FREQUENCY);
				Double espFreq = var.getProperty(VariantRec.EXOMES_FREQ);
				Double mmFreq = var.getProperty(VariantRec.MITOMAP_FREQ);//for MDNA tests
				
				if ( (tgpFreq != null && tgpFreq > 0) || (espFreq != null && espFreq > 0) || (mmFreq != null && mmFreq > 0)) {
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
		} else if (knowns.countSNPs()==0) {
			ttRatios[0] = Double.NaN; //set to NaN so that known.tt won't be written to the qc.json when there are no known variants
		}
		if (novels.countSNPs()>0) {
			ttRatios[1] = novels.computeTTRatio();
		} else if (novels.countSNPs()==0) {
			ttRatios[1] = Double.NaN; //set to NaN so that novel.tt won't be written to the qc.json when there are no known variants
		}
		return ttRatios;
	}


	/**
	 * Compute number of variants previously seen in 1000 Genomes or MitoMap if mitochondrial genome
	 * @param vp
	 * @return
	 */
	private int countKnownVars(VariantPool vp) {
		int knowns = 0;
		for(String contig : vp.getContigs()) {
			for(VariantRec var : vp.getVariantsForContig(contig)) {
				Double tgpFreq = var.getProperty(VariantRec.POP_FREQUENCY);
				Double espFreq = var.getProperty(VariantRec.EXOMES_FREQ);
				Double mmFreq = var.getProperty(VariantRec.MITOMAP_FREQ);//for MDNA
				if ( (tgpFreq != null && tgpFreq > 0) || (espFreq != null && espFreq > 0) || (mmFreq != null && mmFreq > 0)) {
					knowns++;
				}
			}
		}
		return knowns;
	}


	@Override
	public void initialize(NodeList children) {
		
		String nmDefs = this.getAttribute(NM_DEFS);
		try {
			nms = loadPreferredNMs(nmDefs);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Preferred NM files are missing:  "+ nmDefs);
		}
		
		String minLength = this.getAttribute(MIN_LENGTH);
		if (minLength != null) {
			this.minNoCallLength = Integer.parseInt(minLength);
		}
		
		
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
	
		
		String strictNMStr = this.getAttribute(STRICT_NMS);
		if (strictNMStr != null) {
			strictNM = Boolean.parseBoolean(strictNMStr);
		}
	}

	
}
