package operator.examples.pluginExamples;

import java.util.ArrayList;
import java.util.List;

import pipeline.PipelineObject;
import plugin.Plugin;

/**
 * This is a simple example of a Pipeline Plugin. Plugins are dynamically loaded at runtime (not compile time),
 * and just provide access to one or more PipelineObjects. Often, these PipelineObjects will be Operators
 * or Annotators, but in principle they could really be anything. The idea is that we can simply
 * drop these into a specified 'plugins' folder to add new functionality to Pipeline, without having to
 * change any 'core' pipeline code and thus avoiding the need to revalidate.  
 * @author brendan
 *
 */
public class ExamplePlugin implements Plugin {

	@Override
	public List<Class<? extends PipelineObject>> getClassesProvided() {
		
		//This is the list of Classes we'll return.
		//Note that this doesn't actually create any new PipelineObjects - it makes the JVM aware of the existence
		//of the classes, but does nothing else. The pipeline.ObjectHandler handles actual creation of the
		//objects when necessary. 
		List< Class<? extends PipelineObject> > classes = new ArrayList< Class<? extends PipelineObject> >();
		
		//Just add some classes here. They must all extend PipelineObject
		classes.add( ExampleAnnotator.class );
		classes.add( ExampleOperator.class );
		
		return classes;
	}

}
