package concurrent;

import java.io.File;

import junit.framework.Assert;
import operator.OperationFailedException;
import operator.Operator;
import pipeline.Pipeline;
import pipeline.PipelineListener;

/**
 * This class tests some of the basic parallel operator things
 * @author brendan
 *
 */
public class TestParallelOperators {


	/**
	 * In progress.... not an official test yet
	 */
	public void TestParallelOperator() {
		
		File testInputFile = new File("src/test/java/concurrent/concurrentInput1.xml");
		File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
		
		Pipeline ppl = new Pipeline(testInputFile, propertiesFile.getAbsolutePath());
		
		CounterListener listener = new CounterListener();
		
		try {
			ppl.addListener( listener );
			ppl.initializePipeline();
			ppl.execute();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
		Assert.assertTrue(listener.completed == 6);
		Assert.assertTrue(listener.started == 6);
		Assert.assertTrue(listener.finished);
	}
	
	
	public class CounterListener implements PipelineListener {

		public int completed = 0;
		public int started = 0;
		public boolean finished = false;
		
		@Override
		public void operatorCompleted(Operator op) {
			completed++;
		}

		@Override
		public void operatorBeginning(Operator op) {
			started++;
		}

		@Override
		public void errorEncountered(OperationFailedException opEx) {
			System.err.println("Operator " + opEx.getSourceOperator().getObjectLabel() + " had an error: "  + opEx.getLocalizedMessage());
			throw new IllegalStateException("This shouldn't happen");
		}

		@Override
		public void pipelineFinished() {
			finished = true;
		}

		@Override
		public void message(String messageText) {
			//nothing to do
		}
		
	}
}
