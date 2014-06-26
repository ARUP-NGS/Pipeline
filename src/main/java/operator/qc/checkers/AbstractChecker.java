package operator.qc.checkers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;

/**
 * Base class of PipelineObjects that perform a qc validity check
 * @author brendan
 *
 * @param <T>
 */
public abstract class AbstractChecker<T> extends PipelineObject implements QCItemCheck<T> {

	protected Map<String, String> attrs = new HashMap<String, String>();
	private List<PipelineObject> children = new ArrayList<PipelineObject>();
	
	@Override
	public void setAttribute(String key, String value) {
		attrs.put(key, value);
	}

	@Override
	public String getAttribute(String key) {
		return attrs.get(key);
	}

	@Override
	public Collection<String> getAttributeKeys() {
		return attrs.keySet();
	}

	protected List<PipelineObject> getChildren() {
		return children;
	}
	
	/**
	 * Return the first child object that is assignable from the given class
	 * @param cls
	 * @return
	 */
	protected PipelineObject firstChildOfClass(Class<?> cls) {
		for(PipelineObject child : children) {
			if (cls.isAssignableFrom( child.getClass() )) {
				return child;
			}
		}
		return null;
	}
	
	@Override
	public void initialize(NodeList items) {
		for(int i=0; i<items.getLength(); i++) {
			Node iChild = items.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				children.add(obj);
			}
		}
	}


}
