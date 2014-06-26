package util;

import java.io.IOException;
import java.io.Writer;

/**
 * A type of writer that adds results to a StringBuilder
 * @author brendan
 *
 */
public class StringWriter extends Writer {

	private final StringBuilder strB;

	public StringWriter() {
		this(new StringBuilder());
	}
	
	public StringWriter(StringBuilder strB) {
		this.strB = strB;
	}
	
	@Override
	public void write(char[] cbuf, int offset, int len) throws IOException {
		strB.append(cbuf, offset, len);
	}
	
	public String toString() {
		return strB.toString();
	}
	
	@Override
	public void flush() throws IOException {
		// Don't need to do anything	
	}

	@Override
	public void close() throws IOException {
		// Don't do anything	
	}
	
	
}
