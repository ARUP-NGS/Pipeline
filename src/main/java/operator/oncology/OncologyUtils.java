package operator.oncology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import json.JSONException;
import json.JSONObject;
import operator.IOOperator;
import operator.OperationFailedException;
import operator.bamutils.CountBAMRecords;
import pipeline.Pipeline;
import util.CompressGZIP;
import util.FastaReader;
import util.bamUtil.ReadCounter;
import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.JSONBuffer;
import buffer.ReferenceFile;


/*
 * @author daniel/elaine
 * Calculates ratios and counts for alignments to multiple custom reference files. 
 * 
 * 
 */
public class OncologyUtils extends IOOperator {

	public static final String SAMTOOLS_PATH = "samtools.path";
	public static final String INCLUDE_NTRK = "include.ntrk";
	public static String defaultSamPath = "samtools";

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		boolean runFailed = false;
		String samtoolsPath = defaultSamPath;
		String samAttr = this.getAttribute(SAMTOOLS_PATH);
		if (samAttr != null)
			samtoolsPath = samAttr;
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning utilities: Checking Arguments");
		System.out.println("Beginning utilities: Checking Arguments.");

		List<FileBuffer> FastqBuffers = this.getAllInputBuffersForClass(FastQFile.class); // Should contain 4 files
		List<FileBuffer> BamBuffers = this.getAllInputBuffersForClass(BAMFile.class);
		List<FileBuffer> CustomRefBuffers = this.getAllInputBuffersForClass(ReferenceFile.class);

		if (FastqBuffers.size() != 4) {
			System.out.println(FastqBuffers.size() + " fastq files provided.");
			throw new IllegalArgumentException(
					"4 Fastq files required as input.");
		}

		if (BamBuffers.size() != 11) {
			System.out.println(BamBuffers.size() + " bam files provided.");
			throw new IllegalArgumentException("11 BAM files required as input.");
		}

		if (CustomRefBuffers.size() != 3) {
			System.out.println(CustomRefBuffers.size()
					+ " fasta reference files provided.");
			throw new IllegalArgumentException(
					"3 fasta reference files required as input.");
		}

		logger.info("Counting reads in Fastq Files");
		System.out.println("Counting reads in Fastq Files");
		long InFq = countLines(FastqBuffers.get(0).getAbsolutePath()) / 4;
		/*
		long adapterTrimmedFq = countLines(FastqBuffers.get(1)
				.getAbsolutePath()) / 4;
		long UnmappedTermFq = countLines(FastqBuffers.get(2).getAbsolutePath()) / 4;
		long UnmappedFusFq = countLines(FastqBuffers.get(3).getAbsolutePath()) / 4;
		*/

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

		FastaReader FusionRefSplit = null;
		FusionRefSplit = new FastaReader(CustomRefBuffers.get(2).getFile());
		String[] FusionSplitContigs = FusionRefSplit.getContigs();
		int fusionSplitLength = FusionSplitContigs.length;
		
		/*
		 * 3. Count records for all bam files
		 */
		logger.info("Counting records in BAM Files");
		System.out.println("Counting records in BAM Files");
		// TODO: Create external operator to complete this task
		// TODO: Also, change the logging information
		CountBAMRecords counter = new CountBAMRecords();
		long ratioMapped = counter.CountRecords(BamBuffers.get(0));
		logger.info("Finished counting reads in BAM #1: mapped to ratio reference.");
		long ratioUnmapped = counter.CountRecords(BamBuffers.get(1));
		logger.info("Finished counting reads in BAM #2: unmapped to ratio reference.");
		long fusionMapped = counter.CountRecords(BamBuffers.get(2));
		logger.info("Finished counting reads in BAM #3: mapped to fusion reference");
		long fusionUnmapped = counter.CountRecords(BamBuffers.get(3));
		logger.info("Finished counting reads in BAM #4: unmapped to fusion reference.");
		long filterFusion = counter.CountRecords(BamBuffers.get(4));
		logger.info("Finished counting reads in BAM #5: mapped to fusion reference, passing bed filter.");
		long passMappedRatio = counter.CountRecords(BamBuffers.get(5));
		logger.info("Finished counting reads in BAM #6: mapped to ratio reference, passing amplicon coverage filter.");
		long passMatchRatio = counter.CountRecords(BamBuffers.get(6));
		logger.info("Finished counting reads in BAM #7: mapped to ratio reference, passing mismatch filter and the amplicon coverage filter.");
		long passMappedFusion = counter.CountRecords(BamBuffers.get(7));
		logger.info("Finished counting reads in BAM #8: mapped to fusion reference, passing amplicon coverage filter.");
		long passMatchFusion = counter.CountRecords(BamBuffers.get(8));
		logger.info("Finished counting reads in BAM #9: mapped to fusion reference, passing mismatch filter and the amplicon coverage filter.");
		long rescueMapped = counter.CountRecords(BamBuffers.get(9));
		logger.info("Finished counting reads in BAM #10: mapped to split fusion reference for rescue step.");
		long rescueUnmapped = counter.CountRecords(BamBuffers.get(10));
		logger.info("Finished counting reads in BAM #11: unmapped to all references.");

