package util.comparators;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import json.JSONException;
import operator.IOOperator;
import pipeline.Pipeline;
import util.reviewDir.ManifestParseException;
import util.reviewDir.ReviewDirectory;


/*
 * Compares review directories, including VCFs, CSVs, and QC Metrics.
 * Contains two ReviewDirectory objects as well as a specific implementation of the ReviewDirComparator base class for each comparison type (vcf, qc.json, annotated.json).
 * Can be run using the performOperation override i.e. as a pipeline operation or through the main class.
 * To use:
 * 	CompareReviewDirs crd = new CompareReviewDirs(path1, path2);
 *	crd.compare();
 *						
 * @author Kevin Boehme
 */

public class CompareReviewDirs extends IOOperator {

	private static ManifestSummaryComparator manifestSummaryComparator = null;
	private static QCJSONComparator qcJSONComparator = null;
	private static VCFComparator vcfComparator = null;
	private static AnnotatedJSONComparator annotatedJSONComparator = null;
	
	private DiscordanceSummary discordanceSummary = new DiscordanceSummary();

	private LinkedHashMap<String, Object> finalJSONOutput = new LinkedHashMap<String, Object>();
	
	private Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
	
	private ReviewDirectory rd1 = null;
	private ReviewDirectory rd2 = null;

	public CompareReviewDirs(String reviewdir1, String reviewdir2) throws IOException, ManifestParseException {
		rd1 = new ReviewDirectory(reviewdir1);
		rd2 = new ReviewDirectory(reviewdir2);
	}
	
	public enum Severity {
		MAJOR, MODERATE, MINOR, EXACT
	}
	
	public enum ComparisonType {
		TWONUMBERS, EXACTNUMBERS, TEXT, TIME, ANNOTATIONS, NONE
	}
	
	/** Just a wrapper for a map for each of the severity classes. It provides convenient helper functions
	 *  to access and increment summaries.
	 * @author kevin
	 *
	 */
	public static class DiscordanceSummary {
		
		private Map<Severity, Map<String, AtomicInteger>> severitySummary = new HashMap<Severity, Map<String, AtomicInteger>>();
		
		public DiscordanceSummary() {
			for (Severity sev : Severity.values()) {
				this.severitySummary.put(sev, new HashMap<String, AtomicInteger>());
			}
		}
		
		public void putAllSeverity(Severity sev, Map<String, AtomicInteger> sevSum) {
			this.severitySummary.get(sev).putAll(sevSum);
		}
		
		public Map<String, AtomicInteger> getSeveritySummary(Severity sev) {
			return severitySummary.get(sev);
		}
		
		public void addNewDiscordance(Severity sev, String key) {
			if (this.severitySummary.get(sev).get(key) != null) {
				this.severitySummary.get(sev).get(key).incrementAndGet();
			} else {
				this.severitySummary.get(sev).put(key, new AtomicInteger(1));
			}
		}

		/** This will loop through the given DiscordanceSummary object and add its contents to this DiscordanceSummary object.
		 * @param disSum
		 */
		public void collect(DiscordanceSummary disSum) {
			for (Severity sev : Severity.values()) {
				this.putAllSeverity(sev, disSum.getSeveritySummary(sev));
			}
		}
	}
	
	public void compare() throws IOException, JSONException {
		logger.info("Begin Review Directory Comparison.");
		
		//Compare Manifest information
		manifestSummaryComparator = new ManifestSummaryComparator(rd1, rd2, "Summary Information Comparison");
		manifestSummaryComparator.performOperation();
		finalJSONOutput.put("compare.manifest", manifestSummaryComparator.getJSONOutput());

		//Compare QC metrics
		qcJSONComparator = new QCJSONComparator(rd1, rd2, "(Final) QC Metrics Comparison");
		qcJSONComparator.performOperation();
		finalJSONOutput.put("compare.qcjson", qcJSONComparator.getJSONOutput());

		//Compare VCFs
		vcfComparator = new VCFComparator(rd1, rd2, "VCF Comparison");
		vcfComparator.performOperation();
		finalJSONOutput.put("compare.vcf", vcfComparator.getJSONOutput());

		//Compare annotations.
		annotatedJSONComparator = new AnnotatedJSONComparator(rd1, rd2, "Annotated JSON");
		annotatedJSONComparator.performOperation();
		finalJSONOutput.put("compare.annotatedjson", annotatedJSONComparator.getJSONOutput());		

		//Collect up all of the discordance information from each comparator.
		discordanceSummary.collect(manifestSummaryComparator.getDiscordanceSummary());
		discordanceSummary.collect(qcJSONComparator.getDiscordanceSummary());
		discordanceSummary.collect(vcfComparator.getDiscordanceSummary());
		//discordanceSummary.collect(annotatedJSONComparator.getDiscordanceSummary());
	}
	
	public DiscordanceSummary getDiscordanceSummary() {
		return discordanceSummary;
	}

	public void setDiscordanceSummary(DiscordanceSummary discordanceSummary) {
		this.discordanceSummary = discordanceSummary;
	}
	
	public ReviewDirectory getRd1() {
		return rd1;
	}

	public void setRd1(ReviewDirectory rd1) {
		this.rd1 = rd1;
	}

	public ReviewDirectory getRd2() {
		return rd2;
	}

	public void setRd2(ReviewDirectory rd2) {
		this.rd2 = rd2;
	}

	public LinkedHashMap<String, Object> getFinalJSONOutput() {
		return finalJSONOutput;
	}

	public void setFinalJSONOutput(LinkedHashMap<String, Object> finalJSONOutput) {
		this.finalJSONOutput = finalJSONOutput;
	}
	
	@Override
	public void performOperation() {
		System.out.println("Not implemented");
/*		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		// Get folder locations
		String revDirLoc1 = this.getAttribute("ReviewDir1");
		String revDirLoc2 = this.getAttribute("ReviewDir2");

		try {
			this.rd1 = new ReviewDirectory(revDirLoc1);
			this.rd2 = new ReviewDirectory(revDirLoc2);
			this.compare();
		} catch (ManifestParseException e) {
			e.printStackTrace();
		}*/
	}
}