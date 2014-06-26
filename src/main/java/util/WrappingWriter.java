package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;

public class WrappingWriter extends PrintWriter {

	public static final int wrapWidth = 80;
	private int pos = 0;
	
	public WrappingWriter(File file) throws FileNotFoundException {
		super(file);
	}
	
	public WrappingWriter(PrintStream stream) {
		super(stream);
	}
	
	public void write(int c) {
		pos++;
		if (pos >= wrapWidth) {
			super.write("\n");
			pos = 0;
		}
		super.write(c);
		
	}
	
	public void write(char c) {
		pos++;
		if (pos >= wrapWidth) {
			super.write("\n");
			pos = 0;
		}
		if (c == '\n')
			pos = 0;
		super.write(c);
		
	}
	
	public void write(String str) {
		for(int i=0; i<str.length(); i++)
			this.write(str.charAt(i));
	}
}
