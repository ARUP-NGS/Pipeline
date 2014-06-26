package operator.qc;

import java.util.ArrayList;
import java.util.List;

/**
 * Clumsy class to create HTML tables as text
 * @author brendan
 *
 */
public class TableWriter {

	final int columns; //Number of columns in table, set once at creation
	List<Integer> columnWidths = new ArrayList<Integer>();
	List<String[]> cells = new ArrayList<String[]>();
	
	private String id = null;
	private String className = null;
	private String widthStr = null;
	private String borderStr = null;
	private String cellSpacing = null;
	private String cellPadding = null;
	
	public TableWriter(int cols) {
		columns = cols;
		for(int i=0; i<cols; i++) {
			columnWidths.add(-1); //-1 means unspecified
		}
	}
	
	public void setCellSpacing(String spacing) {
		cellSpacing = spacing;
	}
	
	public void setColumnWidth(int col, int width) {
		if (col < columnWidths.size()) {
			columnWidths.set(col, new Integer(width));
		}
	}

	public void setCellPadding(String padding) {
		cellPadding = padding;
	}
	
	public void setWidth(String widthStr) {
		this.widthStr = widthStr;
	}
	
	public void setBorder(String borderStr) {
		this.borderStr = borderStr;
	}
	
	public void setID(String id) {
		this.id = id;
	}
	
	public void setClassName(String className) {
		this.className = className;
	}
	
	public void addRow(List<String> rowData) {
		String[] newRow = new String[columns];
		for(int i=0; i<Math.min(rowData.size(), columns); i++) {
			newRow[i] = rowData.get(i);
		}
		
		cells.add(newRow);
	}
	
	public void addRow(String[] rowData) {
		String[] newRow = new String[columns];
		for(int i=0; i<Math.min(rowData.length, columns); i++) {
			newRow[i] = rowData[i];
		}
		
		cells.add(newRow);
	}
	
	public String getHTML() {
		StringBuilder html =new StringBuilder();
		String idStr = "";
		String classStr = "";
		String border = "";
		if (borderStr != null) {
			border = "border=\"" + borderStr + "\"";
		}
		String width = "";
		if (widthStr != null)
			width = "width=\"" + widthStr + "\"";
		
		String spacingStr = "";
		if (cellSpacing != null) {
			spacingStr = "cellspacing=\"" + cellSpacing + "\"";
		}
		
		String paddingStr = "";
		if (cellPadding != null) {
			paddingStr = "cellpadding=\"" + cellPadding + "\"";
		}
		
		if (id != null)
			idStr = "id=\"" + id + "\"";
		
		if (className != null)
			classStr = "class=\"" + className +"\"";
		
		html.append("<table " + idStr + " " + classStr + " " + border + " " + width + " " + spacingStr + " " + paddingStr + ">\n");
		
		for(Integer colWidth : columnWidths) {
			if (colWidth > 0) {
				html.append("<col style=\"width: " + colWidth + "px\" />" );
			}
			else {
				html.append("<col />" );
			}
		}
		
		for(String[] row : cells) {
			html.append("  <tr>\n");
			
			for(int i=0; i<row.length; i++) {
				html.append("    <td>" + row[i] + "</td>\n");
			}
			
			html.append("  </tr>\n");
		}
		
		html.append("</table>\n");
		return html.toString();
	}
	
	
	public static void main(String[] args) {
		TableWriter tableW = new TableWriter(2);
		List<String> row1 = new ArrayList<String>();
		row1.add("Apples");
		row1.add("Oranges");
		
		List<String> row2 = new ArrayList<String>();
		row2.add("Bananas");
		row2.add("Boutros Boutros Ghali");
		
		List<String> row3 = new ArrayList<String>();
		row3.add("Cows");
		row3.add("Ducks");
		
		tableW.addRow(row1);
		tableW.addRow(row2);
		tableW.addRow(row3);
		
		tableW.setCellPadding("10");
		System.out.println( tableW.getHTML() );
	}
}
