package operator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import json.JSONException;
import operator.hook.IOperatorEndHook;
import operator.hook.IOperatorStartHook;
import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * This is the base class of all things that perform an "operation" on some data. There's not a lot
 * of functionality here - we just store some properties (which typically come in as attributes
 * from the input xml file), and define an abstract method 'performOperation' which gets called
 * when this operator is to do its job. 
 * 
 * Most important subclass is IOOperator, which is a type of operator that takes data from an
 * input file and (usually) writes it to an output file. 
 * 
 * @author brendan
 *
 */
public abstract class Operator extends PipelineObject {

	enum State {Initialized, Started, Completed, Error};
	
	protected Map<String, String> properties = new HashMap<String, String>();
	protected List<IOperatorStartHook> startHooks = new ArrayList<IOperatorStartHook>();
	protected List<IOperatorEndHook> endHooks = new ArrayList<IOperatorEndHook>();
	
	protected State state = State.Initialized;
	protected boolean verbose = true;
	
	@Override
	public void setAttribute(String key, String value) {
		properties.put(key, value);
		//Logger.getLogger(Pipeline.primaryLoggerName).info("Operator : " + this.getObjectLabel() + " adding attribute " + key + " = " + value);
	}
		
	@Override
	public String getAttribute(String key) {
		return properties.get(key);
	}
	
	public Collection<String> getAttributeKeys() {
		return properties.keySet();
	}
	
	/**
	 * Get the current State of this operator
	 * @return
	 */
	public State getState() {
		return state;
	}
	
	public void operate() throws OperationFailedException, JSONException, IOException {
		state = State.Started;

		// Perform the start hooks
		Iterator<IOperatorStartHook> its = startHooks.iterator();
		try{
			while(its.hasNext()){
				its.next().doHook();
			}
		} catch (Exception e) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Operator start hooks failed to complete: " + e.getMessage());
		}
		
		// Perform the Operation
		try {
			performOperation();
		}
		catch (OperationFailedException oex) {
			state = State.Error;
			throw oex;
		}
		
		
		// Perform the end hooks
		Iterator<IOperatorEndHook> ite = endHooks.iterator();
		try{
			while(ite.hasNext()){
				ite.next().doHook();
			}
		}catch(Exception e){
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Operator end hooks failed to complete: " + e.getMessage());
		}
		
		state = State.Completed;
	}
	
	/**
	 * Add a start hook to this Operator
	 * @param start
	 */
	public void addStartHook(IOperatorStartHook start){
		startHooks.add(start);
	}
	
	/**
	 * Add an end hook to this operator
	 * @param end
	 */
	public void addEndHook(IOperatorEndHook end){
		endHooks.add(end);
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
	
	protected void executeCommandCaptureBinary(final String command, File outputFile) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		final Process p;

		try {
			FileOutputStream outputStream = new FileOutputStream(outputFile);
			
			p = r.exec(command);
			
			//Weirdly, processes that emits tons of data to their error stream can cause some kind of 
			//system hang if the data isn't read. Since BWA and samtools both have the potential to do this
			//we by default capture the error stream here and write it to System.err to avoid hangs. s
			final Thread errConsumer = new BinaryPipeHandler(p.getErrorStream(), System.err);
			errConsumer.start();
			
			final Thread outputConsumer = new BinaryPipeHandler(p.getInputStream(), outputStream);
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
	
	protected String executeCommandOutputToString(final String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		final Process p;
		System.out.println("About to execute " + command);
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
	
	public abstract void performOperation() throws OperationFailedException, JSONException, IOException;
}
