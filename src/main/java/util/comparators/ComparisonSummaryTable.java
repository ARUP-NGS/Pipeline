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
			throw new IllegalArgumentException("Incorrect number of columns, got " + row.size() + ", but should be " + String.valueOf(colNames.size() + 1));
		} else {
			rowData.add(row);
		}
	}
	
	public void printSummaryTable() {
		System.out.println("");
		this.printSummaryInColumns(this.comparisonType, colNames.get(0), colNames.get(1));
		this.printSummaryInColumns("==============","===","==============");
		for(List<String> row : rowData) {
			this.printSummaryInColumns(row.get(0), row.get(1), row.get(2));
		}
	}
	
	public void printTable() {
		System.out.println("");
		this.printInColumns(this.comparisonType,colNames.get(0), colNames.get(1), colNames.get(2));
		this.printInColumns("==============","==============","==============", "==============");
		for(List<String> row : rowData) {
/*			if (!row.get(3).equals("") ) {
				this.printInColumns(row.get(0), row.get(1), row.get(2), row.get(3));
				counter += 1;
			}*/
			this.printInColumns(row.get(0), row.get(1), row.get(2), row.get(3));
		}
	}
	
	public void printInColumns(String name, String f1, String f2, String f3) {
		System.out.printf("%-40.40s %-50.50s %-50.50s %-40.40s%n", name+":", f1, f2, f3);
	}
	
	public void printSummaryInColumns(String name, String sevNum, String sevMap) {
		System.out.printf("%-30.30s %-5.5s %-200.200s%n", (String) name+":", sevNum, sevMap);
	}
}
