package operator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
	
	public void operate() throws OperationFailedException {
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
	
	public abstract void performOperation() throws OperationFailedException;
}
