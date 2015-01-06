package util.varFreqDB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import util.reviewDir.ManifestParseException;
import util.reviewDir.SampleManifest;
import util.vcfParser.VCFParser;
import util.vcfParser.VCFParser.GTType;
import buffer.BEDFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class ComputeVarFreqs {

	//Map from analysis type to collection of variants
	public static final String SAMPLES = "samples";
	public static final String HETS = "hets";
	public static final String HOMS = "homs";
	
	List<SampleInfo> sampleList = new ArrayList<SampleInfo>();
	
	public boolean addSample(SampleManifest info) throws IOException {
		System.err.println("Adding sample : " + info.getSampleName() + " :" + info.getAnalysisType());
		File vcf = info.getVCF();
		File bed = info.getBED();
		boolean include = info.hasProperty("include.in.freq.calc") && Boolean.parseBoolean(info.getProperty("include.in.freq.calc"));
		if (! include) {
			System.err.println("Sample " + info.getSampleName() + " " + info.getAnalysisType() + " flagged for non-inclusion, skipping.");
		}
		String analysis = info.getAnalysisType();
		if (analysis.contains("(")) {
			analysis = analysis.substring(0, analysis.indexOf("(")).trim();
		}
		
		if (bed == null) {
			System.err.println("BED file is null for sample: " + info.getSampleName() + ", " + info.getAnalysisType());
			return false;
		}
		
		SampleInfo sampInfo = new SampleInfo(vcf);
		sampInfo.analysisType = info.getAnalysisType();
		sampInfo.bed = new BEDFile(bed);
		sampleList.add(sampInfo);
		return true;
	}
	
	
	
	private static String formatProperty(String prop) {
		if (prop.equals("-")) {
			return "0.0";
		}
		else 
			return prop;
	}
	
	public void readSamplesInDir(File dir) throws ManifestParseException, IOException {
		File[] files = dir.listFiles();
		for(int i=0; i<files.length; i++) {
			if (files[i].isDirectory()) {
				SampleManifest info = SampleManifest.create(files[i].getAbsolutePath());
				addSample(info);
			}
		}
		
	}
	
	
	private static void incrementProperty(VariantRec var, String key) {
		Double current = var.getProperty(key);
		if (current == null) {
			current = 0.0;
		}
		current++;
		var.addProperty(key, current);
	}
	
	public void emitTabulated() throws IOException {
		
		//Step 1 : Read all variants into gigantic variant pool (without duplicates)
		System.err.println("Tabulating variants, this may take a moment....");
		List<SampleInfo> errors = new ArrayList<SampleInfo>();
		List<String> analysisTypes = new ArrayList<String>();
		VariantPool everything = new VariantPool();
		
		
		for(SampleInfo sampInfo : sampleList) {
			if (! analysisTypes.contains(sampInfo.analysisType)) {
				analysisTypes.add(sampInfo.analysisType);
			}
			
			try {
				everything.addAll(sampInfo.getPool(), false); //Do not allow duplicates
			}
			catch (Exception ex) {
				errors.add(sampInfo);
				if (sampInfo.source != null) {
					System.err.println("Error reading variants in " + sampInfo.source.getAbsolutePath() + ": " + ex.getLocalizedMessage() + ", skipping it.");
				}
			}
			sampInfo.disposePool();
		}
		
		
		for(SampleInfo err : errors) {
			sampleList.remove(err);
		}
		
		System.err.println("Found " + everything.size() + " variants in " + sampleList.size() + " samples.");
		System.err.println(errors.size() + " of which had errors and could not be read.");
		
		
		//Step 2: Iterate across all samples, then across all variants, see how many
		// samples targeted each variant 
		
		
		for(SampleInfo info : sampleList) {
			info.bed.buildIntervalsMap();
			String typeKey = info.analysisType;
			
			for(String contig: everything.getContigs()) {
				for(VariantRec var : everything.getVariantsForContig(contig)) {
					
					//Is this variant targeted for this sample?
					boolean targeted = info.bed.contains(var.getContig(), var.getStart(), false);
					
					if (targeted) {
						incrementProperty(var, typeKey+SAMPLES);
						incrementProperty(var, SAMPLES);
						
						VariantRec queryVar = info.getPool().findRecordNoWarn(contig, var.getStart());

						if (queryVar != null) {
							if (queryVar.getGenotype() == GTType.HET) {
								incrementProperty(var, typeKey+HETS);
								incrementProperty(var, HETS);
							}
							else {
								incrementProperty(var, typeKey+HOMS);
								incrementProperty(var, HOMS);
							}
						}
					}
					
					
				}
			}
			
			info.disposePool();
		}
		
		//Now emit everything
		System.out.print("#chr\tpos\tref\talt\test.type\tsample.count\thets\thoms");
		System.out.println();
		
		for(String contig: everything.getContigs()) {
			for(VariantRec var : everything.getVariantsForContig(contig)) {
				
				//First do 'overall'
				Double totSamples = var.getProperty(SAMPLES);
				Double compTotSamples = 0.0; //Sanity check, make sure sum of samples by type matches overall total
				if (totSamples != null && totSamples > 0) {
					Double hets = var.getProperty(HETS);
					Double homs = var.getProperty(HOMS);
					if (hets == null) {
						hets = 0.0;
					}
					if (homs == null) {
						homs = 0.0;
					}
					

					System.out.print(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt() + "\toverall");
					System.out.println("\t" + ("" + (int)Math.round(totSamples)) + "\t" + ("" + (int)Math.round(hets)) + "\t" + ("" + (int)Math.round(homs)));
					
					for(String type : analysisTypes) {
						Double totSamplesType = var.getProperty(type+SAMPLES);
						if (totSamplesType == null) {
							totSamplesType = 0.0;
						}
						compTotSamples += totSamplesType;
						hets = var.getProperty(type+HETS);
						homs = var.getProperty(type+HOMS);
						if (hets == null) {
							hets = 0.0;
						}
						if (homs == null) {
							homs = 0.0;
						}
					
						System.out.print(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt() + "\t" + type);
						System.out.println("\t" + ("" + (int)Math.round(totSamplesType)) + "\t" + ("" + (int)Math.round(hets)) + "\t" + ("" + (int)Math.round(homs)));
					}
					
					if (! compTotSamples.equals(totSamples)) {
						throw new IllegalStateException("Sanity check failed: sum of samples did not match total! tot=" + totSamples + " comp: " + compTotSamples);
					}
				}
				
			}
		}
		
			
		
	}
	
	public static void main(String[] args) {
		ComputeVarFreqs cFreqs = new ComputeVarFreqs();
		
		int added = 0;
		for(int i=0; i<args.length; i++) {
			
			try {
				boolean ok = cFreqs.addSample( SampleManifest.create(args[i]));
				if (ok) {
					added++;
				}
			} catch (ManifestParseException e) {
				System.err.println("Warning: Skipping file : " + args[i]  + " : " + e.getLocalizedMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.println("Error in file : " + args[i]  + " : " + e.getLocalizedMessage() + " skipping it.");
				e.printStackTrace();
			}	
		}
		

		System.err.println("Found " + added + " valid samples");
		try {
			cFreqs.emitTabulated();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	class SampleInfo {
		File source = null;
		String analysisType = null;
		BEDFile bed = null; 
		private VariantPool pool = null;
		
		public SampleInfo(File source) throws IOException {
			this.source = source;	
		}
		
		public VariantPool getPool() throws IOException {
			if (pool == null) {
				pool = new VariantPool(new VCFParser(source));
			}
			return pool;
		}
		
		public void disposePool() {
			pool = null;
		}
		
	}
	
	}
	

