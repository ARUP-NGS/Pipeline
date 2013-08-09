package util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that writes onto a string buffer
 * @author brendan
 *
 */
public class StringOutputStream extends OutputStream {

	
	StringBuffer buf = new StringBuffer();
	
	@Override
	public void write(int b) throws IOException {
		buf.append( (char)b);
	}
	
	public String toString() {
		return buf.toString();
	}

}
