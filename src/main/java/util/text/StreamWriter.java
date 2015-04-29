package util.text;

import java.io.IOException;
import java.io.OutputStream;

public class StreamWriter extends java.io.Writer {

	private final OutputStream os;
	
	public StreamWriter(OutputStream outputStream) {
		this.os = outputStream;
	}
	
	@Override
	public void close() throws IOException {
		os.close();
	}

	@Override
	public void flush() throws IOException {
		os.flush();
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		String s = new String(cbuf);
		os.write(s.getBytes(), off, len);
	}

}
