package util.comparators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import json.JSONException;
import json.JSONObject;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import operator.IOOperator;
import pipeline.Pipeline;
import util.comparators.ReviewDirComparator.Severity;
import util.reviewDir.ManifestParseException;
import util.reviewDir.ReviewDirectory;


/*
 * Compares review directories, including VCFs, CSVs, and QC Metrics.
 * Contains two ReviewDirectory objects. Can be run using the performOperation override i.e. as a pipeline operation or through the main class.
 * @author Kevin Boehme
 */

public class CompareReviewDirs extends IOOperator {

	private static ManifestSummaryComparator manifestSummaryComparator = null;
	private static QCJSONComparator qcJSONComparator = null;
	private static VCFComparator vcfComparator = null;
	//private static AnnotationComparator annotationComparator = null;
	private static AnnotatedJSONComparator annotatedJSONComparator = null;

	Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

	private ReviewDirectory rd1 = null;
	private ReviewDirectory rd2 = null;

	public CompareReviewDirs(String reviewdir1, String reviewdir2) throws IOException, ManifestParseException {
		rd1 = new ReviewDirectory(reviewdir1);
		rd2 = new ReviewDirectory(reviewdir2);
	}

	/**
	 * @param args
	 * @throws ManifestParseException 
	 * @throws IOException 
	 * @throws ArgumentParserException 
	 * @throws JSONException 
	 */
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
	}
	
	public void compare() throws IOException, JSONException {
		logger.info("Begin Review Directory Comparison.");
		LinkedHashMap<String, Object> finalJSONOutput = new LinkedHashMap<String, Object>();
		
		//Compare Manifest information
		manifestSummaryComparator = new ManifestSummaryComparator(rd1, rd2, "Summary Information Comparison");
		manifestSummaryComparator.performOperation();
		finalJSONOutput.put("manifest.summary", manifestSummaryComparator.getJSONOutput());

		//Compare QC metrics
		qcJSONComparator = new QCJSONComparator(rd1, rd2, "(Raw) QC Metrics Comparison");
		qcJSONComparator.performOperation();
		finalJSONOutput.put("qcjson.summary", qcJSONComparator.getJSONOutput());

		//Compare VCFs
		vcfComparator = new VCFComparator(rd1, rd2, "VCF Comparison");
		vcfComparator.performOperation();
		finalJSONOutput.put("vcf.summary", vcfComparator.getJSONOutput());

		//Compare annotations.
		//annotationComparator = new AnnotationComparator(rd1, rd2, "Variant annotation comparison");
		//annotationComparator.performOperation();
		//finalJSONOutput.put("annotations.summary", annotationComparator.getJSONOutput());

		//Compare annotations.
		annotatedJSONComparator = new AnnotatedJSONComparator(rd1, rd2, "Variant annotation comparison");
		annotatedJSONComparator.performOperation();
		finalJSONOutput.put("annotations.summary", annotatedJSONComparator.getJSONOutput());		
		
		List<Map<Severity,Integer>> summaryList = new ArrayList();
		summaryList.add(manifestSummaryComparator.getSeveritySummary());
		summaryList.add(qcJSONComparator.getSeveritySummary());
		summaryList.add(vcfComparator.getSeveritySummary());
		summaryList.add(annotatedJSONComparator.getSeveritySummary());
		this.generateComparisonSummary(summaryList);
		
		JSONObject ResultsJson = new JSONObject(finalJSONOutput);
		String ResultsStr = ResultsJson.toString();
		//System.out.println(ResultsStr);
	}
	
	private void generateComparisonSummary(List<Map<Severity,Integer>> summaryList) {
		for (Map<Severity,Integer> curMap : summaryList) {
			for (Map.Entry<Severity, Integer> entry : curMap.entrySet()) {
			    Severity sev = entry.getKey();
			    Integer value = entry.getValue();
			}
		}
	}
	
	@Override
	public void performOperation() throws IOException, JSONException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		// Get folder locations
		String revDirLoc1 = this.getAttribute("ReviewDir1");
		String revDirLoc2 = this.getAttribute("ReviewDir2");

		try {
			this.rd1 = new ReviewDirectory(revDirLoc1);
			this.rd2 = new ReviewDirectory(revDirLoc2);
			this.compare();
		} catch (ManifestParseException e) {
			e.printStackTrace();
		}
	}
}