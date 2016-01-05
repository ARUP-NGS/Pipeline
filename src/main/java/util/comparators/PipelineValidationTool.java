package util.comparators;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import json.JSONException;
import json.JSONObject;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import util.comparators.CompareReviewDirs.DiscordanceSummary;
import util.comparators.CompareReviewDirs.Severity;
import util.reviewDir.ManifestParseException;
import util.reviewDir.ReviewDirectory;

public class PipelineValidationTool {

	public PipelineValidationTool() {
	}

	public static void main(String[] args) {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("CompareReviewDirs.jar")
			.defaultHelp(true)
			.description("Perform comparisons of Pipeline review directories, comparing annotations, BAM coverage/metrics, variants, run times, etc. "
			+ "If given two directories full of RDs this tools performs a comprehensive comparison between all RDs, intelligently comparing runs "
			+ "that used the same fastq file (so ideally, you have a truth set directories of RDs and a new test set run using a newer"
			+ "pipeline version and fastq names werent changed). Used in validations, and produces a PASS or FAIL along with a detailed summary.");
		
		parser.addArgument("-i", "--input")
			.nargs(2)
			.required(true)
			.help("Enter two paths: Either two RDs or two folders of RDs (in the case of validations).");
		parser.addArgument("-o", "--out")
			.nargs(1)
			.required(true)
			.help("Name of output file, where a summary of the comparison will be written (will write a json and a summary file).");
		
		Namespace ns = null;
		List<String> paths = null;
		String outFileName = "";
		try {
		    ns = parser.parseArgs(args);
			paths = ns.getList("input");
			outFileName = ns.getString("out").replace("[", "").replace("]", ""); //whatahack
		} catch (ArgumentParserException e) {
		    parser.handleError(e);
		    System.exit(1);
		}
				
		try {
			//kinda a hack, this will make sure both paths are valid RDs, if not we hit the catch and try a validation.
			ReviewDirectory rd1 = new ReviewDirectory(paths.get(0));
			ReviewDirectory rd2 = new ReviewDirectory(paths.get(1));
			
			compareTwoDirectories(paths.get(0), paths.get(1), outFileName);
			
		} catch (ManifestParseException e) {
			System.out.println("Seems like we are doing a validation.");
			validatePipeline(paths.get(0), paths.get(1), outFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Simply given two paths this will compare the two review directories.
	 * @param path1
	 * @param path2
	 * @param outFileName
	 */
	private static void compareTwoDirectories(String path1, String path2, String outFileName) {
		CompareReviewDirs crd;
		try {
			crd = new CompareReviewDirs(path1, path2);
			crd.compare();
			
			writeJSONToFile(new JSONObject(crd.getFinalJSONOutput()), outFileName);
			
		} catch (IOException | ManifestParseException | JSONException e) {
			e.printStackTrace();
		}
	}
	
	/** Given two paths this will perform a validation on all contained review directories.
	 * @param path1
	 * @param path2
	 * @param outFileName
	 */
	private static void validatePipeline(String path1, String path2, String outFileName) {
		
		Map<String, List<ReviewDirectory>> comparisonMap = prepareComparisons(path1, path2);
		List<CompareReviewDirs> crds = performComparisons(comparisonMap);
		summarizeComparison(crds, outFileName);
	}
	
	/** Given two paths to directories of review directories this will match up RDs with the same fastq and put in a map.
	 * 
	 * @param path1
	 * @param path2
	 * @return
	 */
	private static Map<String, List<ReviewDirectory>> prepareComparisons(String path1, String path2) {
		Map<String, List<ReviewDirectory>> comparisonMap = new HashMap<String, List<ReviewDirectory>>();

		File dir1 = new File(path1);
		File dir2 = new File(path2);
		if (dir1.isDirectory() && dir2.isDirectory()) {
			if(dir1.list().length > 0 && dir2.list().length > 0) {
				// Prepare comparison ------------------------------------------------------------------------------
				System.out.println("Begining validation of: " + dir1.getName() + " and " + dir2.getName());
				Map<ReviewDirectory, String> reviewDirPathMap = new HashMap<ReviewDirectory, String>();
				
				ArrayList<ReviewDirectory> RDs1 = new ArrayList<ReviewDirectory>();
				ArrayList<ReviewDirectory> RDs2 = new ArrayList<ReviewDirectory>();
				
				for (File f : dir1.listFiles()) {
					try {
						ReviewDirectory newRD = new ReviewDirectory(f.getAbsolutePath());
						RDs1.add(newRD);
						reviewDirPathMap.put(newRD, f.getAbsolutePath());
					} catch (IOException | ManifestParseException ex) {
						ex.printStackTrace();
					}
				}

				for (File f : dir2.listFiles()) {
					try {
						ReviewDirectory newRD = new ReviewDirectory(f.getAbsolutePath());
						RDs2.add(newRD);
						reviewDirPathMap.put(newRD, f.getAbsolutePath());
					} catch (IOException | ManifestParseException ex) {
						ex.printStackTrace();
					}
				}
				
				System.out.println(dir1.getName() + " has " + String.valueOf(RDs1.size()) + " review directories." );
				System.out.println(dir2.getName() + " has " + String.valueOf(RDs2.size()) + " review directories." );
				//Now lets populate our comparisonMap.
				for (ReviewDirectory rd1 : RDs1) {
					String[] rd1Fastqs = rd1.getLogFile().getFastqNames();
					
					for (ReviewDirectory rd2 : RDs2) {
						String[] rd2Fastqs = rd2.getLogFile().getFastqNames();
						if( Arrays.equals(rd1Fastqs, rd2Fastqs) ) {
							List<ReviewDirectory> rds = new ArrayList<ReviewDirectory>();
							//Make sure the older run RD gets put in the first column as our truth set.
							if(Long.valueOf(rd1.getSampleManifest().getTime()) < Long.valueOf(rd2.getSampleManifest().getTime())) { //rd1 is the older truth set.
								rds.add(rd1);
								rds.add(rd2);
							} else { //rd1 is newer and as such is the test set.
								rds.add(rd2);
								rds.add(rd1);
							}
							comparisonMap.put(rd1Fastqs[0], rds );
						}
					}
				}
			} else {
				System.out.println("It seems one (or both) the directories given are empty:" + dir1.getName() + " and " + dir2.getName());
				System.exit(0);
			}
		} else {
			System.out.println("It seems one (or both) of the inputs are either not directories or don't exist.");
			System.exit(0);
		}
		return comparisonMap;
	}
	
	/** Given a comparison map this will loop through and compare Review Directories.
	 * @param comparisonMap
	 * @return
	 */
	private static List<CompareReviewDirs> performComparisons(Map<String, List<ReviewDirectory>> comparisonMap) {
		List<CompareReviewDirs> crds = new ArrayList<CompareReviewDirs>();
		for (Map.Entry<String, List<ReviewDirectory>> entry : comparisonMap.entrySet()) {
			try {
				CompareReviewDirs crd = new CompareReviewDirs(entry.getValue().get(0).getSourceDirPath(), entry.getValue().get(1).getSourceDirPath());
				crd.compare();
				crds.add(crd);
				//Now collect relevant summary information from our comparator class.
				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n");
				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

			} catch (IOException | ManifestParseException | JSONException ex) {
				System.out.println("Error with comparison for RDs: " + entry.getValue().get(0).getSourceDirPath() + " and " + entry.getValue().get(1).getSourceDirPath());
				ex.printStackTrace();
			}
		}
		return crds;
	}
	
	/** Given a list of CompareReviewDirs objects this will loop through each one and summarize their results and produce a summary JSON String.
	 * @param crds
	 * @param outFileName
	 */
	private static void summarizeComparison(List<CompareReviewDirs> crds, String outFileName) {
		LinkedHashMap<String, Object> validationSummary = new LinkedHashMap<String, Object>();
		//validationSummary.put("severity.key", getNames(Severity.class));
		System.out.println("\n\n+++++++++++++++++++++++++");
		System.out.println("| Summary of Validation |");
		System.out.println("+++++++++++++++++++++++++");
		
		Map<String, DiscordanceSummary> valSummary = new LinkedHashMap<String, DiscordanceSummary>();
		Map<String, Object> validationJSON = new LinkedHashMap<String, Object>();
		for (CompareReviewDirs crd : crds) {
			String comparisonName = "(" + crd.getRd1().getAnalysisType() + ") " + crd.getRd1().getSampleName() + "-" + crd.getRd2().getSampleName();
			valSummary.put(comparisonName, crd.getDiscordanceSummary());
			validationJSON.put(comparisonName, crd.getFinalJSONOutput());	
		}
		
		for (Severity sev: Severity.values()) {
			if (!sev.toString().equals("EXACT")) {
				ComparisonSummaryTable st = new ComparisonSummaryTable(sev.toString(), Arrays.asList("#", "Type", ""));
				LinkedHashMap<String, Object> sevJSON = new LinkedHashMap<String, Object>();

				for (Map.Entry<String, DiscordanceSummary> entry : valSummary.entrySet()) {
					String comparisonName = entry.getKey();
					DiscordanceSummary disSum = entry.getValue();
					
					List<String> newRow = new ArrayList<>();
					newRow.add(comparisonName);
					
					int sum = disSum.getSeveritySummary(sev).size();
					//if (sum > 0) {
					String sevNum = String.valueOf(sum);
					newRow.add(sevNum);
					
					String sevMap = disSum.getSeveritySummary(sev).toString();
					newRow.add(sevMap);
					newRow.add("");
					
					String[] summaryArray = {sevNum, sevMap};
					//validationSummary.put(comparisonName, summaryArray);
					sevJSON.put(comparisonName, summaryArray);
					st.addRow(newRow);
					//}				
				}
				st.printSummaryTable();
				validationSummary.put(sev.toString(), sevJSON);
			}
		}
		validationJSON.put("validation", validationSummary);
		
		writeJSONToFile(new JSONObject(validationJSON), outFileName);
	}
	
	private static void writeJSONToFile(JSONObject json, String outFileName) {
		try (FileWriter file = new FileWriter(outFileName + ".json")) {
			file.write(json.toString());
			System.out.println("\nSuccessfully Copied JSON Object to File: " + outFileName + ".json");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
