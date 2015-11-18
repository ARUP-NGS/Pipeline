package util.comparators;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import json.JSONException;
import pipeline.Pipeline;
import util.comparators.CompareReviewDirs.ComparisonType;
import util.comparators.CompareReviewDirs.DiscordanceSummary;
import util.comparators.CompareReviewDirs.Severity;
import util.reviewDir.ReviewDirectory;

/**
 * Base class for all review directory comparators including: SummaryComparator, QCMetricsComparator, VCFComparator, AnnotationComparator. It contains two review directories
 * as well as all the variables needed to collect summary information.
 * 
 * @author Kevin
 */
public abstract class ReviewDirComparator {

	//Variables for all the subclasses.
	ReviewDirectory rd1 = null;
	ReviewDirectory rd2 = null;
	
	ComparisonSummaryTable summaryTable = new ComparisonSummaryTable();
	LinkedHashMap<String, Object> summaryJSON = new LinkedHashMap<String, Object>();
	Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
	
	private DiscordanceSummary discordanceSummary = new DiscordanceSummary();
	
	public DiscordanceSummary getDiscordanceSummary() {
		return this.discordanceSummary;
	}
	
	public ReviewDirComparator() {
	}
	
	public ReviewDirComparator(ReviewDirectory rd1, ReviewDirectory rd2, String analysisHeader) {
		this.rd1 = rd1;
		this.rd2 = rd2;
		//this.summaryTable.setColumnNames("", "", "Notes");
		this.summaryTable.setCompareType(analysisHeader);
	}
	
	/** Function which collects specific comparisons between the two review directories. 
	 * @param rowName - Descriptive title of whats being compared.
	 * @param c1Entry
	 * @param c2Entry
	 * @param c3Entry - Note (may or may not be empty) describing the discordance.
	 */
	protected void addNewEntry(String jsonKey, String rowName, String c1Entry, String c2Entry , ComparisonType compareType) {
		String notes = "";
		notes = this.generateComparionsNotes(jsonKey, c1Entry, c2Entry, compareType);
		List<String> newRow = Arrays.asList(rowName, c1Entry, c2Entry, notes);
		this.summaryTable.addRow(newRow);
		
/*		
		if (!c3Entry.equals("")) {
			String[] jsonString = {c1Entry,c2Entry,c3Entry};
			this.summaryJSON.put(jsonKey, jsonString);
		}*/
		String[] jsonString = {c1Entry, c2Entry, notes};
		this.summaryJSON.put(jsonKey, jsonString);
	}
	
	protected void addNewSummaryEntry(String jsonKey, String rowName, String c1Entry, String c3Entry) {
		List<String> newRow = Arrays.asList(rowName, c1Entry, "", c3Entry); //give blank column for aesthetic purposes.
		this.summaryTable.addRow(newRow);
		
		String[] jsonString = {c1Entry,c3Entry};
		this.summaryJSON.put(jsonKey, jsonString);
	}
	
	
	protected void addNewAnnotationSummaryEntry(String jsonKey, String rowName, String dropped, String gained, String changed, String totalComparisons, ComparisonType compareType) {
		String notes = "";
		//notes = this.generateComparionsNotes(jsonKey, c1Entry, totalComparisons, compareType);
		notes = "";
		List<String> newRow = Arrays.asList(rowName, dropped, gained, changed, notes); //give blank column for aesthetic purposes.
		this.summaryTable.addRow(newRow);
		
		String[] jsonString = {dropped, gained, changed, notes};
		this.summaryJSON.put(jsonKey, jsonString);
	}
	
	public LinkedHashMap<String, Object> getJSONOutput() {		
		return this.summaryJSON;
	}
	
/*	public Map<Severity, List<String>> getSeveritySummary() {
		return this.severitySummary;
	}*/
	
