package util.varFreqDB;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
	
	
	 
	
	private static synchronized void incrementProperty(VariantRec var, String key) {
		Double current = var.getProperty(key);
		if (current == null) {
			current = 0.0;
		}
		current++;
		var.addProperty(key, current);
	}
	
	public void emitTabulated(int threadCount) throws IOException {
		
		//Step 1 : Read all variants into gigantic variant pool (without duplicates)
		System.err.println("Tabulating variants, this may take a moment....");
		List<SampleInfo> errors = new ArrayList<SampleInfo>();
		List<String> analysisTypes = new ArrayList<String>();
		VariantPool everything = new VariantPool();
		
		Queue<SampleInfo> finishedTasks = new ConcurrentLinkedQueue<SampleInfo>();
		final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( threadCount );
		for(SampleInfo sampInfo : sampleList) {
			PoolReader readerTask = new PoolReader(sampInfo, finishedTasks);
			threadPool.submit(readerTask);
		}
		
		System.err.println("Before... completed tasks: " + threadPool.getCompletedTaskCount() + " Finished tasks size: " + finishedTasks.size());

	
		
		threadPool.shutdown();
		while(threadPool.getCompletedTaskCount() < sampleList.size() || (!finishedTasks.isEmpty())) {
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
				
				System.err.println("Adding variants for " + sampInfo.source.getName());
				everything.addAll(sampInfo.getPool(), false); //Do not allow duplicates
			}
			catch (Exception ex) {
				errors.add(sampInfo);
				if (sampInfo.source != null) {
					System.err.println("Error reading variants in " + sampInfo.source.getAbsolutePath() + ": " + ex.getLocalizedMessage() + ", skipping it.");
				}
			}
			sampInfo.disposePool();
			
			if (finishedTasks.isEmpty() && threadPool.getActiveCount()>0) {
				try {
					System.err.println("Queue is empty, but still some active tasks, sleeping for a bit...");
					Thread.currentThread().sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		System.err.println("Completed tasks: " + threadPool.getCompletedTaskCount() + " Finished tasks size: " + finishedTasks.size());
		System.err.println("All done adding  variants...");
		
		try {
			threadPool.awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(SampleInfo err : errors) {
			sampleList.remove(err);
		}
		
		System.err.println("Found " + everything.size() + " variants in " + sampleList.size() + " samples.");
		System.err.println(errors.size() + " of which had errors and could not be read.");
		
		
		//Step 2: Iterate across all samples, then across all variants, see how many
		// samples targeted each variant 
		
		final ThreadPoolExecutor threadPool2 = (ThreadPoolExecutor) Executors.newFixedThreadPool( threadCount );

		
		for(SampleInfo info : sampleList) {
			info.bed.buildIntervalsMap();
			
			threadPool2.submit(new VariantAdder(info, everything));
			
							
		}
		
		threadPool2.shutdown();
		try {
			threadPool2.awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.err.println("Done adding all variants, now just emitting output...");
		
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
					
						if (totSamplesType>0) {
							System.out.print(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt() + "\t" + type);
							System.out.println("\t" + ("" + (int)Math.round(totSamplesType)) + "\t" + ("" + (int)Math.round(hets)) + "\t" + ("" + (int)Math.round(homs)));
						}
					}
					
					if (! compTotSamples.equals(totSamples)) {
						throw new IllegalStateException("Sanity check failed: sum of samples did not match total! tot=" + totSamples + " comp: " + compTotSamples);
					}
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
			cFreqs.emitTabulated(threads);
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
				VCFParser vcfParser = new VCFParser(source);
				pool = new VariantPool(vcfParser);
			}
			return pool;
		}
		
		public void disposePool() {
			pool = null;
		}
		
	}
	
	class VariantAdder implements Runnable {
		final SampleInfo info;
		final VariantPool everything;
		
		public VariantAdder(SampleInfo info, VariantPool everything) {
			this.info = info;
			this.everything = everything;
		}
		
		@Override
		public void run() {
			String typeKey = info.analysisType;
			VariantPool pool;
			System.err.println("Running " + info.source.getName());
			try {
				pool = info.getPool();
			} catch (IOException e) {
				System.err.println("Could not load pool");
				return;
				
			}
			for(String contig: everything.getContigs()) {
				for(VariantRec var : everything.getVariantsForContig(contig)) {

					//Is this variant targeted for this sample?
					boolean targeted = info.bed.contains(var.getContig(), var.getStart(), false);

					if (targeted) {
						incrementProperty(var, typeKey+SAMPLES);
						incrementProperty(var, SAMPLES);

						VariantRec queryVar = pool.findRecord(contig, var.getStart(), var.getRef(), var.getAlt());

						if (queryVar != null) {
							if (queryVar.getZygosity() == GTType.HET) {
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
			System.err.println(info.source.getName() + " is done");
		}
	
	}
	
	class PoolReader implements Runnable {

		final SampleInfo info;
		final Queue<SampleInfo> finishedQueue;
		Exception ex = null;
		
		public PoolReader(SampleInfo info, Queue<SampleInfo> queue) {
			this.info = info;
			this.finishedQueue = queue;
		}
		
		@Override
		public void run() {
			System.err.println("Reading pool for " + info.source.getName());
			try {
				info.getPool();
				finishedQueue.add(info);		
				System.err.println("Done reading pool for " + info.source.getName());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.println("Yikes, errored out reading " + info.source.getName() + " Error: " + e.getLocalizedMessage());
				e.printStackTrace();
				this.ex = e;
			} //Spawns a potentially pretty long job
				
		}
		
		public Exception getException() {
			return ex;
		}
		
	}
	
	}


	

