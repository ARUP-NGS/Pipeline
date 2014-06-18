package operator.bwa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import operator.IOOperator;
import operator.OperationFailedException;
import operator.StringPipeHandler;
import pipeline.PipelineXMLConstants;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.SAIFile;

/*
 * @author daniel
 * Adds control of output SAI file to the run xml.
 */

public class BWAalnD extends IOOperator {
	
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	protected String pathToBWA = "bwa";
	protected int defaultThreads = 4;
	protected int threads = defaultThreads;

	@Override
	public boolean requiresReference() {
		return true;
	}
	@Override
	public void performOperation() throws OperationFailedException {
	
		Object propsPath = getPipelineProperty(PipelineXMLConstants.BWA_PATH);
		if (propsPath != null)
			pathToBWA = propsPath.toString();
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
		
		String threadsAttr = properties.get(THREADS);
		if (threadsAttr != null) {
			threads = Integer.parseInt(threadsAttr);
		}
		
		FileBuffer saiAttr = this.getOutputBufferForClass(SAIFile.class);
		File saiPath = saiAttr.getFile();
		FileBuffer reference = this.getInputBufferForClass(ReferenceFile.class);
		String referencePath = reference.getAbsolutePath();
		FileBuffer reads = this.getInputBufferForClass(FastQFile.class);
		String readsPath = reads.getAbsolutePath();
		String command_str = pathToBWA + " aln -t " + threads + " " + referencePath + " " + readsPath;
		executeCommandCaptureOutput(command_str, saiPath);

	}
	protected void executeCommandCaptureOutput(final String command, File outputFile) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		final Process p;

		try {
			FileOutputStream outputStream = new FileOutputStream(outputFile);
			
			p = r.exec(command);
			
			//Weirdly, processes that emits tons of data to their error stream can cause some kind of 
			//system hang if the data isn't read. Since BWA and samtools both have the potential to do this
			//we by default capture the error stream here and write it to System.err to avoid hangs. s
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

			outputStream.close();
		}
		catch (IOException e1) {
			throw new OperationFailedException("Task encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
	}


}
