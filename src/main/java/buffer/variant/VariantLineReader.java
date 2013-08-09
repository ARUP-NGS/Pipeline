package buffer.variant;

import java.io.IOException;

/**
 * Interface for things that can look at a line of text and parse a 
 * variant record from it
 *
 * @author brendan
 *
 */
public interface VariantLineReader {

	/**
	 * Advance this line reader to the next variant record
	 * returns false is line cannot be read
	 */
	public boolean advanceLine() throws IOException; 
	
	/**
	 * Return the 'raw' (unaltered) line from which the current variant will be parsed
	 */
	public String getCurrentLine() throws IOException;
	
	/**
	 * Obtain the header, if present, for this variant file. Headers start at the beginning
	 * of the file and each line starts with a '#' character
	 * @return
	 * @throws IOException
	 */
	public String getHeader() throws IOException;
	
	/**
	 * Obtain a new variant record with information from a current line
	 */	
	public VariantRec toVariantRec();
	
}
