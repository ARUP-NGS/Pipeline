package operator.hook;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pipeline.PipelineObject;

/**
 * This is the base class of all hooks that are performed before (Type = Start)
 * and after (Type = End) an Operator executes. 
 * @author quin
 *
 */
public abstract class OperatorHook extends PipelineObject implements IOperatorHook {
	protected Map<String, String> properties = new HashMap<String, String>();
	protected static final String STATUS_STARTING = "Starting";
	protected static final String STATUS_STOPPING = "Stopping";
	
	
	@Override
	public void setAttribute(String key, String value) {
		properties.put(key, value);
	}

	@Override
	public String getAttribute(String key) {
		return properties.get(key);
	}

	@Override
	public Collection<String> getAttributeKeys() {
		return properties.keySet();
	}

	public abstract void doHook() throws Exception;
}
