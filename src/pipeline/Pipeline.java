package pipeline;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class Pipeline {

	protected File source;
	protected Document xmlDoc;
	
	public Pipeline(File inputFile) {
		this.source = inputFile;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			xmlDoc = builder.parse(source);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Attempt to run the pipeline defined in the source input file
	 * @throws PipelineDocException If there are errors in document structure
	 * @throws ObjectCreationException If errors arise regarding instiation of particular objects
	 */
	public void execute() throws PipelineDocException, ObjectCreationException {
		if (xmlDoc == null) {
			throw new IllegalStateException("XMLDoc not defined");
		}
		
		Element docElement = xmlDoc.getDocumentElement();
		String docRootName = docElement.getNodeName();
		if (! docRootName.equals(PipelineXMLConstants.DOCUMENT_ROOT)) {
			throw new PipelineDocException("Document root name should be " + PipelineXMLConstants.DOCUMENT_ROOT + ", but found : " + docRootName);
		}
		
		ObjectHandler handler = new ObjectHandler(xmlDoc);

		//A quick scan for errors / validity would be a good idea
		
		
		handler.readObjects();
		
		
		
		
		
	}
	
	public static void main(String[] args) {
		File input = new File("src/test/testInput.xml");
		Pipeline pipeline = new Pipeline(input);
		
		try {
			pipeline.execute();
		} catch (PipelineDocException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ObjectCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
