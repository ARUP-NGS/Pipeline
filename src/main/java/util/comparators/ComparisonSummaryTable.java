package util.comparators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	
	public ComparisonSummaryTable() {
		this.colNames = Arrays.asList("","","");
	}
	
	public void setColNames(List<String> colNames) {
		this.colNames = colNames;
	}
	
	public void setCompareType(String comparison) {
		this.comparisonType = comparison;
	}
	
	public void addRow(List<String> row) {
		if (row.size() != colNames.size() + 1) {
			throw new IllegalArgumentException("Incorrect number of columns, got " + row.size() + ", but should be " + colNames.size());
		} else {
			rowData.add(row);
		}
	}
	
	public void printSeverityTable() {
		StringBuilder str = new StringBuilder();
		int counter = 0;
		System.out.println("");
		this.printSeverityInColumns(this.comparisonType,colNames.get(0), colNames.get(1), colNames.get(2), colNames.get(3));
		this.printSeverityInColumns("==============","==============","==============", "==============", "==============");
		//System.out.println("==============");
		for(List<String> row : rowData) {
			this.printSeverityInColumns(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4));
			counter += 1;
		}
	}
	
	public void printTable() {
		StringBuilder str = new StringBuilder();
		int counter = 0;
		System.out.println("");
		this.printInColumns(this.comparisonType,colNames.get(0), colNames.get(1), colNames.get(2));
		this.printInColumns("==============","==============","==============", "==============");
		//System.out.println("==============");
		for(List<String> row : rowData) {
			this.printInColumns(row.get(0), row.get(1), row.get(2), row.get(3));
			counter += 1;
		}
	}
	
	public void printInColumns(String name, String f1, String f2, String f3) {
		System.out.printf("%-40.40s %-50.50s %-50.50s %-40.40s%n", name+":", f1, f2, f3);
	}
	
	public void printSeverityInColumns(String name, String major, String moderate, String minor, String exact) {
		System.out.printf("%-50.50s %-20.20s %-20.20s %-20.20s %-20.20s%n", (String) name+":", major, moderate, minor, exact);
	}
}
