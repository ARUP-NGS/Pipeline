package util.comparators;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import json.JSONException;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import operator.IOOperator;
import pipeline.Pipeline;
import util.comparators.ReviewDirComparator.Severity;
import util.reviewDir.ManifestParseException;
import util.reviewDir.ReviewDirectory;


/*
 * Compares review directories, including VCFs, CSVs, and QC Metrics.
 * Contains two ReviewDirectory objects. Can be run using the performOperation override i.e. as a pipeline operation or through the main class.
 * To use:
 * 	CompareReviewDirs crd = new CompareReviewDirs(path1, path2);
	crd.compare();
						
 * @author Kevin Boehme
 */

public class CompareReviewDirs extends IOOperator {

	private static ManifestSummaryComparator manifestSummaryComparator = null;
	private static QCJSONComparator qcJSONComparator = null;
	private static VCFComparator vcfComparator = null;
	private static AnnotatedJSONComparator annotatedJSONComparator = null;
	
	private Map<Severity, List<String>> severityTotals = new HashMap<Severity, List<String>>();

	//private ComparisonSummaryTable summary = new ComparisonSummaryTable();
	private LinkedHashMap<String, Object> finalJSONOutput = new LinkedHashMap<String, Object>();
	
	private Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
	
	
	
	private ReviewDirectory rd1 = null;
	private ReviewDirectory rd2 = null;

	public CompareReviewDirs(String reviewdir1, String reviewdir2) throws IOException, ManifestParseException {
		rd1 = new ReviewDirectory(reviewdir1);
		rd2 = new ReviewDirectory(reviewdir2);
	}

/*	*//**
	 * @param args
	 * @throws ManifestParseException 
	 * @throws IOException 
	 * @throws ArgumentParserException 
	 * @throws JSONException 
	 *//*
	public static void main(String[] args) throws IOException, ManifestParseException, ArgumentParserException, JSONException {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("compare");
		parser.description("Compare two Review Directories from Pipeline.");
		
		parser.addArgument("reviewdir1")
		.type(String.class)
		.help("Path to review directory 1.");
		parser.addArgument("reviewdir2")
		.type(String.class)
		.help("Path to review directory 2.");

		Namespace parsedArgs = null;
		String reviewdir1 = "";
		String reviewdir2 = "";

		parsedArgs = parser.parseArgs(args);
		reviewdir1 = (String) parsedArgs.get("reviewdir1");
		reviewdir2 = (String) parsedArgs.get("reviewdir2");

		CompareReviewDirs crd = new CompareReviewDirs(reviewdir1, reviewdir2);
		crd.compare();
	}*/
	
	public void compare() throws IOException, JSONException {
		logger.info("Begin Review Directory Comparison.");
		
		//Compare Manifest information
		manifestSummaryComparator = new ManifestSummaryComparator(rd1, rd2, "Summary Information Comparison");
		manifestSummaryComparator.performOperation();
		finalJSONOutput.put("compare.manifest", manifestSummaryComparator.getJSONOutput());

		//Compare QC metrics
		qcJSONComparator = new QCJSONComparator(rd1, rd2, "(Raw) QC Metrics Comparison");
		qcJSONComparator.performOperation();
		finalJSONOutput.put("compare.qcjson", qcJSONComparator.getJSONOutput());

		//Compare VCFs
		vcfComparator = new VCFComparator(rd1, rd2, "VCF Comparison");
		vcfComparator.performOperation();
		finalJSONOutput.put("compare.vcf", vcfComparator.getJSONOutput());

		//Compare annotations.
		annotatedJSONComparator = new AnnotatedJSONComparator(rd1, rd2, "annotated.json");
		annotatedJSONComparator.performOperation();
		finalJSONOutput.put("compare.annotatedjson", annotatedJSONComparator.getJSONOutput());		
		
		severityTotals.putAll(manifestSummaryComparator.getSeveritySummary());
		severityTotals.putAll(qcJSONComparator.getSeveritySummary());
		severityTotals.putAll(vcfComparator.getSeveritySummary());
		severityTotals.putAll(annotatedJSONComparator.getSeveritySummary());
		
		
	}
	
	public Map<Severity, List<String>> getSeverityTotals() {
		return severityTotals;
	}

	public void setSeverityTotals(Map<Severity, List<String>> severityTotals) {
		this.severityTotals = severityTotals;
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