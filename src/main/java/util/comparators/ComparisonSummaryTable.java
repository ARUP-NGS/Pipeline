package util.comparators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import buffer.variant.VariantRec;

/** This class encapsulates all the comparison information and provides an easy framework for building nice looking output
 *  of several column tables.
 *  
 * @author kevin
 *
 */
public class ComparisonSummaryTable {

	List<List<String> > rowData = new ArrayList<List<String> >();
	List<String> colNames;
	String comparisonType = "";
	String comparisonName = "";
	Map<String, String> failedVariants = new LinkedHashMap<String, String>(); //Keep track of variants to display for a given analysis comparison.
	Map<String, String> droppedVariants = new LinkedHashMap<String, String>(); //Keep track of variants to display for a given analysis comparison.

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	
	public ComparisonSummaryTable(String comparison, List<String> colNames) {
		this.comparisonType = comparison;
		this.colNames = colNames;
		//this.colNames = Arrays.asList("","","");
	}
	
	public void setColNames(List<String> colNames) {
		this.colNames = colNames;
	}
	
	public void setCompareType(String comparison) {
		this.comparisonType = comparison;
	}
	
	public void addRow(List<String> row) {
		if (row.size() != colNames.size() + 1) {
			throw new IllegalArgumentException("Incorrect number of columns, got " + row.size() + ", but should be " + String.valueOf(colNames.size() + 1));
		} else {
			rowData.add(row);
		}
	}
	
	public void printTable() {

		System.out.println("");
		this.printInColumns(ANSI_WHITE + this.comparisonType + ANSI_RESET,colNames.get(0), colNames.get(1), ANSI_WHITE + colNames.get(2) + ANSI_RESET);
		this.printInColumns(ANSI_WHITE + "==============" + ANSI_RESET,"==============","==============", ANSI_WHITE + "==============" + ANSI_RESET);
		for(List<String> row : rowData) {
			
			String analysisTypeKey = row.get(0);
			String colorAnalysisTypeKey = analysisTypeKey;
			String notes = row.get(3);
			if ( notes.contains(CompareReviewDirs.Severity.MAJOR.toString())) {
				colorAnalysisTypeKey = ANSI_RED + analysisTypeKey + ANSI_RESET;
				notes = ANSI_RED + notes + ANSI_RESET;
			} else if ( notes.contains(CompareReviewDirs.Severity.MINOR.toString())) {
				colorAnalysisTypeKey = ANSI_YELLOW + analysisTypeKey + ANSI_RESET;
				notes = ANSI_YELLOW + notes + ANSI_RESET;
			} else if ( notes.contains(CompareReviewDirs.Severity.EXACT.toString())) {
				colorAnalysisTypeKey = ANSI_GREEN + analysisTypeKey + ANSI_RESET;
				notes = ANSI_GREEN + notes + ANSI_RESET;
			} else {
				colorAnalysisTypeKey = ANSI_WHITE + analysisTypeKey + ANSI_RESET;
				notes = ANSI_WHITE + notes + ANSI_RESET;		
			}
			
			this.printInColumns(colorAnalysisTypeKey, row.get(1), row.get(2), notes);
			//Here lets check if we should be printing some example variants.
			if (failedVariants.get(analysisTypeKey) != null && !failedVariants.get(analysisTypeKey).isEmpty()) {
				System.out.println("\t==== Discrepant ====");
				if ( analysisTypeKey.equals("Unique variants")) {
					System.out.println("\t"+VariantRec.getSimpleHeader());
				} else {
					System.out.println("\t" + "Contig" + "\t\t" + "Start" + "\t\t" + analysisTypeKey);
				}
				failedVariants.get(analysisTypeKey).split(System.getProperty("line.separator"));
				System.out.println("\t" + failedVariants.get(analysisTypeKey).trim());
				System.out.println("\t------------------------------------------------");
			}
			if (droppedVariants.get(analysisTypeKey) != null && !droppedVariants.get(analysisTypeKey).isEmpty()) {
				System.out.println("\t==== Dropped ====");
				System.out.println("\t" + "Contig" + "\t\t" + "Start" + "\t\t" + analysisTypeKey);
				droppedVariants.get(analysisTypeKey).split(System.getProperty("line.separator"));
				System.out.println("\t" + droppedVariants.get(analysisTypeKey).trim());
				System.out.println("\t------------------------------------------------");
			}
			
		}
	}
	
	public void printInColumns(String name, String f1, String f2, String f3) {
		System.out.printf("%-70.70s %-40.40s %-40.40s %-60.60s%n", name+":", f1, f2, f3);
	}
	
	public void printSummaryTable() {
		
		System.out.println("");
		this.printSummaryInColumns(this.comparisonType, colNames.get(0));
		this.printSummaryInColumns("==============","==============");
		for(List<String> row : rowData) {
			this.printSummaryInColumns(row.get(0), row.get(1));
		}
	}
	
	public void printSummaryInColumns(String name, String sevMap) {
		System.out.printf("%-100.100s %-200.200s%n", (String) name+":", sevMap);
	}
}