		long filteredFromFusion = fusionMapped - filterFusion;
		long mapFilterRatioCount = ratioMapped - passMappedRatio;
		long mismatchFilterRatioCount = passMappedRatio - passMatchRatio;
		long mapFilterFusionCount = fusionMapped - passMappedFusion;
		long mismatchFilterFusionCount = passMappedFusion - passMatchFusion;

		// Get map containing # of reads per contig

		String commandStr = samtoolsPath + " index " + BamBuffers.get(6).getAbsolutePath();
		executeCommand(commandStr);
		Map<String, Long> bamRatioMap = ReadCounter.countReadsByChromosome( (BAMFile) BamBuffers.get(6), 1);
		Set<String> keysRatio = bamRatioMap.keySet();
		Map<String, Long> ratioMap = new HashMap<String, Long>();
		for (String contig : RatioContigs) {
			ratioMap.put(contig, (long) 0);
		}
		
		for (String key : keysRatio) {
			//System.out.println(key + " is the key with value " + bamRatioMap.get(key).toString());
			ratioMap.put(key, bamRatioMap.get(key));
		}

		String commandStr1 = samtoolsPath + " index " + BamBuffers.get(4).getAbsolutePath();
		//System.out.println("Now executing " + commandStr1);
		executeCommand(commandStr1);
		Map<String, Long> bamFusionMap = ReadCounter.countReadsByChromosome(
				(BAMFile) BamBuffers.get(4), 1);
		Set<String> keysFusion = bamFusionMap.keySet();
		Map<String, Long> fusionMap = new HashMap<String, Long>();
		for (String contig : FusionContigs) {
			fusionMap.put(contig, (long) 0);
		}
		for (String key : keysFusion) {
			// System.out.println(key + " is the key with value " +
			// bamFusionMap.get(key).toString());
			fusionMap.put(key, bamFusionMap.get(key));
		}

		String commandStr2 = samtoolsPath + " index "
				+ BamBuffers.get(9).getAbsolutePath();
		logger.info("Now executing " + commandStr2);
		executeCommand(commandStr2);
		Map<String, Long> bamRescueMap = ReadCounter.countReadsByChromosome( (BAMFile) BamBuffers.get(9), 0);
		//System.out.println("Grabbing contigs from this bam file: " + BamBuffers.get(9).getAbsolutePath());
		Set<String> keysRescue = bamRescueMap.keySet();
		Map<String, Long> rescueMap = new HashMap<String, Long>();
		for (String contig : FusionSplitContigs) {
			rescueMap.put(contig, (long) 0);
		}
		for (String key : keysRescue) {
			rescueMap.put(key, bamRescueMap.get(key));
		}
		
		/*
		 * 4. Calculate ratios as needed
		 */
		logger.info("Now calculating counts for fastq and BAM files.");
		double fracRatioMapped = (double) ratioMapped / InFq;
		System.out.println("Printing fracRatioMapped " + fracRatioMapped
				+ " ratioMapped " + ratioMapped + " InFq " + InFq);
		logger.info("Printing fracRatioMapped " + fracRatioMapped
				+ " ratioMapped " + ratioMapped + " InFq " + InFq);
		double fracFusionMapped = (double) fusionMapped / InFq;
		System.out.println("Printing fracFusionMapped " + fracFusionMapped
				+ " fusionMapped " + fusionMapped + " InFq " + InFq);
		logger.info("Printing fracFusionMapped " + fracFusionMapped
				+ " fusionMapped " + fusionMapped + " InFq " + InFq);
		double fracFilterFusion = (double) filterFusion / InFq;
		double fracRemovedFilterFusion = (double) filteredFromFusion / InFq;
		double fracUnmapped = (double) fusionUnmapped / InFq;

