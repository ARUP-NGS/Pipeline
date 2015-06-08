package operator.vardb;

import java.util.ArrayList;
import java.util.List;

import pipeline.PipelineObject;
import plugin.Plugin;

public class VariantDBPlugin implements Plugin {

	public List<Class<? extends PipelineObject>> getClassesProvided() {
		List< Class<? extends PipelineObject>> classes = new ArrayList< Class<? extends PipelineObject>>();
		
		classes.add( VariantDBUploader.class );
		
		return classes;
	}
}
