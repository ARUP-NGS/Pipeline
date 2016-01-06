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
	Map<String, List<VariantRec>> failedVariants = new LinkedHashMap<String, List<VariantRec>>(); //Keep track of variants to display for a given analysis comparison.

	
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
		this.printInColumns(this.comparisonType,colNames.get(0), colNames.get(1), colNames.get(2));
		this.printInColumns("==============","==============","==============", "==============");
		for(List<String> row : rowData) {
			String analysisTypeKey = row.get(0);
			this.printInColumns(analysisTypeKey, row.get(1), row.get(2), row.get(3));
			List<VariantRec> listOfVariants = failedVariants.get(analysisTypeKey);
			if (listOfVariants != null && listOfVariants.size() > 0) {
				System.out.println("\t===================================================================================================");
				System.out.println("\t||" + listOfVariants.get(0).getSimpleHeader() + "\t||");
			    for (VariantRec rec : listOfVariants) {
			    	System.out.println("\t||"+rec.toSimpleString() + "\t||");
			    }
				System.out.println("\t===================================================================================================");
			} else {
			    // No such key
				continue;
			}
		}
	}
	
	public void printInColumns(String name, String f1, String f2, String f3) {
		System.out.printf("%-40.40s %-40.40s %-40.40s %-60.60s%n", name+":", f1, f2, f3);
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
