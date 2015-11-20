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

import json.JSONException;
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
public abstract class Comparator {

	//Variables for all the subclasses.
	ReviewDirectory rd1 = null;
	ReviewDirectory rd2 = null;
	
	Integer annotationsCompared = 0;
	
	ComparisonSummaryTable summaryTable;
	LinkedHashMap<String, Object> summaryJSON = new LinkedHashMap<String, Object>();
	DiscordanceSummary discordanceSummary = new DiscordanceSummary();
	//Logger logger = Logger.getLogger(Pipeline.primaryLoggerName); //if we want to log.
		
	public DiscordanceSummary getDiscordanceSummary() {
		return this.discordanceSummary;
	}
	
	public Comparator() {
	}
	
	public Comparator(ReviewDirectory rd1, ReviewDirectory rd2) {
		this.rd1 = rd1;
		this.rd2 = rd2;
	}
	
	/** Function which collects specific comparisons between the two review directories. 
	 * @param rowName - Descriptive title of whats being compared.
	 * @param c1Entry
	 * @param c2Entry
	 * @param c3Entry - Note (may or may not be empty) describing the discordance.
	 */
	protected void addNewEntry(String jsonKey, String rowName, String c1Entry, String c2Entry , ComparisonType compareType) {
		String notes = this.generateComparionsNotes(jsonKey, c1Entry, c2Entry, compareType);
		
		this.summaryTable.addRow(Arrays.asList(rowName, c1Entry, c2Entry, this.generateComparionsNotes(jsonKey, c1Entry, c2Entry, compareType)));
		
		String[] jsonString = {c1Entry, c2Entry, notes};
		this.summaryJSON.put(jsonKey, jsonString);
	}
	
	public LinkedHashMap<String, Object> getJSONOutput() {		
		return this.summaryJSON;
	}
	
	/** Function which runs the core operations for each comparison.
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	protected void performOperation() throws IOException, JSONException {
		performComparison();
		this.summaryTable.printTable();
	}
	
	/** Given two strings (most often numbers formated as strings) this function will create a string summarizing the difference between the two numbers.
	 *  Comparison is based on a given ComparisonType.
	 *  This funcation is an attempt to localize all comparison logic in one spot.
	 * @param compareKey
	 * @param s1
	 * @param s2
	 * @param compareType
	 * @return
	 */
	protected String generateComparionsNotes(String compareKey, String s1, String s2, ComparisonType compareType) { //, EnumMap cutOffs) {
		//Handle the correct comparison type here. The first few actually return strings in the switch statement whereas the last ones for the numbers 
		//continue on to complete the comparison.. Maybe that is not the best design approach.
		Double diff;
		Double diffPercent;
		Severity severity;

		switch (compareType) {
			case NONE:
				return "";
			case TEXT:
				if (s1.equals(s2)) {
					discordanceSummary.addNewDiscordance(Severity.EXACT, compareKey);
					return "[EXACT]";
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
				return "";
			case ANNOTATIONS:
				return "";
			case EXACTNUMBERS:
				Double int1 = Double.valueOf(s1);
				Double int2 = Double.valueOf(s2);
				diff = Math.abs(int1 - int2);
				if (diff > 0) {
					severity = Severity.MAJOR;
					discordanceSummary.addNewDiscordance(severity, compareKey);
					if (diff == 0.0) {
						diffPercent = 0.0;
					} else {
						diffPercent = diff/int2;
					}
					
					return createNote(severity, diff, diffPercent);
				} else {
					discordanceSummary.addNewDiscordance(Severity.EXACT, compareKey);
					return "[EXACT]";
				}
			case TWONUMBERS:
				Double n1 = Double.valueOf(s1);
				Double n2 = Double.valueOf(s2);
				diff = Math.abs(n1 - n2);
				if (diff == 0.0) {
					diffPercent = 0.0;
				} else {
					diffPercent = diff/n2;
				}
				if (diffPercent >= 0.2) {
					severity = Severity.MAJOR;
				} else if (0.2 > diffPercent && diffPercent >= 0.1) {
					severity = Severity.MODERATE;
				} else if (0.1 > diffPercent && diffPercent > 0.0) {
					severity = Severity.MINOR;
				} else if (diffPercent == 0.0 ){
					severity = Severity.EXACT;
					//return ""; //Lets just leave the notes blank if its equal
				} else {
					severity = Severity.MAJOR; //.UNKOWN;
				}
				discordanceSummary.addNewDiscordance(severity, compareKey);

				return createNote(severity, diff, diffPercent);
			default:
				return "";
		}
	}
	
	/** Given severity of difference, absolute value of difference, and percent difference, this will return a formated string.
	 * @param sev
	 * @param diff 			absolute value of difference
	 * @param diffPercent	Percent difference
	 * @return
	 */
	private String createNote(Severity sev, Double diff, Double diffPercent) {
		NumberFormat defaultFormat = NumberFormat.getPercentInstance();
		defaultFormat.setMinimumFractionDigits(1);
		
		StringBuilder note = new StringBuilder();
		note.append("["+sev.toString()+"]");
		if (sev != Severity.EXACT) {
			note.append(" | ");
			note.append(String.format("%.1f", diff));
			note.append(" | ");
			note.append(defaultFormat.format(diffPercent));
		}
		return note.toString();
	}
	
	/** This function gets overridden by each of the sub-classes and is where the specific comparators perform their specific comparisons.
	 * 
	 * @throws IOException
	 * @throws JSONException
	 */
	abstract void performComparison() throws IOException, JSONException;

}