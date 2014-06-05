package util.vcfParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import buffer.variant.VariantLineReader;
import buffer.variant.VariantRec;

/**
 * A replacement for the venerable VCFLineParser, this thing is in early dev but will ideally
 * have several handy features:
 * 	1. Reads single variants at a time, even when a single vcf line has multiple variants
 * 	2. Handles multi-sample VCF input easily
 * 	3. Provide access to VCF header information
 *  4. Automatically parse information from info and format fields correctly
 *  5. Thorough testing and validation  
 * @author brendan
 *
 */
public class VCFParser implements VariantLineReader {

	protected Map<String, HeaderEntry> headerItems = null; //Stores info about FORMAT and INFO fields from header
	protected Map<String, String> headerProperties = null; //Stores generic key=value pairs from header, not FORMAT or INFO 
	
	protected File source = null;
	
	private BufferedReader reader = null;
	
	private int altIndex = 0; //Index of alt allele for the current line
	private String currentLine = null;
	private String[] currentLineToks = null; //current line split on tabs
	private int altsInCurrentLine = 1;
	
	
	public VCFParser(File source) throws IOException {
		setFile(source);

		reader = new BufferedReader(new FileReader(source));
		parseHeader();
	}
	
	/**
	 * Read the header of the file but do not parse any variants
	 * @throws IOException 
	 */
	public void parseHeader() throws IOException {
		if (source == null) {
			throw new IllegalStateException("Source file has not been set, cannot parse header");
		}
		headerItems = new HashMap<String, HeaderEntry>();
		headerProperties = new HashMap<String, String>();
		String line = reader.readLine();
		while(line != null && line.startsWith("##")) {
			if (line.startsWith("##INFO") || line.startsWith("##FORMAT")) {
				HeaderEntry entry = parseHeaderItem(line);
				headerItems.put(entry.id, entry);
			}
			else {
				String[] keyval = line.replace("#", "").split("=", 2);
				if (keyval.length==2) {
					headerProperties.put(keyval[0], keyval[1]);
				}
			}
			
			line = reader.readLine();
		}
		
		//Line should now be at the line immediately prior to the actual variants list
		if (! line.toUpperCase().startsWith("#CHR")) {
			throw new IOException("Didn't find expected next line starting with #CHROM");
		}
	}
	
	/**
	 * Return the header entry associated with the given id or null if there is no
	 * header entry with that id
	 * @param id
	 * @return
	 */
	public HeaderEntry getHeaderEntry(String id) {
		return headerItems.get(id);
	}
	
	private HeaderEntry parseHeaderItem(String line) {
		HeaderEntry entry = new HeaderEntry();
		if (line.startsWith("##INFO=")) {
			entry.entryType = EntryType.INFO;
			line = line.replace("##INFO=<", "").replace(">", "");
		}
		if (line.startsWith("##FORMAT=")) {
			entry.entryType = EntryType.FORMAT;
			line = line.replace("##FORMAT=<", "").replace(">", "");
		}
		
		String[] toks = line.split(",");
		for(int i=0; i<toks.length; i++) {
			
			if (toks[i].startsWith("ID=")) {
				entry.id = toks[i].replace("ID=", "");
			}
			if (toks[i].startsWith("Number=")) {
				entry.number = toks[i].replace("Number=", "");
			}
			if (toks[i].startsWith("Type=")) {
				entry.type = toks[i].replace("Type=", "");
			}
			if (toks[i].startsWith("Description=")) {
				entry.description = toks[i].replace("Description=\"", "").replace("\"", "");
			}
		}
			
		return entry;
	}
	
	@Override
	public void setFile(File file) throws IOException {
		headerItems = null;
		this.source = file;
	}

	/**
	 * This actually advances to the next variant to be read, which doesn't involve advancing to
	 * the next line if there are more alts to read on the current line
	 */
	@Override
	public boolean advanceLine() throws IOException {
		altIndex++;
		if (altIndex == altsInCurrentLine) {
			
			currentLine = reader.readLine();
			if (currentLine != null) {
				currentLineToks = currentLine.split("\t");
				if (currentLineToks.length < 7) {
					throw new IOException("Invalid number of tokens on line: " + currentLine);
				}
				
				altIndex = 0;
				altsInCurrentLine = currentLineToks[4].split(",").length;
			}
		}
		return currentLine != null;
	}

	@Override
	public String getCurrentLine() throws IOException {
		return currentLine;
	}

	@Override
	public String getHeader() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VariantRec toVariantRec() {
		
		String chr = currentLineToks[0].toUpperCase().replace("CHR","");
		int pos = Integer.parseInt(currentLineToks[1]);
		String ref = currentLineToks[3];
		String alt = currentLineToks[4].split(",")[altIndex];
		
		String qualStr = currentLineToks[5];
		double quality = -1;
		try {
			quality = Double.parseDouble(qualStr);
		}
		catch (NumberFormatException ex) {
			//we tolerate it if we can't parse quality...
		}
		
		VariantRec var = new VariantRec(chr, pos, pos+alt.length(), ref, alt);
		var.setQuality(quality);
		
		return var;
	}

	
	
	
	public enum EntryType {
		FORMAT, INFO
	}
	
	/**
	 * Just a container for a single entry in the vcf header (either INFO or FORMAT)
	 * @author brendan
	 *
	 */
	public class HeaderEntry {
		public EntryType entryType;
		public String id;
		public String number;
		public String description;
		public String type;
		
		public String toString() {
			return entryType + ": ID=" + id + " " + description; 
		}
	}
}
