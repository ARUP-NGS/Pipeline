package plugin;

import java.util.List;

import pipeline.PipelineObject;

/**
 * A 'plugin' is any class that can supply a list of Class<PipelineObject> objects. Implementations
 * of this interface are meant to supply lists of Operators and Annotators that can be loaded at *runtime*
 * when Pipeline executes.   
 * @author brendan
 *
 */
public interface Plugin {

	public List< Class<? extends PipelineObject> > getClassesProvided();
	
}