	/** Function which runs the core operations for each comparison.
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	protected void performOperation() throws IOException, JSONException {
		performComparison();
		this.summaryTable.printTable();
	}
	
	protected String handleComparison(Double n1, Double n2, boolean calcDiff, String compareKey) {

		return "";
	}
	
	/** Given two numbers (passed as Strings) this function will create a string summarizing the difference between the two numbers.
	 *  Such as: Difference of 0.4 (0.6%)
	 * @param n1
	 * @param n2
	 * @return
	 */
	protected String generateComparionsNotes(String compareKey, String s1, String s2, ComparisonType compareType) { //, EnumMap cutOffs) {
		try{
			StringBuilder note = new StringBuilder();
			Double diff = 0.0;
			Double n1 = 0.0;
			Double n2 = 0.0;
			boolean calcSeverity = true;
			Severity severity = null;
			
			NumberFormat defaultFormat = NumberFormat.getPercentInstance();
			defaultFormat.setMinimumFractionDigits(1);
			Double difPercent = null;
			
			//Handle the correct comparison type here. The first few actually return strings in the switch statement whereas the last ones for the numbers 
			//continue on to complete the comparison.. Maybe that is not the best design approach.
			switch (compareType) {
				case NONE:
					return "";
				case TEXT:
					if (s1.equals(s2)) {
						return "";
					} else {
						severity = Severity.MAJOR;
						discordanceSummary.addNewDiscordance(severity, compareKey);
						return "["+severity.toString()+"]";
					}
				case TIME:
					SimpleDateFormat sdfRunTime = new SimpleDateFormat("HH:mm:ss");
					
					Date run1Time;
					Date run2Time;
					try {
						run1Time = sdfRunTime.parse(s1);
						run2Time = sdfRunTime.parse(s2); // Set end date
						long duration  = run2Time.getTime() - run1Time.getTime();

						//long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
						long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
						//long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);
						
						String runTimeNotes = "Test run took " + diffInMinutes + " minutes longer.";
						return runTimeNotes;
					} catch (ParseException e) {
						e.printStackTrace();
					}
				case EXACTNUMBER:
					calcSeverity = false;
					n1 = Double.valueOf(s1);
					n2 = Double.valueOf(s2);
					diff = Math.abs(n1 - n2);
					if (diff > 0) {
						severity = Severity.MAJOR;
					} else {
						severity = Severity.EXACT;
					}
				case TWONUMBERS:
					n1 = Double.valueOf(s1);
					n2 = Double.valueOf(s2);
					diff = Math.abs(n1 - n2);
					
					if (diff == 0.0 || n2 == 0.0 || n1 == 0.0) {
						difPercent = 0.0;
					} else {
						difPercent = diff/n2;
					}
					
					severity = calculateSeverity(difPercent, compareKey);
					break;
				case ONENUMBER: //Used for annotations, where we are given number discordant and total comparisons.
					n1 = Double.valueOf(s1);
					n2 = Double.valueOf(s2);
					diff = Double.valueOf(n1/n2);
					
					if (diff == 0.0 || n2 == 0.0 || n1 == 0.0) {
						difPercent = 0.0;
					} else {
						difPercent = diff/n2;
					}
					
					severity = calculateSeverity(difPercent, compareKey);
					break;
				default:
					break;
			}

			note.append("["+severity.toString()+"]");
			if (severity != Severity.EXACT) {
				note.append(" | ");
				note.append(String.format("%.1f", diff));
				note.append(" | ");
				note.append(defaultFormat.format(difPercent));
			}
			
			return note.toString();			
		} catch(Exception e) {
			return "";
		}
	}
	
	private Severity calculateSeverity(Double difPercent, String compareKey) {
		Severity severity = null;
		if (difPercent >= 0.2) {
			severity = Severity.MAJOR;
			discordanceSummary.addNewDiscordance(severity, compareKey);
		} else if (0.2 > difPercent && difPercent >= 0.1) {
			severity = Severity.MODERATE;
			discordanceSummary.addNewDiscordance(severity, compareKey);
		} else if (0.1 > difPercent && difPercent > 0.0) {
			severity = Severity.MINOR;
			discordanceSummary.addNewDiscordance(severity, compareKey);
		} else if (difPercent == 0.0 ){
			severity = Severity.EXACT;
			discordanceSummary.addNewDiscordance(severity, compareKey);
			//return ""; //Lets just leave the notes blank if its equal
		}
		return severity;
	}
	
	
	/** This function gets overridden by each of the sub-classes and is where the specific comparators perform their specific comparisons.
	 * 
	 * @throws IOException
	 * @throws JSONException
	 */
	abstract void performComparison() throws IOException, JSONException;

}