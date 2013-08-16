package pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import operator.Operator;
import operator.hook.OperatorHook;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is responsible for reading an xml DOM document and creating Pipeline objects based
 * on the xml. Element creation is recursively handled by a call to createElement(root of DOM), which happens
 * prior to any actual execution of the document. Execution occurs when some other object calls .performOperation()
 * on an operator object, causing the operator to perform its operation (whatever it may be).
 *  
 * @author brendan
 *
 */
public class ObjectHandler {

	protected Document doc;
	protected List<Operator> operatorList = null;
	protected List<OperatorHook> hookList = null;

	protected Map<String, PipelineObject> objectMap = new HashMap<String, PipelineObject>();

	private final boolean verbose = false;
	private Pipeline pipelineOwner = null;

	private ClassLoader classLoader = null;

	public ObjectHandler(Pipeline pipeline, Document doc) {
		this.pipelineOwner = pipeline;
		this.doc = doc;
	}

	/**
	 * Returns the object whose label .equals the given label
	 * @param label
	 * @return
	 */
	public PipelineObject getObjectForLabel(String label) {
		return objectMap.get(label);
	}

	/**
	 * Return a reference to the Pipeline object that 'owns' this ObjectHandler 
	 * @return
	 */
	public Pipeline getPipelineOwner() {
		return pipelineOwner;
	}

	/**
	 * Recursively create and initialize all elements found in the input Document. All objects that are
	 * Operators are added to the operatorList field. 
	 * 
	 * @throws ObjectCreationException
	 */
	public void readObjects() throws ObjectCreationException {

		//Instantiate top-level buffers
		Element root = doc.getDocumentElement();
		Logger.getLogger(Pipeline.primaryLoggerName).info("Reading objects for document with root element : " + root.getNodeName() );
		if (verbose) {
			System.out.println("Reading objects... root element is : " + root);
		}
		createElement(root); //Recursively creates all child elements

		//Build operator list and hook list.  
		// Hooks are added to every operator by the Pipeline (for now).
		// We assume all operators and the hooks list are at top level for now
		hookList = new ArrayList<OperatorHook>();
		operatorList = new ArrayList<Operator>();
		NodeList children = root.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);

