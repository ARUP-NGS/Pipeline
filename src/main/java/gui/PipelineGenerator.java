package gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pipeline.ObjectHandler;
import pipeline.PipelineXMLConstants;

/**
 * A class which examines an XML document to find certain key attributes into which data can injected, 
 * and then handles those injections and can return the modified DOM document with the newly written data.
 * Useful if we want to insert filenames, for instance, into an XML template, and then run the modified
 * Document in a Pipeline
 * @author brendan
 *
 */
public class PipelineGenerator {
	
	public static final String INJECTION_START_TAG = "$${";
	public static final String PREFIX_TAG = "prefix";
	protected Document xmlDoc;
	
	Map<String, InjectableItem> injectableElements = new HashMap<String, InjectableItem>();
	
	protected String description = null;
	
	public PipelineGenerator(File xmlTemplate) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			xmlDoc = builder.parse(xmlTemplate);
			findInjectableElements(xmlDoc.getDocumentElement());
			findDescription();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public PipelineGenerator(InputStream xmlStream) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			xmlDoc = builder.parse(xmlStream);
			findInjectableElements(xmlDoc.getDocumentElement());
			findDescription();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getDescription() {
		return description;
	}
	
	/**
	 * Scan input looking for a description element
	 */
	private void findDescription() {
		Element root = xmlDoc.getDocumentElement();
		NodeList childNodes = root.getChildNodes();
		for(int i=0; i<childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				if (childNode.getNodeName().equalsIgnoreCase("Description")) {
					//Find text child
					NodeList gKids = childNode.getChildNodes();
					for(int j=0; j<gKids.getLength(); j++) {
						Node kid = gKids.item(j);
						if (kid.getNodeType() == Node.TEXT_NODE) {
							description = kid.getNodeValue();
							return;
						}
					}
				}
			}
		
		}
	}
	
	
	
	/**
	 * Recursively find elements with injectable attributes and add them to the injectableElements map
	 */
	protected void findInjectableElements(Element root) {
		NodeList childNodes = root.getChildNodes();
		for(int i=0; i<childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element)childNode;
				
				findInjectableElements(child);
				
				NamedNodeMap attrs = child.getAttributes();
				for(int j=0; j<attrs.getLength(); j++) {
					Node attr = attrs.item(j);
					if (attr.getNodeValue().startsWith(INJECTION_START_TAG)) {
						//String attrKey = attr.getNodeName();
						//System.out.println("Found element : " + child.getNodeName() + " with attribute key: " + attrKey + " and value: " + attr.getNodeValue());
						injectableElements.put(child.getNodeName(), new InjectableItem(child, attr));
					}
				}
			}
		
		}
	}
	
	/**
	 * A Collection of all the items which can be injected into this document
	 * @return
	 */
	public Collection<InjectableItem> getInjectables() {
		return injectableElements.values();
	}
	
	/**
	 * Sets the value field of the injectable attribute to be equal to the string given in item
	 * @param elementName
	 * @param item
	 */
	public void inject(String elementName, String item) {
		InjectableItem injectable = injectableElements.get(elementName);
		if (injectable == null)
			throw new IllegalArgumentException("No element with name : " + elementName + " can be found");
		
		Element el = injectable.element;
		String currentVal = el.getAttribute(injectable.injectableKey);
		int start = currentVal.indexOf(INJECTION_START_TAG);
		int end = currentVal.indexOf('}', start);
		if (start < 0)
			throw new IllegalArgumentException("No injection tag found for element " + elementName);
		StringBuffer itemRemoved = new StringBuffer(currentVal.substring(0, start) + (end < currentVal.length() ? currentVal.substring(end+1) : ""));
		System.out.println("After removing tag : " + itemRemoved);
		String newValue = itemRemoved.insert(start, item).toString();
		System.out.println("After inseting new bit: " + newValue);
		el.setAttribute(injectable.injectableKey, newValue);
	}
	
	/**
	 * Reaplces all injection tags with key (content) matching 'tagKey' with the given new value
	 * For instance, all tags that look like $${prefix} will be replaced with newValue
	 * 
	 * @param tagKey
	 * @param newValue
	 */
	public void injectMatchingTags(String tagKey, String newValue) {
		for(String label : injectableElements.keySet()) {
			InjectableItem item = injectableElements.get(label);
			if (item.injectableDesc.contains(INJECTION_START_TAG + tagKey)) {
				System.out.println("Injecting into element with label: " + label);
				inject(label, newValue);
			}
		}
	}
	
	public Document getDocument() {
		return xmlDoc;
	}
	
	
	class InjectableItem {
		Element element;
		String elementClass;
		String injectableKey;
		String injectableDesc;
		
		public InjectableItem(Element element, Node attr) {
			this.element = element;
			this.injectableKey = attr.getNodeName();
			this.injectableDesc = attr.getNodeValue();
			String classStr = element.getAttribute(PipelineXMLConstants.CLASS_ATTR);
			this.elementClass = classStr;
		}
	}
	
	public static void main(String[] args) {
		PipelineGenerator gen = new PipelineGenerator(new File("practice_xmlgen.xml"));
		Collection<InjectableItem> items = gen.getInjectables();
		Iterator<InjectableItem> it = items.iterator();

//		InjectableItem item = it.next();
//		gen.inject(item.element.getNodeName(), "Hello!");

		gen.injectMatchingTags("prefix", "newPrefix");
		Transformer t;
		try {
			t = TransformerFactory.newInstance().newTransformer();

			t.setOutputProperty(OutputKeys.METHOD, "xml");

			t.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
					"-//W3C//DTD XHTML 1.0 Transitional//EN");

			t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
					"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");

			t.setOutputProperty(OutputKeys.METHOD, "html");
			t.transform(new DOMSource(gen.getDocument()), new StreamResult(System.out));

		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
         
	}
	
}
