package util.varFreqDB;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import buffer.BEDFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import buffer.variant.VariantStore;
import util.reviewDir.ManifestParseException;
import util.reviewDir.SampleManifest;
import util.vcfParser.VCFParser;

public class ComputeVarFreqs {

	//Map from analysis type to collection of variants
	public static final String SAMPLES = "samples";
	public static final String HETS = "hets";
	public static final String HOMS = "homs";
	
	public static final String[] CONTIGS = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "M", "MT"};
	//public static final String[] CONTIGS = new String[]{"10", "12"};
	
	Map<String, SampleInfo> allSamples = new HashMap<String, SampleInfo>();
	
	
	public boolean addSample(SampleManifest info) throws IOException {
		System.err.println("Adding sample : " + info.getSampleName() + " :" + info.getAnalysisType());
		File vcf = info.getVCF();
		File bed = info.getBED();
		boolean include = true;
		if (info.hasProperty("include.in.freq.calc")) {
			include =   Boolean.parseBoolean(info.getProperty("include.in.freq.calc"));	
		}
		
		if (! include) {
			System.err.println("Sample " + info.getSampleName() + " " + info.getAnalysisType() + " flagged for non-inclusion, skipping.");
			return false;
		}
		String analysis = info.getAnalysisType();
		if (analysis.contains("(")) {
			analysis = analysis.substring(0, analysis.indexOf("(")).trim();
		}
		
		if (bed == null) {
			System.err.println("BED file is null for sample: " + info.getSampleName() + ", " + info.getAnalysisType());
			return false;
		}
		
		SampleInfo sampInfo = new SampleInfo(vcf, info.getSampleName(), info.getCompletionDate());
		sampInfo.analysisType = info.getAnalysisType();
		sampInfo.bed = new BEDFile(bed);
		
		//Only add if there's nothing else with this key OR if there is a sample with this key but it
		//has an older date than the current sample
		
		if (! allSamples.containsKey(sampInfo.key())) {
			allSamples.put(sampInfo.key(), sampInfo);
		} else {
			
			Date existing = allSamples.get(sampInfo.key()).completionDate;
			if (sampInfo.completionDate.after(existing)) {
				System.err.println("Found conflicting samples with accession " + sampInfo.accession + " replacing older version with this one.");
				allSamples.put(sampInfo.key(), sampInfo);	
			} else {
				System.err.println("Found conflicting samples with accession " + sampInfo.accession + " but preserving existing one since it's newer.");
			}
				
		}
		
		return true;
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
	
	
	public void emitTabulatedByContig(int threadCount, String contig) throws IOException {
		System.err.println("Tabulating variants, this may take a moment....");
		List<SampleInfo> errors = new ArrayList<SampleInfo>();
		List<String> analysisTypes = new ArrayList<String>();
		VariantStore everything = new VariantPool();
		
		Queue<SampleInfo> finishedTasks = new LinkedBlockingQueue<SampleInfo>(20);
		
		final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( threadCount );
		for(SampleInfo sampInfo : allSamples.values()) {
			VariantStoreReader readerTask = new VariantStoreReader(sampInfo, finishedTasks);
			threadPool.submit(readerTask);
		}
		
		System.err.println("Before... completed tasks: " + threadPool.getCompletedTaskCount() + " Finished tasks size: " + finishedTasks.size());

		threadPool.shutdown();
		while(threadPool.getCompletedTaskCount() < allSamples.size() || (!finishedTasks.isEmpty())) {
			SampleInfo sampInfo = finishedTasks.poll();
			if (sampInfo == null) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}
			
			System.err.println("Completed tasks: " + threadPool.getCompletedTaskCount() + " Finished tasks size: " + finishedTasks.size());
			
			try {
				if (! analysisTypes.contains(sampInfo.analysisType)) {
					analysisTypes.add(sampInfo.analysisType);
				}
					
				System.err.println("Chr : " + contig + " adding variants for " + sampInfo.source.getName() + " pool size: " + everything.size());
				VariantPool vp = new VariantPool(sampInfo.getPool().getVariantsForContig(contig));
				everything.addAll(vp, false); //Do not allow duplicates
				sampInfo.disposePool();
			}
			catch (Exception ex) {
				errors.add(sampInfo);
				if (sampInfo.source != null) {
					System.err.println("Error reading variants in " + sampInfo.source.getAbsolutePath() + ": " + ex.getLocalizedMessage() + ", skipping it.");
				}
			}
			
			if (finishedTasks.isEmpty() && threadPool.getActiveCount()>0) {
				try {
					System.err.println("Queue is empty, but still some active tasks, sleeping for a bit...");
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		
		System.err.println("Completed tasks: " + threadPool.getCompletedTaskCount() + " queue size: " + finishedTasks.size());
		System.err.println("All done adding  variants...");
		
		try {
			threadPool.awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(SampleInfo err : errors) {
			allSamples.remove(err);
		}
		
		System.err.println("Found " + everything.size() + " variants in " + allSamples.size() + " samples.");
		System.err.println(errors.size() + " of which had errors and could not be read.");
		
		
		//Step 2: Iterate across all samples, then across all variants, see how many
		// samples targeted each variant 
		
		final ThreadPoolExecutor threadPool2 = (ThreadPoolExecutor) Executors.newFixedThreadPool( threadCount );

		
		for(SampleInfo info : allSamples.values()) {
			info.bed.buildIntervalsMap();
			threadPool2.submit(new VarCountCalculator(info, everything));							
		}
		
		threadPool2.shutdown();
		try {
			threadPool2.awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.err.println("Done adding all variants, now just emitting output...");


		for(VariantRec var : everything.getVariantsForContig(contig)) {
			Double totSamples = var.getProperty(SAMPLES);
			Double compTotSamples = 0.0; 
			if (totSamples != null && totSamples > 0) {
				Double hets = var.getProperty(HETS);
				Double homs = var.getProperty(HOMS);
				if (hets == null) {
					hets = 0.0;
				}
				if (homs == null) {
					homs = 0.0;
				}

				System.out.println(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt() + "\toverall\t" + (int)Math.round(totSamples) + "\t" + (int)Math.round(hets) + "\t" + (int)Math.round(homs));
				
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

					if (totSamplesType>0) {
						System.out.print(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt() + "\t" + type);
						System.out.println("\t" + (int)Math.round(totSamplesType) + "\t" + (int)Math.round(hets) + "\t" + (int)Math.round(homs));
					}
				}
			
			if (! compTotSamples.equals(totSamples)) {
				throw new IllegalStateException("Sanity check failed: sum of samples did not match total! tot=" + totSamples + " comp: " + compTotSamples);
			}
			}

		}	
	}
	
	
	
	public static String[] readSamplesFromFile(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		List<String> samples = new ArrayList<String>();
		while(line != null) {
			String dir = line.trim();
			if (dir.length()>0) {
				samples.add(dir);
			}
			line = reader.readLine();
		}
		
		reader.close();
		return samples.toArray(new String[]{});
	}
	
	public static void main(String[] args) {
		ComputeVarFreqs cFreqs = new ComputeVarFreqs();
		
		//By default, just use the argument list as the list of sample review dirs
		//But if first arg is -f, then assume second arg is the name of a file to read review dirs from
		String[] samples = args;
		if (args[0].equals("-f")) {
			try {
				samples = readSamplesFromFile(args[1]);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		
		int added = 0;
		for(int i=0; i<samples.length; i++) {
			
			try {
				boolean ok = cFreqs.addSample( SampleManifest.create(samples[i]));
				if (ok) {
					added++;
				}
			} catch (ManifestParseException e) {
				System.err.println("Warning: Skipping file : " + samples[i]  + " : " + e.getLocalizedMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.println("Error in file : " + samples[i]  + " : " + e.getLocalizedMessage() + " skipping it.");
				e.printStackTrace();
			}	
		}
		

		System.err.println("Found " + added + " valid samples");
		
		int threads = 8;
		
		
		try {
			for(String contig : CONTIGS) {
				System.err.println("Processing contig " + contig);
				cFreqs.emitTabulatedByContig(threads, contig);
				System.err.println("Done processing contig " + contig + "\n\n\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	class SampleInfo {
		File source = null;
		String analysisType = null;
		BEDFile bed = null;
		String sampleName;
		String accession;
		Date completionDate = null;
		private VariantStore pool = null;
		
		public SampleInfo(File source, String sampleName, Date completionDate) throws IOException {
			this.source = source;	
			this.sampleName = sampleName;
			this.accession = parseAccession(sampleName);
		}
		
		public VariantStore getPool() throws IOException {
			if (pool == null) {
				VCFParser vcfParser = new VCFParser(source);
				pool = new VariantPool(vcfParser);
			}
			return pool;
		}
		
		public String key() {
			return accession + analysisType;
		}
		
		public void disposePool() {
			pool = null;
		}
		
	}


	public static String parseAccession(String sampleName) {
		String[] toks = sampleName.split("_");
		for(String tok : toks) {
			if (tok.length()==11) {
				return tok;
			}
		}
		return null;
	}
	
}


	

