package operator.oncology;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;
import operator.StringPipeHandler;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/*
 * @author daniel
 * Counts the number of records for fastq or sam files. 
 * Contains countLines from StackOverflow question 453018
 * 
*/
		/* TODO Metacode:
		 * 1. Count fastq records for input file
		 * 2. Count sam records for all 4 bam files
		 * 3. Get list of "chromosomes"
		 * 4. Calculate ratios as needed
		 * 5. Write results to JSON
		 */
public class OncologyUtils extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning utilities");
		
		return;
	}

	
	public static int countLines(String filename) throws IOException {
	    InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    try {
	        byte[] c = new byte[1024];
	        int count = 0;
	        int readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++count;
	                }
	            }
	        }
	        return (count == 0 && !empty) ? 1 : count;
	    } finally {
	        is.close();
	    }
	}
	protected String executeCommandOutputToString(final String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		final Process p;

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			p = r.exec(command);
			
			final Thread errConsumer = new StringPipeHandler(p.getErrorStream(), System.err);
			errConsumer.start();
			
			final Thread outputConsumer = new StringPipeHandler(p.getInputStream(), outputStream);
			outputConsumer.start();
			
			//If runtime is going down, destroy the process so it won't become orphaned
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					//System.err.println("Invoking shutdown thread, destroying task with command : " + command);
					p.destroy();
					errConsumer.interrupt();
					outputConsumer.interrupt();
				}
			});
		
			try {
				if (p.waitFor() != 0) {
					throw new OperationFailedException("Task terminated with nonzero exit value : " + System.err.toString() + " command was: " + command, this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Task was interrupted : " + System.err.toString() + "\n" + e.getLocalizedMessage(), this);
			}

			return outputStream.toString();
		}
		catch (IOException e1) {
			throw new OperationFailedException("Task encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
	}	
	
}