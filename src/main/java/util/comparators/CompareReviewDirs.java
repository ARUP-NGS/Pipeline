package util.comparators;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import json.JSONException;
import json.JSONObject;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
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
		TWONUMBERS, ONENUMBER, EXACTNUMBER, TEXT, TIME, ANNOTATIONS, NONE
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
	
	/**
	 * @param args
	 * @throws ManifestParseException 
	 * @throws IOException 
	 * @throws ArgumentParserException 
	 * @throws JSONException 
	 */
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
			outFileName = ns.getString("out").replace("[", "").replace("]", "");
		} catch (ArgumentParserException e) {
		    parser.handleError(e);
		    System.exit(1);
		}
		System.out.println(paths);
		System.out.println(outFileName);
		//check if inputs are valid RDs if not, assume its a validation.
		try {
			ReviewDirectory rd1 = new ReviewDirectory(paths.get(0));
			ReviewDirectory rd2 = new ReviewDirectory(paths.get(1));
			CompareReviewDirs crd = new CompareReviewDirs(paths.get(0), paths.get(1));
			crd.compare();
			
			try (FileWriter file = new FileWriter(outFileName + ".json")) {
				file.write(new JSONObject(crd.getFinalJSONOutput()).toString());
				System.out.println("Successfully Copied JSON Object to File: " + outFileName + ".json");
			}
			
		} catch (ManifestParseException e) {
			PrintStream out = System.out;
			if (paths.get(0) == "" || paths.get(1) == "") {
				out.println("Error with input.");
				return;
			}
			
			File dir1 = new File(paths.get(0));
			File dir2 = new File(paths.get(1));
			if (dir1.isDirectory() && dir2.isDirectory()) {
				if(dir1.list().length > 0 && dir2.list().length > 0) {
					// Prepare comparison ------------------------------------------------------------------------------
					System.out.println("Begining validation of: " + dir1.getName() + " and " + dir2.getName());
					Map<String, List<ReviewDirectory>> comparisonMap = new HashMap<String, List<ReviewDirectory>>();
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
								if(Long.valueOf(rd1.getSampleManifest().getTime()) < Long.valueOf(rd2.getSampleManifest().getTime())) {
									rds.add(rd1);
									rds.add(rd2);
								} else {
									rds.add(rd2);
									rds.add(rd1);
								}
								comparisonMap.put(rd1Fastqs[0], rds );
							}
						}
					}
					// End Prepare comparison --------------------------------------------------------------------------
					
					//Start processing summary of comparison -----------------------------------------------------------------
					Map<String, DiscordanceSummary> valSummary = new HashMap<String, DiscordanceSummary>();
					
					//Map<Severity, Integer> severitySummary = new HashMap<Severity, Integer> ();
					LinkedHashMap<String, Object> validationJSON = new LinkedHashMap<String, Object>();

					for (Map.Entry<String, List<ReviewDirectory>> entry : comparisonMap.entrySet()) {
						try {
							CompareReviewDirs crd = new CompareReviewDirs(entry.getValue().get(0).getSourceDirPath(), entry.getValue().get(1).getSourceDirPath());
							crd.compare();
							//Now collect relevant summary information from our comparator class.
							System.out.println("===================================================");
							String comparisonName = crd.getRd1().getSampleName() + "-" + crd.getRd2().getSampleName();
							valSummary.put(comparisonName, crd.getDiscordanceSummary());
							validationJSON.put(comparisonName, crd.getFinalJSONOutput());
							//valSummary.add(crd.getSummary());
						} catch (IOException | ManifestParseException | JSONException ex) {
							System.out.println("Error with comparison for RDs: " + entry.getValue().get(0).getSourceDirPath() + " and " + entry.getValue().get(1).getSourceDirPath());
							ex.printStackTrace();
						}
					}
					
					LinkedHashMap<String, Object> validationSummary = new LinkedHashMap<String, Object>();
					//validationSummary.put("severity.key", getNames(Severity.class));
					System.out.println("\n\n+++++++++++++++++++++++++");
					System.out.println("| Summary of Validation |");
					System.out.println("+++++++++++++++++++++++++");

					for (Severity sev: Severity.values()) {
						if (!sev.toString().equals("EXACT")) {
							ComparisonSummaryTable st = new ComparisonSummaryTable();
							st.setCompareType(sev.toString());
							st.setColNames(Arrays.asList("#", "Type", ""));
							LinkedHashMap<String, Object> sevJSON = new LinkedHashMap<String, Object>();

							for (Map.Entry<String, DiscordanceSummary> entry : valSummary.entrySet()) {
								String comparisonName = entry.getKey();
								DiscordanceSummary disSum = entry.getValue();
								
								List<String> newRow = new ArrayList<>();
								newRow.add(comparisonName);
								
								//newRow.add(sev.toString());
								Integer sum = 0;
								for (AtomicInteger i : disSum.getSeveritySummary(sev).values()) {
								    sum += i.get();
								}
								if (sum > 0) {
									String sevNum = String.valueOf(sum);
									newRow.add(sevNum);
									
									String sevMap = disSum.getSeveritySummary(sev).keySet().toString();
									newRow.add(sevMap);
									newRow.add("");
									
									String[] summaryArray = {sevNum, sevMap};
									//validationSummary.put(comparisonName, summaryArray);
									sevJSON.put(comparisonName, summaryArray);
									st.addRow(newRow);
								}				
							}
							st.printSeverityTable();
							validationSummary.put(sev.toString(), sevJSON);
						}
					}
					validationJSON.put("validation", validationSummary);
					//String jsonString = new JSONObject(validationJSON).toString();
					//System.out.println(jsonString);
					
					// try-with-resources statement based on post comment below :)
					try (FileWriter file = new FileWriter(outFileName + ".json")) {
						file.write(new JSONObject(validationJSON).toString());
						System.out.println("Successfully Copied JSON Object to File: " + outFileName + ".json");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					//END processing summary of comparison -----------------------------------------------------------------

				} else {
					System.out.println("It seems one (or both) the directories given are empty:" + dir1.getName() + " and " + dir2.getName());
					return;
				}
			} else {
				System.out.println("It seems one (or both) of the inputs are either not directories or don't exist.");
				return;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
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