		long[] fusionCounts = new long[fusionLength];
		long[] ratioCounts = new long[ratioLength];
		long[] fusionSplitCounts = new long[fusionSplitLength];
		double[] fusionFrac = new double[fusionLength];
		double[] ratioFrac = new double[ratioLength];
		double[] fusionSplitFrac = new double[fusionSplitLength];

		int houseKeepingReads = 0;

		for (int i = 0; i < fusionLength; i++) {
			fusionCounts[i] = fusionMap.get(FusionContigs[i]);
			if (fusionMapped != 0) {
				fusionFrac[i] = (double) fusionCounts[i] / fusionMapped;
			} else {
				fusionFrac[i] = -1729; // Dividing by zero is only for Ramanujan
				logger.info("The number of reads mapped to the fusion reference is 0, so a nonsense negative number is returned for the fraction.");
			}
			if (FusionContigs[i].toUpperCase().contains("CTRL")) {
				houseKeepingReads += (int) fusionCounts[i];
			}
		}
		if (houseKeepingReads == 0) {
			logger.info("!!!Experimental run failed - 0 reads for all control genes.!!!");
			runFailed = true;
		}
		for (int i = 0; i < ratioLength; i++) {
			ratioCounts[i] = ratioMap.get(RatioContigs[i]);
			if (ratioMapped != 0) {
				ratioFrac[i] = (double) ratioCounts[i] / ratioMapped;
			} else {
				ratioFrac[i] = -1337; // Dividing by 0 is only for 1337 H4XX0rZ
				logger.info("The number of reads mapped to the ratio reference is 0, so a nonsense negative number is returned for the fraction.");
			}
			System.out.println(Double.toString(ratioFrac[i])
					+ " is the value of this ratioFrac");
		}
		//TODO: Finish loading fusionSplitCounts and fusionSplitFrac
		for (int i = 0; i < fusionSplitLength; i++) {
			fusionSplitCounts[i] = rescueMap.get(FusionSplitContigs[i]);
			if (rescueMapped != 0) {
				fusionSplitFrac[i] = (double) fusionSplitCounts[i] / rescueMapped;
			} else {
				fusionSplitFrac[i] = -666; // Dividing by zero is the devil's business.
				logger.info("The number of reads mapped to the rescue reference is 0, so a nonsense negative number is returned for the fraction.");
			}
		}

		// Normalized comparison
		double[] ratioForRatio = new double[ratioLength / 2];
		if (!runFailed) {

			for (int i = 0; i < ratioLength; i++) {
				if (i % 2 == 0) {
					double tempVar = ((double) ratioCounts[i] - (double) ratioCounts[i + 1])
							/ houseKeepingReads;
					ratioForRatio[i / 2] = tempVar;
				}
			}
		} else {
			System.out
					.println("!!!Run failed. Zero counts for all control genes. Total reads in initial Fastq: "
							+ InFq + ".");
			logger.info("!!!Run failed. Zero counts for all control genes. Total reads in initial Fastq: "
					+ InFq + ".");
			for (double value : ratioForRatio) {
				value = 0;
			}
		}
		// Grabs every other chromosome
		String[] RatioContigSets = new String[RatioContigs.length / 2];
		long[] RatioCounts3p5p = new long[RatioContigs.length / 2];
		for (int i = 0; i < RatioContigSets.length; i++) {
			RatioContigSets[i] = RatioContigs[2 * i];
			RatioCounts3p5p[i] = ratioCounts[2 * i] + ratioCounts[2 * i + 1];
		}

		/*
		 * FOR DEBUGGING for (double value : RatioCounts3p5p) {
		 * System.out.println("Value of RatioCounts3p5p is (at this point) " +
		 * Double.toString(value));
		 * logger.info("Value of RatioCounts3p5p is (at this point) " +
		 * Double.toString(value)); }
		 */

