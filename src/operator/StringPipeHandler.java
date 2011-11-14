package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This class uses a thread to read text data from an input stream and write it to an output stream
 * Its used in Pipeline to read the data emitted to stdout (or stderr) by a process and write it
 * to a file. Without this running as a separate thread, buffers used to store data from stdout will
 * fill up and the process generating the data may hang. 
 *  For binary data, use a BinaryPipeHandler
 * @author brendan
 *
 */
public class StringPipeHandler extends Thread {

		InputStream inpStr;
		PrintStream stream;
		
		public StringPipeHandler(InputStream inpStr, OutputStream stream) {
			this.inpStr = inpStr;
			this.stream = new PrintStream(stream);
		}
		
		public StringPipeHandler(InputStream inpStr, PrintStream stream) {
			this.inpStr = inpStr;
			this.stream = stream;
		}

		public void run() {
			try {
				InputStreamReader inpStrd = new InputStreamReader(inpStr);
				BufferedReader buffRd = new BufferedReader(inpStrd);
				String line = null;
				while((line = buffRd.readLine()) != null) {
					if (stream != null)
						stream.println(line);
				}
				buffRd.close();

			} catch(Exception e) {
				System.out.println(e);
			}

		}
}