			// We have a special case for hooks, since all hooks are listed inside the "hooks" node
			if(child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("hooks")){
				NodeList hookChildren = child.getChildNodes();
				for(int j=0; j<hookChildren.getLength(); j++){
					addObjectToList(hookChildren.item(j), OperatorHook.class, hookList);
				}
			}
			addObjectToList(child, Operator.class, operatorList);
		}
	}

	public void addObjectToList(Node n, Class<?> c, List list) throws ObjectCreationException{
		if(n.getNodeType() == Node.ELEMENT_NODE){
			String label = n.getNodeName();
			PipelineObject obj = objectMap.get(label);
			if(c.isInstance(obj)){
				Object o = c.cast(obj);
				if(list.contains(o)){
					throw new ObjectCreationException("The input file appears to contain duplicate " + c.getCanonicalName() + ", named: " + obj.getObjectLabel(), (Element)n);
				}
				Logger.getLogger(Pipeline.primaryLoggerName).info("Adding " + c.getCanonicalName() + ": " + label + " of class " + obj.getClass() + " to " + c.getSimpleName() + " list");
				list.add(o);
			}
		}
	}


	/**
	 * Get the list of Operators defined at top level in the xml file 
	 * @return
	 */
	public List<Operator> getOperatorList() {
		return operatorList;
	}

	public List<OperatorHook> getHookList(){
		return hookList;
	}

	/**
	 * Recursively creates all objects descending from the given XML element (in pre-order) 
	 * and then creates this element using the default no-arg constructor (as in class.newInstance() )
	 * @param el
	 * @return
	 * @throws ObjectCreationException
	 */
	private PipelineObject createElement(Element el) throws ObjectCreationException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		if (verbose) {
			System.out.println("Examining element : " + el.getNodeName());
		}

		//Recursively create children first
		NodeList children = el.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node childNode = children.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element)childNode;
				PipelineObject childObj = createElement(child);
				//We don't actually do anything with the element now. It might be null if the element has no class="blah" info
			}	
		}

		String classStr = getElementClass(el);
		if (classStr != null && classStr.length()>0) {
			try {
				if (verbose) {
					System.out.println("Class element is : " + classStr + " ... attempting creation");
				}

				//We're here because a class string has been listed as an argument to this element, meaning that it maps
				//to an object we should create. If there's already an object with the same label but a different
				//class in the objectMap, we should throw an error
				String label = el.getNodeName();
				PipelineObject preObj = objectMap.get(label);
				if (preObj != null) {
					if (! preObj.getClass().getCanonicalName().equals(classStr)) {
						throw new ObjectCreationException("Found two objects with label " + label + " but conflicting classes", el);
					}
					NamedNodeMap attrs = el.getAttributes();
					if (attrs.getLength() != 0)
						throw new ObjectCreationException("Found two objects with label " + label + ", but both have attributes specified. Attributes should only be specified on the first reference to the object", el);
				}

				Class<?> clz = loadClass(classStr);
				if (verbose) {
					System.out.println("Class " + clz + " loaded successfully!");
				}

				Object instance = clz.newInstance();
				if (verbose) {
					System.out.println("Successfully created object of " + instance.getClass() );
				}


				PipelineObject obj = (PipelineObject) instance;
				obj.setObjectLabel(el.getNodeName());
				obj.setObjectHandler(this);

				//Set all attributes found in XML to be attributes of the PipelineObject created
				NamedNodeMap attrs = el.getAttributes();
				for(int i=0; i<attrs.getLength(); i++) {
					obj.setAttribute(attrs.item(i).getNodeName(), attrs.item(i).getNodeValue());
				}

				if (pipelineOwner.getProjectHome() != null)
					obj.setAttribute(Pipeline.PROJECT_HOME, pipelineOwner.getProjectHome());

				if (verbose) {
					System.out.println("Creating object with label : " + obj.getObjectLabel() + " and class " + obj.getClass());
				}

				obj.initialize(children);

				objectMap.put(obj.getObjectLabel(), obj);

				return obj;

			} catch (ClassNotFoundException e) {
				logger.severe("Critical object creation error : " + e.getClass() + "\n " + e.toString());
				e.printStackTrace();
				throw new ObjectCreationException("Object creation error: Class " + classStr + " not found \n" + e.getCause() + " : " + e.getLocalizedMessage(), el);
			} catch (InstantiationException e) {
				logger.severe("Critical object creation error : " + e.getClass() + "\n " + e.toString());
				e.printStackTrace();
				throw new ObjectCreationException("Instantiation exception, \n " + e.getCause() + " : " + e.getLocalizedMessage(), el);
			} catch (IllegalAccessException e) {
				logger.severe("Critical object creation error : " + e.getClass() + "\n " + e.toString());
				e.printStackTrace();
				throw new ObjectCreationException("Illegal access exception, \n " + e.getCause() + " : " + e.getLocalizedMessage(), el);
			} catch (Exception e) {
				logger.severe("Critical object creation error : " + e.getClass() + "\n " + e.toString());
				e.printStackTrace();
				throw new ObjectCreationException(e.getCause() + " : " + e.getLocalizedMessage(), el);
			}
		}
		return null; //We'll make it here if there's no object to create, so just return null
	}

	private Class<?> loadClass(String classStr) throws ClassNotFoundException {
		//TODO We'd like to be able to search other paths, not just already loaded classes
		if (classLoader == null)
			classLoader = ClassLoader.getSystemClassLoader();
		Class<?> clazz = classLoader.loadClass(classStr);
		return clazz;
	}

	public void setClassLoader(ClassLoader loader) {
		this.classLoader = loader;
	}

	/**
	 * Returns the value of the attribute associated with the key CLASS_ATTR, or 
	 * an *EMPTY STRING* (not null) if no such attribute exists 
	 * @param el
	 * @return
	 */
	private String getElementClass(Element el) {
		String val = el.getAttribute(PipelineXMLConstants.CLASS_ATTR);
		return val;
	}



}