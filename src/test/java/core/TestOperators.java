package core;

import java.io.File;

import junit.framework.Assert;
import operator.OperationFailedException;
import operator.Operator;

import org.junit.Test;

import pipeline.ObjectHandler;
import pipeline.Pipeline;
import pipeline.PipelineListener;
import pipeline.PipelineObject;

/**
 * This class contains some tests for the functionality of operators. 
 * @author brendan
 *
 */
public class TestOperators {
	
	/**
	 * Make sure we can parse a few simple pipeline input files and that errors are thrown
	 * when pipeline-specific violations occur
	 */
	@Test
	public void testPipelineParse() {
		
		File testInputFile = new File("src/test/java/core/inputFiles/simpleInput.xml");
		File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
		
		Pipeline ppl = new Pipeline(testInputFile, propertiesFile.getAbsolutePath());
		
		try {
			ppl.initializePipeline();
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
		File emptyInputFile = new File("src/test/java/core/inputFiles/emptyInput.xml");
		
		try {
			ppl = new Pipeline(emptyInputFile, propertiesFile.getAbsolutePath());
			
			ppl.initializePipeline();
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
		File brokenInputFile = new File("src/test/java/core/inputFiles/brokenInput.xml");
		try {
			ppl = new Pipeline(brokenInputFile, propertiesFile.getAbsolutePath());
			ppl.initializePipeline();
			Assert.assertTrue(false);
		} catch (Exception ex) {
			//this is expected - we want to see an exception thrown for this one
		}
		
		brokenInputFile = new File("src/test/java/core/inputFiles/brokenInput2.xml");
		try {
			ppl = new Pipeline(brokenInputFile, propertiesFile.getAbsolutePath());
			ppl.initializePipeline();
			Assert.assertTrue(false);
		} catch (Exception ex) {
			//this is expected - we want to see an exception thrown for this one
		}
		
		brokenInputFile = new File("src/test/java/core/inputFiles/brokenInput3.xml");
		try {
			ppl = new Pipeline(brokenInputFile, propertiesFile.getAbsolutePath());
			ppl.initializePipeline();
			Assert.assertTrue(false);
		} catch (Exception ex) {
			//this is expected - we want to see an exception thrown for this one
		}
		
			
		
	}
	
	/**
	 * Some pretty basis tests to make sure we can accurately instantiate some objects
	 * and read their attributes. 
	 */
	@Test
	public void testObjectAttributes() {
		
		File testInputFile = new File("src/test/java/core/inputFiles/testOperators1.xml");
		File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
		Pipeline ppl = new Pipeline(testInputFile, propertiesFile.getAbsolutePath());
		
		try {
			ppl.initializePipeline();
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
		ObjectHandler handler = ppl.getObjectHandler();
		PipelineObject obj1 = handler.getObjectForLabel("Test1");
		Assert.assertNotNull(obj1);
		Assert.assertEquals(obj1.getAttribute("filename"), "somefilename");
		
		PipelineObject obj2 = handler.getObjectForLabel("Test2");
		Assert.assertNotNull(obj2);
		Assert.assertEquals(obj2.getAttribute("attrA"), "blah");
		Assert.assertEquals(obj2.getAttribute("attrB"), "foo");
		
	}
	
	@Test 
	public void testParsePipelineProperties() {
		
		File testInputFile = new File("src/test/java/core/inputFiles/threeOperators.xml");
		File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
		
		Pipeline ppl = new Pipeline(testInputFile, propertiesFile.getAbsolutePath());
		Assert.assertTrue(ppl.getProperty("propKey") != null);
		Assert.assertTrue(ppl.getProperty("propKey").equals("value"));
		Assert.assertTrue(ppl.getProperty("noKeyHere") == null);
		Assert.assertTrue(ppl.getProperty("key2").equals("value2"));
		Assert.assertTrue(ppl.getProperty("key3").equals("value3"));
		
	}
	
	/**
	 * Tests to make sure operator started / finished events are being fired properly
	 */
	@Test 
	public void testEventFiring() {
		
		File testInputFile = new File("src/test/java/core/inputFiles/threeOperators.xml");
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
		
		Assert.assertTrue(listener.completed == 3);
		Assert.assertTrue(listener.started == 3);
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
