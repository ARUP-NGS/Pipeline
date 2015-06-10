package operator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.text.ElapsedTimeFormatter;

/**
 * This operator wraps a bunch of other operators and runs them in parallel on separate threads. It
 * returns only when all the operators have completed. All bets are off if the operators write to the same file. 
 * @author brendan
 *
 */
public class ParallelOperator extends Operator {

	protected List<Operator> operators = new ArrayList<Operator>();
	private int completedOperators = 0;
	
	public int getPreferredThreadCount() {
		return getPipelineOwner().getThreadCount();
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPreferredThreadCount() );

		
		Date now = new Date();
		long beginMillis = System.currentTimeMillis();
		logger.info("[" + now + "] ParallelOperator is launching " + operators.size() + " simultaneous jobs");
		this.getPipelineOwner().fireMessage("ParallelOperator is launching " + operators.size() + " jobs");
		List<OpWrapper> wraps = new ArrayList<OpWrapper>();
		
		for(Operator op : operators)  {
			OpWrapper opw = new OpWrapper(op);
			wraps.add(opw);
			threadPool.submit(opw);
		}
		
		threadPool.shutdown(); //No new tasks will be submitted,
		try {
			System.out.println("Awaiting termingation of thread pool");
			threadPool.awaitTermination(7, TimeUnit.DAYS);
			System.out.println("Threadpool has terminated");
		} catch (InterruptedException e1) {
			throw new OperationFailedException("Parallel Operator " + this.getObjectLabel() + " was interrupted during parallel execution", this);
		} //Wait until all tasks have completed

		
		//Examine all wrapped operators, see if any threw an exception, if so, throw it from here
		for(OpWrapper op : wraps) {
			if (op.ex != null) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Detected failed operator in ParallelOp: " + op.op.getObjectLabel() + " threw exception " + op.ex.getLocalizedMessage());
				throw new OperationFailedException("Operator " + op.op.getObjectLabel() + " failed: " + op.ex.getLocalizedMessage(), op.op);
			}
		}
		
		now = new Date();
		long endMillis = System.currentTimeMillis();
		long elapsedMillis = endMillis - beginMillis;
		logger.info("[ " + now + "] Parallel operator: " + getObjectLabel() + " has completed. Time taken = " + elapsedMillis + " ms ( " + ElapsedTimeFormatter.getElapsedTime(beginMillis, endMillis) + " )");		

	}
	
	/**
	 * Add the given operator to the list of those operators that will be executed
	 * when performOperation is called
	 * @param op
	 */
	protected void addOperator(Operator op) {
		operators.add(op);
	}

	@Override
	public void initialize(NodeList children) {
		
		for(int i=0; i<children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(node.getNodeName());
				if (obj instanceof Operator) {
					addOperator( (Operator)obj );
				}
				else {
					throw new IllegalArgumentException("Found non-FileBuffer object in input list for Operator " + getObjectLabel());
				}
			}
		}
	}

	/**
	 * Thin wrapper for operators so we can run them in separate threads
	 * @author brendan
	 *
	 */
	class OpWrapper implements Runnable {
		
		private Operator op;
		Exception ex = null; //If an exception is thrown
		
		public OpWrapper(Operator op) {
			this.op = op;
		}
		
		@Override
		public void run() {
			
			try {
				op.performOperation();
			} catch (OperationFailedException e) {
				this.ex = e;
			} catch (Exception e2) {
				this.ex = e2;
			}
			completedOperators++;
			op.getPipelineOwner().fireMessage("Operator " + op.getObjectLabel() + " has completed (" + completedOperators + " of " + operators.size() + ")");
			
		}
		
	}
}
