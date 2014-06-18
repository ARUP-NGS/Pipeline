package operator.bwa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import buffer.FileBuffer;
import operator.IOOperator;
import operator.OperationFailedException;
import operator.StringPipeHandler;
import pipeline.PipelineXMLConstants;

public class BWASamse extends IOOperator {

	public static final String READ_GROUP = "readgroup";
	public static final String PATH = "path";
	protected String pathToBWA = "bwa";
	
	protected String defaultRG = "@RG\\tID:unknown\\tSM:unknown\\tPL:ILLUMINA";
	protected String readGroupStr = defaultRG;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.BWA_PATH);
		if (propsPath != null)
			pathToBWA = propsPath.toString();

		File outputFile = this.getOutputBufferForClass(FileBuffer.class).getFile();
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
			
		String rgStr = properties.get(READ_GROUP);
		if (rgStr != null) {
			readGroupStr = rgStr;
		}
		String referencePath = inputBuffers.get(0).getAbsolutePath();
		String readsSAI = inputBuffers.get(1).getAbsolutePath();
		String readsPath = inputBuffers.get(2).getAbsolutePath();

		String command = pathToBWA + " sampe -r " + readGroupStr + " " + referencePath + " " + readsSAI + " " + readsPath;
		executeCommandCaptureOutput(command, outputFile);
		return;		
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