		/*
		 * 5. Write results to JSON Stores results in a Hashmap (keys:
		 * "summary", "rna.ratio", & "rna.fusion" that is written to JSON
		 * 
		 * @author elainegee
		 */
		// Build summary map
		Map<String, Object> summary = new HashMap<String, Object>();
		// TODO: Rename the keys for these Map/JSON entries
		summary.put("fraction of reads mapped to ratio reference",
				fracRatioMapped);
		summary.put("fraction of reads mapped to fusion reference",
				fracFusionMapped);
		summary.put(
				"fraction of reads mapped to fusion reference passing fraction filter",
				fracFilterFusion);
		summary.put("fraction of reads filtered from fusion BAM by location",
				fracRemovedFilterFusion);
		summary.put("fraction of unmapped reads", fracUnmapped);
		summary.put(
				"fraction of reads mapped to ratio reference passing map filter.",
				(double) passMappedRatio / InFq);
		summary.put(
				"fraction of reads mapped to fusion reference passing map filter.",
				(double) passMappedFusion / InFq);
		summary.put(
				"fraction of reads mapped to ratio reference passing both map and mismatch filters.",
				(double) passMatchRatio / InFq);
		summary.put(
				"fraction of reads mapped to fusion reference passing both map and mismatch filters.",
				(double) passMatchFusion / InFq);
		summary.put(
				"fraction of reads mapped to ratio reference failing map filter.",
				(double) mapFilterRatioCount / InFq);
		summary.put(
				"fraction of reads mapped to fusion reference failing map filter.",
				(double) mapFilterFusionCount / InFq);
		summary.put(
				"fraction of reads mapped to ratio reference failing match filter.",
				(double) mismatchFilterRatioCount / InFq);
		summary.put(
				"fraction of reads mapped to fusion reference failing match filter.",
				(double) mismatchFilterFusionCount / InFq);

		summary.put("count of original reads", InFq);
		summary.put("count for all housekeeping genes", houseKeepingReads);
		summary.put("count of reads mapped to ratio reference", ratioMapped);
		summary.put("count of reads mapped to fusion reference", fusionMapped);
		summary.put(
				"count of reads mapped to fusion reference passing all filters",
				filterFusion);
		summary.put("count of reads filtered from fusion BAM by location",
				filteredFromFusion - mapFilterFusionCount
						- mismatchFilterFusionCount);
		summary.put("count of unmapped reads", fusionUnmapped);
		summary.put(
				"count of reads mapped to ratio reference passing map filter.",
				passMappedRatio);
		summary.put(
				"count of reads mapped to fusion reference passing map filter.",
				passMappedFusion);
		summary.put(
				"count of reads mapped to ratio reference passing both map and mismatch filters.",
				passMatchRatio);
		summary.put(
				"count of reads mapped to fusion reference passing both map and mismatch filters.",
				passMatchFusion);
		summary.put(
				"count of reads mapped to ratio reference failing map filter.",
				mapFilterRatioCount);
		summary.put(
				"count of reads mapped to fusion reference failing map filter.",
				mapFilterFusionCount);
		summary.put(
				"count of reads mapped to ratio reference failing match filter.",
				mismatchFilterRatioCount);
		summary.put(
				"count of reads mapped to fusion reference failing match filter.",
				mismatchFilterFusionCount);

		// Build rna ratio map
		Map<String, Object> rnaRatio = new HashMap<String, Object>();
		rnaRatio = buildFractionCountMap(RatioContigs, ratioCounts, ratioFrac);

		// Build RNA ratio map
		Map<String, Object> rnaRatioAdjusted = new HashMap<String, Object>();
		rnaRatioAdjusted = buildFractionCountMap(RatioContigSets,
				RatioCounts3p5p, ratioForRatio);

		// Build rna fusion map
		Map<String, Object> rnaFusion = new HashMap<String, Object>();
		rnaFusion = buildFractionCountMap(FusionContigs, fusionCounts,
				fusionFrac);
		
		//Build rescue step map
		Map<String, Object> rnaRescue = new HashMap<String, Object>();
		rnaRescue = buildFractionCountMap(FusionSplitContigs, fusionSplitCounts, fusionSplitFrac);
	
