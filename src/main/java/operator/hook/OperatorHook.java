package operator.hook;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NodeList;

import pipeline.PipelineObject;

/**
 * This is the base class of all hooks that are performed before (Type = Start)
 * and after (Type = End) an Operator executes. 
 * @author quin
 *
 */
public abstract class OperatorHook extends PipelineObject implements IOperatorHook {
	
	public static final String SERVER_URL = "server.url";
	protected Map<String, String> properties = new HashMap<String, String>();
	protected static final String STATUS_STARTING = "Starting";
	protected static final String STATUS_STOPPING = "Stopping";
	
	//This gets set to something like "ngs-webapp-dev" via a pipeline are template attribute server.url 
	protected String serverURL = null;
	
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

	@Override
	public void initialize(NodeList children) {
		//Make sure there's a server URL specified
				if (serverURL == null) {
					serverURL = this.getAttribute(SERVER_URL);
				}
				if (serverURL == null) {
					serverURL = this.getPipelineProperty(SERVER_URL);
				}
				if (serverURL == null) {
						throw new IllegalArgumentException("No server url specified (use server.url attribute)");
				}
	}
	
	
	public abstract void doHook() throws Exception;
}
