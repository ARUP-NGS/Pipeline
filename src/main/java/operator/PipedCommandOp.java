package operator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Logger;

import pipeline.Pipeline;
import buffer.FileBuffer;

/**
 * An IOOperator that invokes a system call (to call an external application), and captures the data written 
 * to standard out from that application and stores it as a file. This happens slightly differently
 * for binary vs. text data, and is accomplished by using a BinaryPipeHandler or a StringPipeHandler class
 * to capture and write the output. 
 * @author brendan
 *
 */
public abstract class PipedCommandOp extends IOOperator {
	
	//protected StringOutputStream errStream = new StringOutputStream();
	
	
	
	/**
	 * Return the string containing the command to be executed
	 * @return
	 * @throws OperationFailedException 
	 */
	protected abstract String getCommand() throws OperationFailedException;
	
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String command = getCommand();
		
		runAndCaptureOutput(command, logger, outputBuffers.get(0));
			
		Date now = new Date();
		logger.info("[ " + now + "] Operator: " + getObjectLabel() + " has completed");
	}

	/**
	 * We do most of the work here because some classes (such as MultiLaneAligner) use the below code
	 * to do some output-stream capturing work 
	 * @param command
	 * @param logger
	 * @throws OperationFailedException
	 */
	protected void runAndCaptureOutput(final String command, Logger logger, FileBuffer destinationBuffer) throws OperationFailedException {
		//Default to writing to first output buffer if it exists
		OutputStream writer = null;
		String outputPath = null;
				
		boolean binaryOutput = false;
		if (destinationBuffer != null) {
			outputPath = destinationBuffer.getAbsolutePath();
			binaryOutput = destinationBuffer.isBinary();
			try {
				writer = new FileOutputStream(outputPath);
			} catch (IOException e1) {
				throw new OperationFailedException("Could not open output stream : " + e1.getCause() + " " + e1.getMessage(), this);
			}
		}
		
		Date now = new Date();
		logger.info("[ " + now + "] Operator: " + getObjectLabel() + " Executing command : " + command );
		if (writer != null);
			
			ProcessBuilder procBuilder = new ProcessBuilder(command);
			Runtime r = Runtime.getRuntime();
			final Process p;
			try {
				p = r.exec(command);
				
				//Destroy child process if runtime is shut down 
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						//System.err.println("Invoking shutdown thread, destroying task with command : " + command);
						p.destroy();
					}
				});

				//The process we just started may hang if the output buffer its writing into gets full
				//to avoid this, we start a couple of threads whose job it is to immediately 
				//read from the output (both to stdout and to stderr) and store the resulting data
				//Furthermore, binary output and text output are handled a bit differently
				Thread outputHandler = null; 
				if (writer != null) {
					if (binaryOutput) {
						logger.info(" Pipe operator " + getObjectLabel() + " is piping binary output to path : " + outputPath);
						outputHandler = new BinaryPipeHandler(p.getInputStream(), writer);
					}
					else {
						logger.info(" Pipe operator " + getObjectLabel() + " is piping text output to path : " + outputPath);
						outputHandler = new StringPipeHandler(p.getInputStream(), new PrintStream(destinationBuffer.getFile()));
					}
					

					outputHandler.start();
				}

				Thread errorHandler = new StringPipeHandler(p.getErrorStream(), System.err);
				errorHandler.start();
				
				try {
					if (p.waitFor() != 0) {
						logger.info(" Piped operator " + getObjectLabel() + " exited with nonzero status!");
						System.err.println("Piped operator " + getObjectLabel() + " exited with nonzero status!");
						throw new OperationFailedException("Operator: " + getObjectLabel() + " terminated with nonzero exit value", this);
					}
				} catch (InterruptedException e) {
					throw new OperationFailedException("Operator: " + getObjectLabel() + " was interrupted " + e.getLocalizedMessage(), this);
				}

				//Wait for output handling thread to die
				if (outputHandler != null && outputHandler.isAlive())
					outputHandler.join();
				
			}
			catch (IOException e1) {
				throw new OperationFailedException("Operator: " + getObjectLabel() + " encountered an IO exception : \n" + e1.getLocalizedMessage(), this);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	
	/**
	 * We do most of the work here because some classes (such as MultiLaneAligner) use the below code
	 * to do some output-stream capturing work 
	 * @param command
	 * @param logger
	 * @throws OperationFailedException
	 */
//	protected void runSeriesAndCaptureOutput(String command1, String command2, Logger logger, FileBuffer destinationBuffer) throws OperationFailedException {
//		//Default to writing to first output buffer if it exists
//		OutputStream writer = null;
//		String outputPath = null;
//				
//		boolean binaryOutput = false;
//		if (destinationBuffer != null) {
//			outputPath = destinationBuffer.getAbsolutePath();
//			binaryOutput = destinationBuffer.isBinary();
//			try {
//				writer = new FileOutputStream(outputPath);
//			} catch (IOException e1) {
//				throw new OperationFailedException("Could not open output stream : " + e1.getCause() + " " + e1.getMessage(), this);
//			}
//		}
//		
//		Date now = new Date();
//		logger.info("[ " + now + "] Operator: " + getObjectLabel() + " Executing command1 : " + command1 + " and command2: " + command2);
//		if (writer != null);
//			
//			
//			Runtime r = Runtime.getRuntime();
//			Process p1;
//			Process p2;
//			try {
//				
//				PrintStream destinationStream = new PrintStream(destinationBuffer.getFile());
//				
//				p1 = r.exec(command1);
//
//				p2 = r.exec(command2);
//				
//				//The process we just started may hang if the output buffer its writing into gets full
//				//to avoid this, we start a couple of threads whose job it is to immediately 
//				//read from the output (both to stdout and to stderr) and store the resulting data
//				//Furthermore, binary output and text output are handled a bit differently
//				Thread pipeConnector = null; //Reads data from first process, writes it to second process 
//				Thread outputHandler = null; //Reads data from second process, writes it to final destination
//				
//					
//				logger.info(" Pipe operator " + getObjectLabel() + " is piping text output to path : " + outputPath);
//				pipeConnector = new StringPipeHandler(p1.getInputStream(), new BufferedOutputStream(p2.getOutputStream()));	
//				pipeConnector.start();
//				
//				outputHandler = new BinaryPipeHandler(p2.getInputStream(), destinationStream);
//				outputHandler.start();
//				
////				Thread errorHandler = new StringPipeHandler(p1.getErrorStream(), errStream);
////				errorHandler.start();
//				
//				System.out.println("Waiting for process 1 to complete");
//				logger.info("Waiting for process 1 to complete...");
//				
//				if (p1.waitFor() != 0) {
//					logger.info(" Piped operator (command #1)" + getObjectLabel() + " exited with nonzero status! \n " + errStream.toString());
//					System.err.println("Piped operator (command #1)" + getObjectLabel() + " exited with nonzero status! \n " + errStream.toString());
//					throw new OperationFailedException("Operator: " + getObjectLabel() + " terminated with nonzero exit value \n" + errStream.toString(), this);
//				}
//					
//				//System.out.println("Process one has completed");
//
//				logger.info("Process one has completed, now closing pipe stream");
//				p2.getOutputStream().close();
//				
//				logger.info("Pipe stream has closed, now waiting for process 2 to complete");
//				//Wait for second process to complete
//				if (p2.waitFor() != 0) {
//					logger.info(" Piped operator (command #2) " + getObjectLabel() + " exited with nonzero status! \n " + errStream.toString());
//					System.err.println("Piped operator (command #2)" + getObjectLabel() + " exited with nonzero status! \n " + errStream.toString());
//					throw new OperationFailedException("Operator: " + getObjectLabel() + " terminated with nonzero exit value \n" + errStream.toString(), this);
//				}
//							
//				
//				logger.info("Process 2 has completed, now waiting for output handler to finish");
//				//Wait for output handling thread to die
////				if (pipeConnector != null && pipeConnector.isAlive())
////					pipeConnector.join();
//								
//				
//				//System.out.println("Pipe connector has finished");
//				//Wait for final writing process to complete
//				if (outputHandler != null && outputHandler.isAlive())
//					outputHandler.join();
//				
//				logger.info("Output handler has finished, now closing destination buffer");
//				//System.out.println("Outputhandler has finished");
//				
//				
//				destinationStream.close();
//				//System.out.println("Destination stream has closed");
//			}
//			catch (IOException e1) {
//				throw new OperationFailedException("Operator: " + getObjectLabel() + " encountered an IO exception : \n" + errStream.toString() + "\n" + e1.getLocalizedMessage(), this);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//	}
	
}
