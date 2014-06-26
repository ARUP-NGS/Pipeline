package pipeline;

import operator.OperationFailedException;
import operator.Operator;

/**
 * Interface for things that listen for events issueing from a Pipeline. Right now
 * this just amounts to operators starting and ending, as well as the mysterious
 * 'message' event.
 * @author brendan
 *
 */
public interface PipelineListener {

	public void operatorCompleted(Operator op);
	
	public void operatorBeginning(Operator op);
	
	/**
	 * Called when an severe error occurred during pipeline execution
	 * @param op
	 */
	public void errorEncountered(OperationFailedException opEx);
	
	/**
	 * Called when the pipeline has finishing all operators
	 */
	public void pipelineFinished();
	
	public void message(String messageText);
	
	
}
