package pipeline;

import org.w3c.dom.Element;

/**
 * These are thrown when a particular element in the XML document cannot be created 
 * @author brendan
 *
 */
public class ObjectCreationException extends Exception {

	protected Element element;
	
	public ObjectCreationException(String message, Element element) {
		super(message);
		this.element = element;
	}
	
	/**
	 * Get the XML element that could not be instantiated
	 * @return
	 */
	public Element getOffendingElement() {
		return element;
	}
}
