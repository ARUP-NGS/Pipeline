package alnGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * A class to read long, fasta-formatted sequences
 * @author brendan
 *
 */
public class FastaReader {
	protected String currentLine = null;
	protected Integer currentTrack = null;
	protected int currentPos = -1;
	protected int lineOffset = 0;
	
	BufferedReader reader;
	
	public FastaReader(File file) throws IOException {
		reader = new BufferedReader(new FileReader(file));
		currentLine = reader.readLine();
		if (! currentLine.trim().startsWith(">")) {
			throw new IOException("First line doesn't start with >");
		}
		String chrStr = currentLine.trim().replace(">chr", "");
		int startIndex = chrStr.indexOf(">");
		int endIndex = chrStr.indexOf(" ");
		Integer tr;

		if (endIndex > 0)
			tr = Integer.parseInt(chrStr.substring(startIndex+1, endIndex));
		else 
			tr = Integer.parseInt(chrStr);
		currentTrack = tr;
		advanceLine();
	}
	
	public char getBaseAt(int track, int pos) throws IOException {
		if (pos <= 0) {
			throw new IllegalArgumentException("Remember, bases are ONE-INDEXED, so the first base is base #1, not 0, so please enter a pos > " + pos);
		}
		pos--;
		if (track < currentTrack)
			throw new IllegalArgumentException("Can't go back in tracks (current track is " + currentTrack + " but requested " + track + " )");
		if (track > currentTrack)
			advanceToTrack(track);
		if (pos < currentPos) {
			throw new IllegalArgumentException("Can't go backwards, current pos is : " + currentPos + " but requested pos : " + pos);
		}
		
		advanceToPos(pos);
		return currentLine.charAt(lineOffset);
	}
	
	/**
	 * Get number of current track
	 * @return
	 */
	public int getCurrentTrack() {
		return currentTrack;
	}
	
	/**
	 * Get current position in ZERO-INDEXED coordinates
	 * @return
	 */
	public int getCurrentPos() {
		return currentPos;
	}
	
	public static int getIntegerTrack(String contig) {
		String chrStr = contig.replace("chr", "");
		int startIndex = chrStr.indexOf(">");
		int endIndex = chrStr.indexOf(" ");
		Integer tr;
		if (endIndex > 0)
			tr = Integer.parseInt(chrStr.substring(startIndex+1, endIndex));
		else
			tr = Integer.parseInt(chrStr);

		if (chrStr.contains("X"))
			tr = 23;
		else {
			if (chrStr.contains("Y"))
				tr = 24;
			else {
				if (endIndex > 0)
					tr = Integer.parseInt(chrStr.substring(startIndex+1, endIndex));
				else
					tr = Integer.parseInt(chrStr);
			}
		}
		return tr;
	}
	
	/**
	 * Returns the base we're currently pointing at
	 * @return
	 */
	public char getCurrentBase() {
		return currentLine.charAt(lineOffset);
	}
	
	private void advanceToPos(int pos) throws IOException {
		int toAdvance = pos - currentPos; //Total number of bases to advance
		int toEndOfLine = currentLine.length() - lineOffset -1; //Number of bases to end of current line, 0 means lineOffset is pointing at last character in line
		
		while(toAdvance > toEndOfLine) {
			advanceLine();
			toEndOfLine = currentLine.length() - lineOffset-1; 
			toAdvance = pos - currentPos;
		}
		
		//Presumably, the number of bases we need to advance is now less than the length of the current line
		//so we can just bump the pointer to the right spot
		lineOffset += toAdvance;
		currentPos = pos;
		
	}
	
	/**
	 * Read in one more line from the reader and put it (after trimming) in currentLine
	 * This also resets the lineOffset
	 * @throws IOException
	 */
	private int advanceLine() throws IOException {
		int advanced = currentLine.length() - lineOffset;
		currentPos += advanced;
		if (currentLine.startsWith(">")) {
			currentPos = 0;
		}
		currentLine = reader.readLine();
		if (currentLine != null)
			currentLine = currentLine.trim();
		lineOffset = 0;
		return advanced;
	}
	
	private void advanceToTrack(int track) throws IOException {
		while(currentLine != null && (! currentLine.startsWith(">"))) {
			advanceLine();
		}
		
		Integer tr = getIntegerTrack(currentLine);
		
		if (tr < track) {
			advanceLine();
			advanceToTrack(track);
		}
		
		if (tr.equals(track)) {
			currentPos=0;
			currentTrack = track;
			advanceLine();
			return;
		}
		
		if (tr > track) {
			System.err.println("Oops, somehow missed track " + track + " maybe tracks are not in order?");
			throw new IOException("Bad tracks");
		}
	}
	
	
}

