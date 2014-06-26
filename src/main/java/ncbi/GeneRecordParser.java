package ncbi;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Takes an input document of the XML format obtained by NCBI eutils and 
 * converts the information into a gene record.
 * @author brendan
 *
 */
public class GeneRecordParser {

	public static final String EntrezGeneSet = "Entrezgene-Set"; //Delineates element containing set of entrez-genes
	public static final String EntrezGene = "Entrezgene"; //Delineates gene-info containing element in document
	public static final String GeneTrack = "Gene-track";
	public static final String EntrezGeneGene = "Entrezgene_gene";
	public static final String GeneTrackInfo = "Entrezgene_track-info"; 
	public static final String GeneID = "Gene-track_geneid";
	public static final String GeneRef = "Gene-ref";
	public static final String GeneRefLocus = "Gene-ref_locus";
	public static final String GeneRefDesc = "Gene-ref_desc";
	public static final String GeneSummary = "Entrezgene_summary";
	public static final String GeneCommentary = "Gene-commentary";
	public static final String GeneProperties = "Entrezgene_properties";
	public static final String GeneCommentaryHeading = "Gene-commentary_heading";
	public static final String GeneCommentaryComment = "Gene-commentary_comment";
	public static final String GeneCommentaryLabel = "Gene-commentary_label";
		
	/**
	 * Parse the given DOM document and obtain a gene record for the information therein. 
	 * Currently, we assume the document is in 'EntrezGene' format, the type produced
	 * by the ncbi e-utils tools.  
	 * @param doc
	 * @return
	 */
	public static GeneRecord parse(Document doc) {
		Element root = doc.getDocumentElement();
		NodeList children = root.getChildNodes();
		
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName() != null && child.getNodeName().equals(EntrezGene)) {
				GeneRecord rec = parseGeneFromElement( (Element)child );
				return rec;
			}
		}
		
		return null;
	}

	private static GeneRecord parseGeneFromElement(Element egene) {
		String symbol = parseSymbol(egene);
		String id = parseID(egene);
		GeneRecord rec = new GeneRecord(symbol);
		rec.setID( id );
		
		String desc = parseDescription(egene);
		rec.setDescription(desc);
		
		String summary = parseSummary(egene);
		rec.setSummary(summary);
		
		//parseGOFunctions(egene);
		
		return rec;
	}
	
	private static String parseID(Element egene) {
		Element trackInfo = childForName(egene, GeneTrackInfo);
		Element trackEl = childForName(trackInfo, GeneTrack);
		if (trackEl == null)
			return "unknown";
		else {
			return textContentForName(trackEl, GeneID);
		}
	}

	private static String parseSummary(Element egene) {
		return textContentForName(egene, GeneSummary);
	}
	
	private static String parseSymbol(Element egene) {
		Element geneInfo = childForName(egene, EntrezGeneGene);
		Element refEl = childForName(geneInfo, GeneRef);
		if (refEl == null)
			return "unknown";
		else {
			return textContentForName(refEl, GeneRefLocus);
		}
	}
	
	private static String parseDescription(Element egene) {
		Element geneInfo = childForName(egene, EntrezGeneGene);
		Element refEl = childForName(geneInfo, GeneRef);
		if (refEl == null)
			return "unknown";
		else {
			return textContentForName(refEl, GeneRefDesc);
		}
	}
	
	private static GOTermList parseGOFunctions(Element egene) {
		Element propsEl = childForName(egene, GeneProperties);
		Element goEl = null; //We search children of props el for this
		
		NodeList children = propsEl.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(GeneCommentary)) {
				String label = textContentForName((Element)child, GeneCommentaryHeading);
				if (label != null && label.equals("GeneOntology")) {
					goEl = (Element)child;
					break;
				}
			}
					
		}
		
		if (goEl == null) {
			System.out.println("Did NOT find GO terms child");
		}
		else {
			System.out.println("Parsing go functions...");
			listChildren(goEl);
			
			System.out.println("Assigning goEl to its GeneCommentary child");
			goEl = childForName(goEl, GeneCommentaryComment);
			listChildren(goEl);
			
			List<Element> goCategories = allChildrenWithName(goEl, GeneCommentaryComment);
			System.out.println("Found " + goCategories.size() + " children with name " + GeneCommentaryComment);
			
			for(Element goType : goCategories) {
				String label = textContentForName(goType, GeneCommentaryLabel);
				System.out.println("Found label:" + label);
			}
		}
		
		return null;
	}
	
//	private static GOTerm parseSingleGOTerm(Element el) {
//		
//	}
	
	/**
	 * Obtain the first child element of the parent element whose nodeName is the given name
	 * or null if no such element child exists
	 * @param parent
	 * @param name
	 * @return
	 */
	public static Element childForName(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			//System.out.println("Looking at node:" + child.getNodeName());
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(name)) {
				return (Element)child;
			}
		}
		return null;
	}
	
	/**
	 * Returns true if the given element has an element child whose name equals the given
	 * name, otherwise false.
	 * @param parent
	 * @param name
	 * @return
	 */
	public static boolean hasChildWithName(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return all children elements of the given node with a name matching the given name
	 * @param parent
	 * @param name
	 * @return
	 */
	public static List<Element> allChildrenWithName(Element parent, String name) {
		List<Element> matches = new ArrayList<Element>();
		NodeList children = parent.getChildNodes();
		
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(name)) {
				matches.add( (Element)child);
			}
		}
		return matches;
	}
	
	private static void listChildren(Element el) {
		NodeList children = el.getChildNodes();
		System.out.println("Children of element " + el.getNodeName() + " :" + children.getLength());
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType()==Node.ELEMENT_NODE) {
				System.out.println("\t" + child.getNodeName() );
			}
		}
	}
	
	/**
	 * Returns the nodeValue of the first child that is an element and has the given node name
	 * @param parent
	 * @param name
	 * @return
	 */
	public static String textContentForName(Element parent, String name) {
		Element child = childForName(parent, name);
		if (child == null)
			return null;
		else {
			NodeList children = child.getChildNodes();
			for(int i=0; i<children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeType() == Node.TEXT_NODE) {
					return node.getNodeValue();
				}
			}
			return null;
		}
	}
}
