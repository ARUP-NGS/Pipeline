package util.comparators;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import json.JSONException;
import pipeline.Pipeline;
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
	protected void addNewEntry(String jsonKey, String rowName, String c1Entry, String c2Entry , String c3Entry) {
		List<String> newRow = Arrays.asList(rowName, c1Entry, c2Entry, c3Entry);
		this.summaryTable.addRow(newRow);
		
/*		
		if (!c3Entry.equals("")) {
			String[] jsonString = {c1Entry,c2Entry,c3Entry};
			this.summaryJSON.put(jsonKey, jsonString);
		}*/
		String[] jsonString = {c1Entry,c2Entry,c3Entry};
		this.summaryJSON.put(jsonKey, jsonString);
	}
	
	protected void addNewSummaryEntry(String jsonKey, String rowName, String c1Entry, String c3Entry) {
		List<String> newRow = Arrays.asList(rowName, c1Entry, "", c3Entry); //give blank column for aesthetic purposes.
		this.summaryTable.addRow(newRow);
		
		String[] jsonString = {c1Entry,c3Entry};
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
	protected String compareNumberNotes(Double n1, Double n2, boolean calcDiff, String compareKey, boolean anyDiscordanceIsMajor) { //, EnumMap cutOffs) {
		try{
			StringBuilder note = new StringBuilder();
			//Double num1 = Double.parseDouble(n1);
			//Double num2 = Double.parseDouble(n2);
			//Lets check if one number is missing and if so set a major difference.

			Double diff = 0.0;
			if (calcDiff) {
				diff = Math.abs(n1 - n2);
			} else {
				diff = n1;
			}
			NumberFormat defaultFormat = NumberFormat.getPercentInstance();
			defaultFormat.setMinimumFractionDigits(1);
			Double difPercent = null;
			if (diff == 0.0 || n2 == 0.0) {
				difPercent = 0.0;
			} else {
				difPercent = diff/n2;
			}
			Severity severity = null;
			if (difPercent >= 0.2 || (anyDiscordanceIsMajor && difPercent > 0.0)) {
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
				return ""; //Lets just leave the notes blank if its equal
			}
			
			note.append("["+severity.toString()+"]");
			note.append(" | ");
			note.append(String.format("%.1f", diff));
			note.append(" | ");
			note.append(defaultFormat.format(difPercent));
			
			return note.toString();			
		} catch(Exception e) {
			return "";
		}
	}
	
	/** This function gets overridden by each of the sub-classes and is where the specific comparators perform their specific comparisons.
	 * 
	 * @throws IOException
	 * @throws JSONException
	 */
	abstract void performComparison() throws IOException, JSONException;

}