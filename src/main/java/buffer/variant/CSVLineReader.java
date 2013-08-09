package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a CSV file and produces VariantRecords from it, typically one variant per line
 * If file has a header (a first line that begins with "#"), then assume those are column headers
 * and parse values accordingly. 
 * @author brendan
 *
 */
public class CSVLineReader implements VariantLineReader {

	protected BufferedReader reader;
	protected String currentLine = null;
	protected String[] headerToks = null;
	//Contains a map for a column header to a column index
	private Map<String, Integer> headerMap = new HashMap<String, Integer>();
	protected final File sourceFile;
	
	
	public CSVLineReader(File csvFile) throws IOException {
		reader = new BufferedReader(new FileReader(csvFile));
		currentLine = reader.readLine();
		this.sourceFile = csvFile;
		if (currentLine.startsWith("#")) {
			String[] rawToks = currentLine.split("\t");
			List<String> tokList = new ArrayList<String>();
			for(int i=0; i<rawToks.length; i++)
				if (rawToks[i].trim().length()>0) {
					tokList.add(rawToks[i].trim());
				}
			headerToks = tokList.toArray(new String[]{});
			for(int i=0; i<tokList.size(); i++)
				headerMap.put(tokList.get(i).trim(), i);
			currentLine = reader.readLine();
		}
	}
	
	public String getHeader() throws IOException {
		BufferedReader headReader = new BufferedReader(new FileReader(sourceFile));
		String line = headReader.readLine();
		StringBuilder strB = new StringBuilder();
		while(line != null && line.trim().startsWith("#")) {
			strB.append(line + "\n");
			line = headReader.readLine();
		}
		headReader.close();
		return strB.toString();
	}
	
	/**
	 * Returns true if there is a column header that matches the given String 
	 * @param colHeader
	 * @return
	 */
	public boolean hasHeaderCol(String colHeader) {
		return headerMap.get(colHeader) != null;
	}
	
	/**
	 * Returns the column index associated with the given header 
	 * @param colHeader
	 * @return
	 */
	public int getIndexForHeader(String colHeader) {
		if (!hasHeader())
			throw new IllegalArgumentException("No header was found for this file");
		return headerMap.get(colHeader);
	}
	
	
	public String getValueForHeader(String colHeader, String[] toks) {
		if (!hasHeader())
			throw new IllegalArgumentException("No header was found for this file");
		return toks[headerMap.get(colHeader)];
	}
	
	/**
	 * Returns true if the header line that describes the columns has been specified
	 * @return
	 */
	public boolean hasHeader() {
		return headerToks != null;
	}
	
	@Override
	public boolean advanceLine() throws IOException {
		currentLine = reader.readLine();
		//Skip zero-length lines
		while (currentLine != null && currentLine.trim().length()==0)
			currentLine = reader.readLine();
		return currentLine != null;
	}

	@Override
	public VariantRec toVariantRec() {
		if (currentLine == null)
			return null;
		
		String[] toks = currentLine.split("\t");
		VariantRec rec = null;
		
		if (toks.length < 8) {
			System.err.println("ERROR: could not parse variant from file : " + sourceFile.getName() + " line : \n " + currentLine + " (at least 8 columns required)");
			return null;
		}
		
		try {
			String contig = getContig(toks);
			Integer start = getStart(toks);
			Integer end = getEnd(toks);
			String ref = getRef(toks);
			String alt = getAlt(toks);
			Double qual = getQuality(toks);
			Double depth = getDepth(toks); 
			boolean isHet = getHet(toks);
			Double genoQual = getGenotypeQuality(toks);


			if (alt.length() != ref.length()) {
				//Remove initial characters if they are equal and add one to start position
				if (alt.charAt(0) == ref.charAt(0)) {
					alt = alt.substring(1);
					ref = ref.substring(1);
					if (alt.length()==0)
						alt = "-";
					if (ref.length()==0)
						ref = "-";
					start++;
				}

				if (ref.equals("-"))
					end = start;
				else
					end = start + ref.length();
			}

			rec = new VariantRec(contig, start, start+ref.length(), ref, alt, qual, isHet);
			rec.addProperty(VariantRec.DEPTH, depth);
			rec.addProperty(VariantRec.GENOTYPE_QUALITY, genoQual);

			//Parse additional annotations / properties from header
			if (hasHeader() && toks.length > 8) {
				if (toks.length != headerToks.length) {
					for(int i=0; i<toks.length; i++) {
						System.out.println(i + "\t" + headerToks[i] + " : " + toks[i]);
					}
					throw new IllegalArgumentException("Incorrect number of columns for variant, header shows " + headerToks.length + ", but this variant has: " + toks.length + "\n" + currentLine);

				}
				for(int i=9; i<toks.length; i++) {
					String key = headerToks[i].trim();
					//System.out.println("Adding annotation for key: " + key + " value:" + toks[i]);
					if (toks[i].equals("-") || toks[i].equals("NA") || toks[i].equals("?"))
						continue; 

					try {
						Double val = Double.parseDouble(toks[i]);
						rec.addProperty(key, val);
					}
					catch (NumberFormatException ex) {
						//this is expected, we just assume it's an annotation, not a property
					}

					rec.addAnnotation(key, toks[i].trim());
				}
			}

			if (sourceFile != null)
				rec.addAnnotation(VariantRec.SOURCE, sourceFile.getName());

		}
		catch (NumberFormatException nfe) {
			System.err.println("ERROR: could not parse variant from file : " + sourceFile.getName() + " line : \n " + currentLine);

		}
		return rec;
	}

	protected String getContig(String[] toks) {
		return toks[0].replace("chr", "");
	}
	
	protected Integer getStart(String[] toks) {
		return Integer.parseInt(toks[1].trim());
	}
	
	protected Integer getEnd(String[] toks) {
		return Integer.parseInt(toks[2].trim());
	}
	
	protected String getRef(String[] toks) {
		return toks[3];
	}
	
	protected String getAlt(String[] toks) {
		return toks[4];
	}
	
	protected Double getQuality(String[] toks) {
		if (toks.length < 6)
			return 0.0;
		String trimmed = toks[5].trim();
		if (trimmed.length()>0)
			return Double.parseDouble(trimmed);
		else
			return 0.0;
	}
	
	protected Double getDepth(String[] toks) {
		if (toks.length < 7)
			return 0.0;
		String trimmed = toks[6].trim();
		if (trimmed.length()>0)
			if (trimmed.equals("-"))
				return 0.0;
			else
				return Double.parseDouble(trimmed);
		else
			return 0.0;
	}
	
	protected Boolean getHet(String[] toks) {
		return toks[7].contains("het");
	}
	
	protected Double getGenotypeQuality(String[] toks) {
		if (toks.length < 9)
			return 0.0;
		String trimmed = toks[8].trim();
		if (trimmed.length()>0)
			if (trimmed.equals("-"))
				return 0.0;
			else
				return Double.parseDouble(trimmed);
		else
			return 0.0;
	}

	@Override
	public String getCurrentLine() throws IOException {
		return currentLine;
	}
	
}
