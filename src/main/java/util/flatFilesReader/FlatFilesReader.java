package util.flatFilesReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A class to seamlessly and quickly read information from a set of flat files  
 * stored in a directory, with sorted, row-ordered data stored in each file.
 * Right now this is pretty specific to dbNSFP2.0 data, 
 * @author brendan
 *
 */
public class FlatFilesReader {

	private File baseDir = null; //Base directory storing chunks files
	private BufferedReader reader = null;
	private String currentContig = null;
	private int currentPos = 0;
	private String currentLine = null;
	
	private String filenameBase = "dbNSFP2.0b4_variant.chr";
	
	
	public FlatFilesReader(File baseDir) {
		if (! baseDir.exists() || (! baseDir.isDirectory())) {
			throw new IllegalArgumentException("Bad base dir : " + baseDir.getAbsolutePath());
		}
		this.baseDir = baseDir;
	}
	
	/**
	 * Attempt to read the line for the given contig, position, and alt base. If 
	 * an exact match is found, the entire line is returned as a String. If not
	 * null is returned. 
	 * @param contig
	 * @param pos
	 * @param alt
	 * @return
	 * @throws IOException
	 */
	public String getRow(String contig, int pos, char alt) throws IOException {
		advanceToContig(contig);
		advanceToPos(pos, alt);
		return currentLine;
	}
	
	public String getRow(String contig, int pos) throws IOException {
		advanceToContig(contig);
		advanceToPos(pos);
		return currentLine;
	}
	
	/**
	 * Obtain current line as a string
	 * @return
	 */
	public String getCurrentLine() {
		return currentLine;
	}
	
	/**
	 * Advance current line to the next line, returns true if next line successfully read
	 * @return
	 */
	public boolean advanceLine() {
		try {
			currentLine = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		if (currentLine == null) {
			currentPos = -1;
			return false;
		}
		
		currentPos = getPositionFromLine(currentLine);
		return true;
	}
	
	
	public int getCurrentPos() {
		return currentPos;
	}
	
	public String getCurrentContig() {
		return currentContig;
	}
	
	/**
	 * Advance to first alt base at given position
	 * @param pos
	 * @throws IOException
	 */
	private void advanceToPos(int pos) throws IOException {
		if (currentLine == null)
			currentLine = reader.readLine();
		if (currentLine == null)
			return;
		
		currentPos = getPositionFromLine(currentLine);
		
		if (currentPos > pos) {
			System.out.println("Current pos : " + currentPos + " is greater than requested pos : " + pos + " so resetting...");
			resetToContig(currentContig);
		}
		
		while(currentPos < pos) {
			currentLine = reader.readLine();
			if (currentLine == null) {
				currentPos = -1;
				return;
			}
			currentPos = getPositionFromLine(currentLine);
		}		
	}

	/**
	 * Advance current line to the given position and alt base
	 * @param pos
	 * @param alt
	 * @throws IOException
	 */
	private void advanceToPos(int pos, char alt) throws IOException {
		if (currentLine == null)
			currentLine = reader.readLine();
		if (currentLine == null)
			return;
		
		currentPos = getPositionFromLine(currentLine);
		
		if (currentPos > pos) {
			//System.out.println("Current pos : " + currentPos + " is greater than requested pos : " + pos + " so resetting...");
			resetToContig(currentContig);
		}
		
		while(currentPos < pos) {
			currentLine = reader.readLine();
			if (currentLine == null) {
				currentPos = -1;
				return;
			}
			currentPos = getPositionFromLine(currentLine);
		}

		char currentAlt = getAltFromLine(currentLine);
		while(currentPos == pos && currentAlt != alt) {
			currentLine = reader.readLine();
			if (currentLine == null) {
				currentPos = -1;
				return;
			}	
			currentPos = getPositionFromLine(currentLine);
			currentAlt = getAltFromLine(currentLine);
		}
		
		if (currentPos != pos || currentAlt != alt) {
			currentLine = null;
		}
		
	}
	
	/**
	 * Begin reading the file associated with the given contig. This always
	 * resets the contig, even if current contig is equal to the given contig
	 * @param contig
	 * @throws IOException
	 */
	public void resetToContig(String contig) throws IOException {
		//System.out.println("Loading contig : " +contig);
		File contigFile = new File(baseDir.getAbsolutePath() + "/" + filenameBase + contig);
		if (! contigFile.exists()) {
			throw new IOException("Cannot find contig file : " + contigFile.getAbsolutePath());
		}
		
		reader = new BufferedReader(new FileReader(contigFile));
		currentLine = reader.readLine();
		while(currentLine != null && currentLine.trim().startsWith("#")) {
			currentLine = reader.readLine();
		}
		currentContig = contig;
		currentPos = getPositionFromLine(currentLine);
	}
	
	/**
	 * If contig is equal to current contig, do nothing. Otherwise begin reading
	 * the given contig
	 * @param contig
	 * @throws IOException
	 */
	private void advanceToContig(String contig) throws IOException {
		if (currentContig != null && currentContig.equals(contig)) {
			return;
		}
		
		resetToContig(contig);
		
	}
	
	private String getContigFromLine(String line) {
		return line.substring(0, 2).trim();
	}
	
	private int getPositionFromLine(String line) {
		int first = line.indexOf("\t");
		int second = line.indexOf("\t", first+1);
		if (! (first < second)) {
			throw new IllegalArgumentException("Could not find tab indices for line : "+ line);
		}
		return Integer.parseInt(line.substring(first+1, second).trim());
	}
	
	
	private char getAltFromLine(String line) {
		int first = line.indexOf("\t");
		int second = line.indexOf("\t", first+1);
		int third = line.indexOf("\t", second+1);
		
		if (! (second < third)) {
			throw new IllegalArgumentException("Could not find tab indices for line : "+ line);
		}
		return line.charAt(third+1);
	}
	
	public static void main(String[] args) {
		FlatFilesReader reader = new FlatFilesReader(new File("/home/brendan/resources/dbNSFP2.0"));
		
		try {
			String line = reader.getRow("2", 1094049, 'T');
			System.out.println(line);
			
			 line = reader.getRow("2", 1094048, 'C');
			System.out.println(line);
			
			line = reader.getRow("2", 1094049, 'C');
			System.out.println(line);
			
	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
