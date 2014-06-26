package util;

import java.util.ArrayList;
import java.util.List;

/**
 * Easy creater of nicely formatted tables of text
 * @author brendan
 *
 */
public class TextTable {

	List<String[]> data = new ArrayList<String[]>();
	String[] rowNames = null;
	List<String> colNames = new ArrayList<String>();
	
	public TextTable(String[] rowNames) {
		this.rowNames = rowNames;
	}
	
	public void addColumn(String header, String[] colData) {
		if (colData.length != rowNames.length) {
			throw new IllegalArgumentException("Incorrect number of rows, got " + colData.length + ", but should be " + rowNames.length);
		}
		colNames.add(header);
		data.add(colData);
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		for(String colHeader : colNames) {
			str.append("\t" + colHeader);
		}
		str.append("\n");
		for(int i=0; i<rowNames.length; i++) {
			str.append(rowNames[i]);
			for(String[] col : data) {
				str.append("\t" + col[i]);
			}
			str.append("\n");
		}
		
		return str.toString();
	}
}
