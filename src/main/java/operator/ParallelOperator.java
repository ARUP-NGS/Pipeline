package operator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.ElapsedTimeFormatter;

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
	class OpWrapper extends SwingWorker {
		
		private Operator op;
		
		public OpWrapper(Operator op) {
			this.op = op;
		}
		
		@Override
		protected Object doInBackground() throws Exception {
			
			op.performOperation();
			completedOperators++;
			op.getPipelineOwner().fireMessage("Operator " + op.getObjectLabel() + " has completed (" + completedOperators + " of " + operators.size() + ")");
			return op;
		}
		
	}
}
