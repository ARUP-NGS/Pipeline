package plugins.example;

import java.util.ArrayList;
import java.util.List;

import pipeline.PipelineObject;
import plugin.Plugin;

/**
 * An example plugin. Plugins are little more than just a collection of classes of PipelineObjects
 * They provide a list of the classes they know about, but don't actually instantiate any classes
 * @author brendan
 *
 */
public class ExamplePlugin implements Plugin {

	/**
	 * Returns a list of the clases provided by this plugin. Right now they must all extend PipelineObject
	 */
	@Override
	public List<Class<? extends PipelineObject>> getClassesProvided() {
		List< Class<? extends PipelineObject>> classes = new ArrayList< Class<? extends PipelineObject>>();
		
		classes.add( SomeNewAnnotator.class );
		
		return classes;
	}

}
