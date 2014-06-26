package pipeline;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base class for all objects that can be created from XML by an ObjectHandler. Mostly just a marker 
 * class, but there's a little functionality having to do with setting a label for each object, maintaining
 * references to the Pipeline and ObjectHandler 'owner' objects, and keeping track of whether the
 * object has been initialized.   
 * @author brendan
 *
 */
public abstract class PipelineObject {

	protected String objectLabel = null; //Label for XML element that begat this object
	private boolean initialized = false; //If initialized() has been called. 
	private ObjectHandler objectHandler = null; //Reference to ObjectHandler that manages initialization steps for this object
	
	public void setObjectHandler(ObjectHandler handler) {
		this.objectHandler = handler;
	}
	
	/**
	 * Obtain a reference to the Pipeline object that created this object.  
	 * @return
	 */
	public Pipeline getPipelineOwner() {
		return objectHandler.getPipelineOwner();
	}
	
	/**
	 * Obtain a reference to the ObjectHandler object that created this PipelineObject
	 */
	public ObjectHandler getObjectHandler() {
		return objectHandler;
	}
	
	/**
	 * Obtain the value of a 'global' property housed in the Pipeline object that
	 * owns this object. These properties are the ones defined in the properties file 
	 * that is '.pipelineprops.xml' by default, or whatever the user specifies on the command line.
	 * Don't confuse these with the attributes that are defined in the XML that defined 
	 * these objects - those are specific to individual PipelineObjects and obtained via get- and setAttribute 
	 * @param propertyKey
	 * @return
	 */
	public String getPipelineProperty(String propertyKey) {
		return (String) getPipelineOwner().getProperty(propertyKey);
	}
	
	/**
	 * Obtain from the PipelineOwner the path describing the base directory of this project.
	 * This may be null if it has not been specified, in which case typically user.dir will be used
	 * @return
	 */
	public String getProjectHome() {
		return getPipelineOwner().getProjectHome();
	}
	
	/**
	 * Returns the object from the handler whose label is equal to the given label
	 * @param label
	 * @return
	 */
	public PipelineObject getObjectFromHandler(String label) {
		return objectHandler.getObjectForLabel(label);
	}
	
	/**
	 * Set the unique label for this object
	 * @param label
	 */
	public void setObjectLabel(String label) {
		if (objectLabel != null)
			throw new IllegalArgumentException("A label has already been set for object with label : " + objectLabel);
		this.objectLabel = label;
		
	}
	
	public String getObjectLabel() {
		return objectLabel;
	}
	
	/**
	 * Returns true if initializeObject has been called on this object
	 * @return
	 */
	public boolean isInitialized() {
		return initialized;
	}
	
	public final void initializeObject(NodeList children) {
		initialize(children);
		initialized = true;
	}
	
	/**
	 * Returns the first *Element* node encountered whose label is .equal to the given key, or
	 * null if no such child element exists 
	 * @param children
	 * @return
	 */
	protected static Element getChildForLabel(String key, NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (key.equals( child.getNodeName() )) {
					return (Element)child;
				}
			}
			
		}
		
		return null;
	}
	
	/**
	 * Set the value for some property associated with the given key. 
	 * @param key
	 * @param value
	 */
	public abstract void setAttribute(String key, String value);

	/**
	 * Obtain the value for some property associated with the given key. 
	 * @param key
	 * @param value
	 */	
	public abstract String getAttribute(String key);
	
	/**
	 * Return keys for all attributes
	 * @return
	 */
	public abstract Collection<String> getAttributeKeys();
	
	/**
	 * Called after 
	 */
	public abstract void initialize(NodeList children);
	
	/**
	 * Returns true if all of the attributes in this object are the
	 * same as ( .equals() is true) those specified for the given object
	 * @param obj
	 * @return
	 */
//	public boolean attributesAreEqual(PipelineObject obj) {
//		for(String key : getAttributeKeys()) {
//			if (! (this.getAttribute(key).equals(obj.getAttribute(key))))
//				return false;
//		}
//		return true;
//	}
}