		// Convert final results to JSON
		JSONObject summaryjson = new JSONObject(summary);
		String summaryStr = summaryjson.toString();
		JSONObject ratiojson = new JSONObject(rnaRatio);
		String ratioStr = ratiojson.toString();
		JSONObject fusionjson = new JSONObject(rnaFusion);
		String fusionStr = fusionjson.toString();
		JSONObject ratioCalcJson = new JSONObject(rnaRatioAdjusted);
		String ratioCalcStr = ratioCalcJson.toString();
		JSONObject rescueJson = new JSONObject(rnaRescue);
		String rescueStr = rescueJson.toString();

		if (ratioStr == null) {
			throw new OperationFailedException("ratioStr is null. Abort!", this);
		}
		if (fusionStr == null) {
			throw new OperationFailedException("fusionStr is null. Abort!",
					this);
		}
		if (summaryStr == null) {
			throw new OperationFailedException("summaryStr is null. Abort!",
					this);
		}
		if (ratioCalcStr == null) {
			throw new OperationFailedException("ratioCalcStr is null. Abort!",
					this);
		}
		if (rescueStr == null) {
			throw new OperationFailedException("rescueJsonStr is null. Abort!", this);
		}
		System.out.println(ratioStr + " is ratio str");
		System.out.println(fusionStr + " is fusion str");
		System.out.println(summaryStr + " is summary str");
		System.out.println(ratioCalcStr + "is ratio calc str");
		System.out.println(rescueStr + "is rescue str");
		// System.out.printf( "JSON: %s", json.toString(2) );

		// Build final results map to be converted to JSON (OLD format for Validation)
//		Map<String, Object> finalResults = new HashMap<String, Object>();
//		finalResults.put("summary", summary);
//		finalResults.put("rna.ratio", rnaRatio);
//		finalResults.put("rna.fusion", rnaFusion);
//		finalResults.put("rna.adjusted.ratio", rnaRatioAdjusted);
//		finalResults.put("rna.rescue", rnaRescue);
		//JSONObject json = new JSONObject(finalResults);
		// Get the json string, then compress it to a byte array
		//String str = json.toString();
		
		boolean includeNTRK = false;
		if (this.getAttribute(INCLUDE_NTRK) != null) {
			includeNTRK = Boolean.parseBoolean(this.getAttribute(INCLUDE_NTRK));
		}
		
		JSONObject strNGSWebWithNTRK = getJSONStrforNGSWeb(summary, rnaFusion, rnaRatio, rnaRatioAdjusted, 
				FusionContigs, RatioContigSets, RatioContigs, false);
		
		JSONObject strNGSWebWithoutNTRK = getJSONStrforNGSWeb(summary, rnaFusion, rnaRatio, rnaRatioAdjusted, 
				FusionContigs, RatioContigSets, RatioContigs, true);
		
		
		byte[] bytes;
		if (includeNTRK) {
			bytes = CompressGZIP.compressGZIP(strNGSWebWithNTRK.toString());
		} else {
			bytes = CompressGZIP.compressGZIP(strNGSWebWithoutNTRK.toString());
		}
		
