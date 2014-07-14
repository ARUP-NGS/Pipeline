package operator.oncology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.lang.IllegalArgumentException;

import json.JSONException;
import json.JSONObject;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import util.FastaReader;
import util.bamUtil.ReadCounter;
import util.CompressGZIP;
import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.JSONBuffer;
import buffer.ReferenceFile;
import operator.bamutils.CountBAMRecords;

/*
 * @author daniel
 * Calculates ratios and counts for alignments to multiple custom reference files. 
 * 
 * 
*/
public class OncologyUtils extends IOOperator {
		
	public static final String SAMTOOLS_PATH = "samtools.path";
	public static String defaultSamPath = "samtools";

	@Override
	public void performOperation() throws OperationFailedException, JSONException, IOException {
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning utilities: Checking Arguments");
		System.out.println("Beginning utilities: Checking Arguments.");
		
		List<FileBuffer> FastqBuffers = this.getAllInputBuffersForClass(FastQFile.class); // Should contain 4 files
		List<FileBuffer> BamBuffers = this.getAllInputBuffersForClass(BAMFile.class);
		List<FileBuffer> CustomRefBuffers = this.getAllInputBuffersForClass(ReferenceFile.class);
		
		if(FastqBuffers.size() != 4) {
			System.out.println(FastqBuffers.size() + " fastq files provided.");
			throw new IllegalArgumentException("4 Fastq files required as input.");
		}
	
		
		if(BamBuffers.size() != 9) {
			System.out.println(BamBuffers.size() + " bam files provided.");
			throw new IllegalArgumentException("9 BAM files required as input.");
		}
		
		
		if(CustomRefBuffers.size() != 2) {
			System.out.println(CustomRefBuffers.size() + " fasta reference files provided.");
			throw new IllegalArgumentException("2 fasta reference files required as input.");
		}
		
		logger.info("Counting reads in Fastq Files");
		System.out.println("Counting reads in Fastq Files");
		/*long InFq = Integer.parseInt(executeCommandOutputToString("wc -l " + FastqBuffers.get(0).getAbsolutePath()).split(" ")[0])/4;
		long Trim40Fq = Integer.parseInt(executeCommandOutputToString("wc -l " + FastqBuffers.get(1).getAbsolutePath()).split(" ")[0])/4;
		long UnmappedFq = Integer.parseInt(executeCommandOutputToString("wc -l " + FastqBuffers.get(2).getAbsolutePath()).split(" ")[0])/4;
		long Trim90Fq = Integer.parseInt(executeCommandOutputToString("wc -l " + FastqBuffers.get(3).getAbsolutePath()).split(" ")[0])/4;
		System.out.println("InFq string is ... " + executeCommandOutputToString("wc -l " + FastqBuffers.get(0).getAbsolutePath()));
		System.out.println("Trim40Fq string is ... " + executeCommandOutputToString("wc -l " + FastqBuffers.get(1).getAbsolutePath()));
		System.out.println("UnmappedFq string is ... " + executeCommandOutputToString("wc -l " + FastqBuffers.get(2).getAbsolutePath()));
		System.out.println("Trim90Fq string is ... " + executeCommandOutputToString("wc -l " + FastqBuffers.get(3).getAbsolutePath()));
		*/
		long Trim90Fq = countLines(FastqBuffers.get(3).getAbsolutePath())/4;
		long InFq = countLines(FastqBuffers.get(0).getAbsolutePath())/4;
		long Trim40Fq = countLines(FastqBuffers.get(1).getAbsolutePath())/4;
		long UnmappedFq = countLines(FastqBuffers.get(2).getAbsolutePath())/4;

		
		/*
		 * 2. Get list of "chromosomes"
		 */
		FastaReader FusionRef = null;
		FusionRef = new FastaReader(CustomRefBuffers.get(0).getFile());
		String[] FusionContigs = FusionRef.getContigs();
		int fusionLength = FusionContigs.length;
		
		FastaReader RatioRef = null;
		RatioRef = new FastaReader(CustomRefBuffers.get(1).getFile());
		String[] RatioContigs = RatioRef.getContigs();
		int ratioLength = RatioContigs.length;
		
		/*
		 *  3. Count records for all 4 bam files
		 */
		logger.info("Counting records in BAM Files");
		System.out.println("Counting records in BAM Files");
		//TODO: Create external operator to complete this task
		CountBAMRecords something = new CountBAMRecords();
		long ratioMapped = something.CountRecords(BamBuffers.get(0));
		logger.info("Finished counting reads in BAM #1: mapped to ratio reference.");
		long ratioUnmapped = something.CountRecords(BamBuffers.get(1));
		logger.info("Finished counting reads in BAM #2: unmapped to ratio reference.");
		long fusionMapped = something.CountRecords(BamBuffers.get(2));
		logger.info("Finished counting reads in BAM #3: mapped to fusion reference");
		long fusionUnmapped = something.CountRecords(BamBuffers.get(3));
		logger.info("Finished counting reads in BAM #4: unmapped to fusion reference.");
		long filterFusion = something.CountRecords(BamBuffers.get(4));
		logger.info("Finished counting reads in BAM #5");
		long passMappedRatio = something.CountRecords(BamBuffers.get(5));
		logger.info("Finished counting reads in BAM #6");
		long passMatchRatio = something.CountRecords(BamBuffers.get(6));
		logger.info("Finished counting reads in BAM #7");
		long passMappedFusion = something.CountRecords(BamBuffers.get(7));
		logger.info("Finished counting reads in BAM #8");
		long passMatchFusion = something.CountRecords(BamBuffers.get(8));
		logger.info("Finished counting reads in BAM #9. Last BAM!");
		
		long filteredFromFusion = fusionMapped - filterFusion;
		long short40 = InFq - Trim40Fq;
		long short90 = UnmappedFq - Trim90Fq; 
		long mapFilterRatioCount = ratioMapped - passMappedRatio;
		long mismatchFilterRatioCount = passMappedRatio - passMatchRatio;
		long mapFilterFusionCount = fusionMapped - passMappedFusion;
		long mismatchFilterFusionCount = passMappedFusion - passMatchFusion;
		
		//Get map containing # of reads per contig
		Map<String, Long> bamRatioMap = ReadCounter.countReadsByChromosome((BAMFile)BamBuffers.get(6),1);
		Set<String> keysRatio = bamRatioMap.keySet();
		Map<String, Long> ratioMap = new HashMap<String, Long>();
		for(String contig:RatioContigs) {
			ratioMap.put(contig,(long)0);
		}
		for(String key:keysRatio) {
			System.out.println(key + " is the key with value " + bamRatioMap.get(key).toString());
			ratioMap.put(key, bamRatioMap.get(key));
		}
		
		Map<String, Long> bamFusionMap = ReadCounter.countReadsByChromosome((BAMFile)BamBuffers.get(4),1);
		Set<String> keysFusion = bamFusionMap.keySet();
		Map<String, Long> fusionMap = new HashMap<String, Long>();
		for(String contig:FusionContigs) {
			fusionMap.put(contig,(long)0);
		}
		for(String key:keysFusion) {
			//System.out.println(key + " is the key with value " + bamFusionMap.get(key).toString());
			fusionMap.put(key, bamFusionMap.get(key));
		}
		
		/*
		 * 4. Calculate ratios as needed
		 */
		
		double fracRatioMapped = (double) ratioMapped/InFq;
		System.out.println("Printing fracRatioMapped " + fracRatioMapped + " ratioMapped " + ratioMapped + " InFq " + InFq);
		double fracFusionMapped = (double) fusionMapped/InFq;
		System.out.println("Printing fracFusionMapped " + fracFusionMapped + " fusionMapped " + fusionMapped + " InFq " + InFq);
		double fracFilterFusion = (double) filterFusion/InFq;
		double fracRemovedFilterFusion = (double) filteredFromFusion/InFq;
		double fracShort40Mapped = (double) short40/InFq;
		double fracShort90Mapped = (double) short90/InFq;
		double fracUnmapped = (double) fusionUnmapped/InFq;
		
		long[] fusionCounts = new long[fusionLength];
		long[] ratioCounts = new long[ratioLength];
		double[] fusionFrac = new double[fusionLength];
		double[] ratioFrac = new double[ratioLength];
		
		int houseKeepingReads = 0;
		
		for(int i=0;i<fusionLength;i++) {
			fusionCounts[i]=fusionMap.get(FusionContigs[i]);
			fusionFrac[i]=(double)fusionCounts[i]/fusionMapped;
			if(i>=fusionLength-5) {
				houseKeepingReads+=(int)fusionCounts[i];
			}
		}
		if(houseKeepingReads == 0) {
			throw new OperationFailedException("Experimental run failed - 0 reads for all control genes.", this);
		}
		for(int i=0;i<ratioLength;i++) {
			ratioCounts[i]=ratioMap.get(RatioContigs[i]);
			ratioFrac[i]=(double)ratioCounts[i]/ratioMapped;
		}
		/*
		 * Old, unnormalized way of comparing 3' and 5'.
		double[] ratioForRatio = new double[ratioLength/2];
		for(int i=0;i<ratioLength;i++){
			if(i%2==0){
					double tempVar = (double)ratioCounts[i]/(double)ratioCounts[i+1];
					//System.out.println(tempVar + " is the ratio we're trying to capture.");
					ratioForRatio[i/2]=tempVar;
			}
		}
		*/
		
		//Normalized comparison
		double[] ratioForRatio = new double[ratioLength/2];
		for(int i=0;i<ratioLength;i++){
			if(i%2==0){
					double tempVar = ((double)ratioCounts[i+1]-(double)ratioCounts[i+1])/houseKeepingReads;
					//System.out.println(tempVar + " is the ratio we're trying to capture.");
					ratioForRatio[i/2]=tempVar;
			}
		}
		/* 5. Write results to JSON
		 * Stores results in a Hashmap (keys: "summary", "rna.ratio", & "rna.fusion" that is written to JSON
		 * @author elainegee
		 */
	    //Build summary map
		Map<String, Object> summary = new HashMap<String, Object>();
		//TODO: Rename the keys for these Map/JSON entries
		summary.put("fraction of reads mapped to ratio reference", fracRatioMapped);
		summary.put("fraction of reads mapped to fusion reference", fracFusionMapped);
		summary.put("fraction of reads mapped to fusion reference passing fraction filter", fracFilterFusion);
		summary.put("fraction of reads filtered out for lengths < 40", fracShort40Mapped);
		summary.put("fraction of reads unmapped to ratio reference filtered out for lengths < 90", fracShort90Mapped);
		summary.put("fraction of reads filtered from fusion BAM by location", fracRemovedFilterFusion);
		summary.put("fraction of unmapped reads", fracUnmapped);
		summary.put("fraction of reads mapped to ratio reference passing map filter.",(double)passMappedRatio/InFq);
		summary.put("fraction of reads mapped to fusion reference passing map filter.",(double)passMappedFusion/InFq);
		summary.put("fraction of reads mapped to ratio reference passing both map and mismatch filters.",(double)passMatchRatio/InFq);
		summary.put("fraction of reads mapped to fusion reference passing both map and mismatch filters.",(double)passMatchFusion/InFq);
		summary.put("fraction of reads mapped to ratio reference failing map filter.",(double)mapFilterRatioCount/InFq);
		summary.put("fraction of reads mapped to fusion reference failing map filter.",(double)mapFilterFusionCount/InFq);
		summary.put("fraction of reads mapped to ratio reference failing match filter.",(double)mismatchFilterRatioCount/InFq);
		summary.put("fraction of reads mapped to fusion reference failing match filter.",(double)mismatchFilterFusionCount/InFq);		
		
		summary.put("count of reads mapped to ratio reference", ratioMapped);
		summary.put("count of reads mapped to fusion reference", fusionMapped);
		summary.put("count of reads mapped to fusion reference passing all filters",filterFusion);
		summary.put("count of reads filtered out for lengths < 40", short40);
		summary.put("count of reads unmapped to ratio reference filtered out for lengths < 90", short90);
		summary.put("count of reads filtered from fusion BAM by location",filteredFromFusion-mapFilterFusionCount-mismatchFilterFusionCount);
		summary.put("count of unmapped reads", fusionUnmapped);
		summary.put("count of reads mapped to ratio reference passing map filter.",passMappedRatio);
		summary.put("count of reads mapped to fusion reference passing map filter.",passMappedFusion);
		summary.put("count of reads mapped to ratio reference passing both map and mismatch filters.",passMatchRatio);
		summary.put("count of reads mapped to fusion reference passing both map and mismatch filters.",passMatchFusion);
		summary.put("count of reads mapped to ratio reference failing map filter.", mapFilterRatioCount);
		summary.put("count of reads mapped to fusion reference failing map filter.", mapFilterFusionCount);
		summary.put("count of reads mapped to ratio reference failing match filter.", mismatchFilterRatioCount);
		summary.put("count of reads mapped to fusion reference failing match filter.", mismatchFilterFusionCount);

		//Build rna ratio map
		Map<String, Object> rnaRatio = new HashMap<String, Object>();
		rnaRatio = buildFractionCountMap(RatioContigs, ratioCounts, ratioFrac);
		
		//Build rna fusion map
		Map<String, Object> rnaFusion = new HashMap<String, Object>();
		rnaFusion = buildFractionCountMap(FusionContigs, fusionCounts, fusionFrac);
		
		//Build final results map to be converted to JSON
		Map<String, Object> finalResults = new HashMap<String, Object>();
		finalResults.put( "summary", summary );
		finalResults.put( "rna.ratio", rnaRatio );
		finalResults.put( "rna.fusion", rnaFusion );

		//Convert final results to JSON
	    JSONObject json = new JSONObject(finalResults);
	    //System.out.printf( "JSON: %s", json.toString(2) );
	    
		//Get the json string, then compress it to a byte array
		String str = json.toString();
	    
		//Makes the JSON string human-readable. Requires GSON library.
		/*Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(json.toString());
		String str = gson.toJson(je);
		*/
		byte[] bytes = CompressGZIP.compressGZIP(str);
		
		// Write compresssed JSON to file
		//File dest = new File(getProjectHome() + "/rna_report.json.gz");
		File dest = this.getOutputBufferForClass(JSONBuffer.class).getFile();
		BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(dest));
		writer.write(bytes);
		writer.close();
	    
		return;
	}

	/**
	 * Synthesizes fraction & count information and return a map keyed by contig
	 * @author elainegee
	 * @return
	 */
	private Map<String, Object> buildFractionCountMap(String[] contigs, long[] counts, double[] fractions ) {
		Map<String, Object> results = new HashMap<String, Object>();
		// Pull out information for each contig
		for (int i=0; i < contigs.length; i++) {
			Map<String, Object> contigResults = new HashMap<String, Object>();
			//Store fraction info
			contigResults.put("fraction", fractions[i]);
			//Store count info
			contigResults.put("count", counts[i]);
			//Store fraction/count map into final result
			results.put(contigs[i], contigResults);	
		}
		return results;
	}
	
	
	public static long countLines(String filename) throws IOException {
	    InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    try {
	        byte[] c = new byte[1024];
	        long count = 0;
	        long readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++count;
	                }
	            }
	        }
	        return (count == 0 && !empty) ? 1 : count;
	    } finally {
	        is.close();
	    }
	}


}