		File dest = this.getOutputBufferForClass(JSONBuffer.class).getFile();
		BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(dest));
		writer.write(bytes);
		writer.close();
		
		
		File withNTRKDest = new File(this.getProjectHome() + "fusions-ntrk.json");
		BufferedWriter ntrkwriter = new BufferedWriter(new FileWriter(withNTRKDest));
		ntrkwriter.write(strNGSWebWithNTRK.toString());
		ntrkwriter.close();
		
		return;
	}
	

	/**
	 * Writes out an NGS-Web friendly JSON output (contains Summary & Fusions keys). 
	 * Requires the previously calculated summary (read coverage), rnaFusion (counts), rnaRatio
	 * (counts for the contigs in the ratio reference), & rnaRatioAdjusted ((3'-5')/(hk genes)
	 * calc for potential/novel fusion genes) HashMaps & String arrays FusionContigs 
	 * (contains all target contigs) &  RatioContigSets (potential positive/novel fusion genes)
	 * 
	 * @author elainegee
	 * @return
	 * @throws IOException 
	 */
	private JSONObject getJSONStrforNGSWeb(Map<String, Object> summary, 
			Map<String, Object> rnaFusion, 
			Map<String, Object> rnaRatio, 
			Map<String, Object> rnaRatioAdjusted, 
			String[] FusionContigs, 
			String[] RatioContigSets,
			String[] RatioContigs,
			boolean removeNTRK) throws IOException {
		
		// Build final results map to be converted to JSON (NEW format for NGS.Web) 
		Map<String, Object> finalResultsNGSWeb = new HashMap<String, Object>();
		
		// Load up summary dictionary
		Map<String, Object> sumStats = new HashMap<String, Object>();
		sumStats.put("RatioMappedReads", summary.get("count of reads mapped to ratio reference"));
		sumStats.put("FusionMappedReads", summary.get("count of reads mapped to fusion reference"));
		finalResultsNGSWeb.put("Summary", sumStats);
		
		//Load up fusions dictionary, including housekeeping, target, & potential novel translocations
		Map<String, Object> fusions = new HashMap<String, Object>();
		//Load up housekeeping genes (ENCTRL = expression neutral control)
		String[] housekeepingGenes = {"MYC.ENCTRL.E2E3", "ITGB7.ENCTRL.E14E15", 
		                              "LMNA.ENCTRL.E3E4", "HMBS.ENCTRL.E8E9","TBP.ENCTRL.E3E4"};
		List<Map<String, Object>> listHKMaps = new ArrayList<Map<String, Object>>();
		for (String hkgene: housekeepingGenes){
			Map<String, Object> housekeeping = new HashMap<String, Object>();
			//Get gene name, exonStart, exonStop
			Map<String, Object> geneDetails = new HashMap<String, Object>();
			String[] toks=hkgene.split("\\.");
			// Get gene
			String gene = toks[0];
			geneDetails.put("name", gene);
			//Get exonStart, exonStop
			String exonStr = toks[2];
			String[] exonToks = getExonToksFromLocusStr(exonStr);
			geneDetails.put("exonStart", exonToks[0]);
			geneDetails.put("exonStop", exonToks[1]);
			//Load up parameters for given locus
			housekeeping.put("locus", hkgene);
			HashMap<String, Object> locusData = (HashMap<String, Object>) rnaFusion.get(hkgene);					
			housekeeping.put("count", locusData.get("count"));
			housekeeping.put("gene", geneDetails);
			listHKMaps.add(housekeeping);
			
		}
		//Add housekeeping genes to fusion map
		fusions.put("HouseKeeping", listHKMaps);

		//Load up target fusions (NOTE: FusionContigs contains control genes that will be skipped)
		String fusion = "";
		List<Map<String, Object>> listTargetMaps = new ArrayList<Map<String, Object>>();
		for (String targetFusion: FusionContigs) {
			if (!targetFusion.contains("ENCTRL") && (removeNTRK && !(targetFusion.contains("NTRK"))) ) {
				Map<String, Object> target = new HashMap<String, Object>(); //contains all fusion info
				ArrayList<Map> geneList = new ArrayList<Map>(); //contains both genes of fusion
				//Get gene name, exonStart, exonStop
				String[] toks=targetFusion.split("\\.");
				String[] geneToks = toks[0].split("\\-");
				String exonStr = toks[1];
				String[] exonToks = getExonToksFromLocusStr(exonStr);
				String cosmicid = "";
				String cosmicid_trim = "";
				if (toks.length > 2) {
					cosmicid = toks[2];
					Pattern cosf = Pattern.compile("^COSF(\\d+)$"); //remove label
					Matcher m = cosf.matcher(cosmicid);
					if (m.find()) {
						cosmicid_trim = m.group(1);
					} else {
						cosmicid_trim = cosmicid;
					}
				}	
				for (int i=0; i < geneToks.length; i++) {
					Map<String, Object> geneDetails = new HashMap<String, Object>(); //details for one gene
					geneDetails.put("name", geneToks[i]);
					geneDetails.put("exon", exonToks[i]);					
					geneList.add(geneDetails);
				}
				
				//Load up parameters for given locus
				target.put("COSMICID", cosmicid_trim);
				target.put("locus", targetFusion);
				HashMap<String, Object> locusData = (HashMap<String, Object>) rnaFusion.get(targetFusion);					
				target.put("count", locusData.get("count"));
				target.put("genes", geneList);
				listTargetMaps.add(target);
			}
		}
		//Add target fusions to fusion map
		fusions.put("Target", listTargetMaps);

				
		//Load up PotentialNovel fusions
		List<Map<String, Object>> listPotNovelMaps = new ArrayList<Map<String, Object>>();
		for (String PotNovelFusion: RatioContigSets) {  
			if (removeNTRK && (PotNovelFusion.contains("NTRK"))) {
				continue;
			}
			Map<String, Object> potNovel = new HashMap<String, Object>();
			Map<String, Object> geneDetails = new HashMap<String, Object>();
			//Get gene name, exonStart, exonStop
			String[] toks=PotNovelFusion.split("\\.");
			String gene = toks[0];
			geneDetails.put("name", gene);	
			//Get read count for 3' (this is in the RatioContigSets)
			ArrayList<String> countList = new ArrayList<String>();
			HashMap<String, Object> countData_3p = (HashMap<String, Object>) rnaRatio.get(PotNovelFusion);
			Long count_3p = (Long) countData_3p.get("count");
			countList.add(count_3p.toString());
			//Get read count for 5'
			String pattern_5p = gene + ".5" + toks[1].substring(1) +"." + toks[2]; 
			String PotNovelFusion_5p = "";
			//Find 5' partner in RatioContigs
			ArrayList<String> listRatioContigs = new ArrayList<String>();
			for (String ratioContig: RatioContigs) {
				listRatioContigs.add(ratioContig);
			}
			for (int i=0; i<RatioContigs.length; i++) {
				String ratioContig = RatioContigs[i]; 
				if (ratioContig.startsWith(pattern_5p)) {
					PotNovelFusion_5p = ratioContig;
				} else if (i == (RatioContigs.length - 1) && PotNovelFusion_5p.length() == 0) {
					throw new IOException("5' partner contig for RNA ratio locus '" + PotNovelFusion + "' that starts with '" + pattern_5p + "' not found in list of ratio contigs: " + listRatioContigs.toString());
				}
			}
			HashMap<String, Object> countData_5p = (HashMap<String, Object>) rnaRatio.get(PotNovelFusion_5p);
			Long count_5p = (Long) countData_5p.get("count");
			countList.add(count_5p.toString());			
			potNovel.put("count", countList.toString());
			
			//Load up parameters for given locus
			ArrayList<String> locusList = new ArrayList<String>();
			locusList.add(PotNovelFusion);
			locusList.add(PotNovelFusion_5p);
			potNovel.put("locus", locusList.toString()); 
			
			HashMap<String, Object> fractionData = (HashMap<String, Object>) rnaRatioAdjusted.get(PotNovelFusion);					
			potNovel.put("fraction", fractionData.get("fraction"));
			potNovel.put("gene", geneDetails);
			listPotNovelMaps.add(potNovel);
		}
		//Add potential positive/novel fusions to fusion map
		fusions.put("PotentialNovel", listPotNovelMaps);
		
		//Create final fusions HashMap to output
		finalResultsNGSWeb.put("Fusions", fusions);
		
		// Get the json string
		JSONObject jsonNGSWeb = new JSONObject(finalResultsNGSWeb);		
		//String result = jsonNGSWeb.toString();
		return jsonNGSWeb;
	}
	
	/**
	 * Extracts the exon start & exon stop from the exon string found  in the contig name
	 * e.g. E2E3 generates exonStart=2, exonStop=3 returned as ["2", "3"]
	 * 
	 * @author elainegee
	 * @return
	 */
	public static String[] getExonToksFromLocusStr(String exonStr) {
		String exonStart = "";
		String exonStop = "";
		Pattern pattern = Pattern.compile("[a-zA-Z](\\d+)[a-zA-Z](\\d+)");
		Matcher matcher = pattern.matcher(exonStr);
		while (matcher.find()) {
			exonStart = matcher.group(1);
			exonStop = matcher.group(2);
		}
		String[] results = {exonStart, exonStop};
		return results;
	}

	/**
	 * Synthesizes fraction & count information and return a map keyed by contig
	 * 
	 * @author elainegee
	 * @return
	 */
	private Map<String, Object> buildFractionCountMap(String[] contigs,
			long[] counts, double[] fractions) {
		Map<String, Object> results = new HashMap<String, Object>();
		// Pull out information for each contig
		for (int i = 0; i < contigs.length; i++) {
			Map<String, Object> contigResults = new HashMap<String, Object>();
			// Store fraction info
			contigResults.put("fraction", fractions[i]);
			// Store count info
			contigResults.put("count", counts[i]);
			// Store fraction/count map into final result
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
