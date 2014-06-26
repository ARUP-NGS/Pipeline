package ncbi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that uses eutils to fetch a pubmed abstract & associated info for a particular pubmed id.
 * PubMed ID's for a gene can be obtained by querying GenePubMedDB. 
 * @author brendan
 *
 */
public class PubMedFetcher {
	
	final String eutilsURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?";
	final static String ARTICLE_SET = "PubmedArticleSet";
	final static String PUBMED_ARTICLE = "PubmedArticle";
	final static String MEDLINE_CITATION = "MedlineCitation";
	final static String PUB_DATE = "PubDate";
	final static String YEAR = "Year";
	final static String ARTICLE = "Article";
	final static String TITLE = "ArticleTitle";
	final static String ABSTRACT = "Abstract";
	final static String ABSTRACT_TEXT = "AbstractText";
	final static String JOURNAL = "Journal";
	final static String JOURNAL_TITLE = "Title";
	final static String JOURNAL_ISSUE = "JournalIssue";
	final static String VOLUME = "Volume";
	final static String ISSUE = "Issue";
	final static String PMID = "PMID";
	

	/**
	 * Retrieve a list of pubmed records associated with the list of pubmed ids
	 * @param pubmedIDs
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws XMLParseException
	 */
	public List<PubMedRecord> getPubMedRecordForIDs(List<Integer> pubmedIDs) throws IOException, ParserConfigurationException, SAXException {
		StringBuilder strb = new StringBuilder();
		for (Integer id : pubmedIDs) {
			strb.append("," + id);
		}
		
		if (pubmedIDs.size()==0) {
			System.err.println("Warning : no ids found in pubmed id list");
			return new ArrayList<PubMedRecord>();
		}
		URL eutils = new URL(eutilsURL + "db=pubmed&id=" + strb.toString().substring(1) + "&retmode=xml");
        Document doc = retriveDocument(eutils);
        List<PubMedRecord> records = new ArrayList<PubMedRecord>(2);
		try {
			records = parseDomToPubMed(doc);
		} catch (XMLParseException e) {
			System.err.println("Error reading pubmed records : " + e.getMessage());
		}
        
        return records;
	}
	
	/**
	 * Obtain a PubMedRecord that contains information for article associated with the given pubmed ID
	 * @param pubmedID
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public PubMedRecord getPubMedRecordForID(Integer pubmedID) throws IOException, ParserConfigurationException, SAXException {
		URL eutils = new URL(eutilsURL + "db=pubmed&id=" + pubmedID + "&retmode=xml");
        Document doc = retriveDocument(eutils);
				
		//From the DOM document construct a gene record object
        
		try {
			List<PubMedRecord> records = parseDomToPubMed(doc);
			if (records.size()==1)
				return records.get(0);
			else
				return null;

		} catch (XMLParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Attempt to retrieve the requested document from NCBI using eUtils, if successful we return a DOM Document
	 * object containing the information
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private Document retriveDocument(URL url) throws IOException, ParserConfigurationException, SAXException {
		URLConnection yc = url.openConnection();
        
        //Create an DOM xml document from the input stream... note that we do not do 
        //any error checking here
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		Document doc = builder.parse( yc.getInputStream() );
		return doc;
	}
	
	/**
	 * Take a DOM document and parse it to obtain 
	 * @param doc
	 * @return
	 * @throws XMLParseException 
	 */
	private List<PubMedRecord> parseDomToPubMed(Document doc) throws XMLParseException {
		List<PubMedRecord> records = new ArrayList<PubMedRecord>(56);
		Element rootEl = doc.getDocumentElement();
		if (rootEl == null || (!rootEl.getNodeName().equals(ARTICLE_SET))) {
			throw new XMLParseException("This does not look like an NCBI article set document");
		}
		
		List<Element> pubmedArticleElements = getChildrenByName(rootEl, PUBMED_ARTICLE);
		if (pubmedArticleElements.size() == 0) {
			//System.err.println("No articles found in document.");
			return records;
		}
		
		for(Element pubmedArticle : pubmedArticleElements) {
			try {
				PubMedRecord rec = parseSingleArticle(pubmedArticle);
				if (rec != null)
					records.add(rec);
			}
			catch (XMLParseException ex) {
				//Skip this record
			}
		}
		
		return records;
	}
	
	/**
	 * Parse a single PubMedRecord from the given article element. We expect that the element is of
	 * type 'Pub,edArticle'
	 * @param pubmedArticleEl
	 * @return
	 * @throws XMLParseException
	 */
	private PubMedRecord parseSingleArticle(Element pubmedArticleEl) throws XMLParseException {
		Element medlineCitationEl = getChildByName(pubmedArticleEl, MEDLINE_CITATION);
		if (medlineCitationEl == null) {
			throw new XMLParseException("Could not find medline citation element");
		}
		
		Element idEl = getChildByName(medlineCitationEl, PMID);
		String idStr = getTextContent(idEl);
		Integer id = Integer.parseInt(idStr);
		
		
		Element articleEl = getChildByName(medlineCitationEl, ARTICLE);
		//Journal title & info
		Element journalEl = getChildByName(articleEl, JOURNAL);
		Element journalIssueEl = getChildByName(journalEl, JOURNAL_ISSUE);
		Element journalVolumeEl = getChildByName(journalIssueEl, VOLUME);
		Element issueEl = getChildByName(journalIssueEl, ISSUE);
		String volume = "?";
		if (journalVolumeEl != null)
			volume = getTextContent(journalVolumeEl);
		String issue = "?";
		if (issueEl != null)
			issue = getTextContent(issueEl);
		Element pubDateEl = getChildByName(journalIssueEl, PUB_DATE);
		Element yearEl = getChildByName(pubDateEl, YEAR);
		Integer year = -1;
		if (yearEl != null)
			year = Integer.parseInt( getTextContent(yearEl));
				
		Element journalTitleEl = getChildByName(journalEl, JOURNAL_TITLE);
		String journalTitle = getTextContent(journalTitleEl);
		String citation = journalTitle + " " + volume + ":" + issue + " (" + year + ")";
				
		Element articleTitleEl = getChildByName(articleEl, TITLE);
		String title = getTextContent(articleTitleEl);
		
		Element abstractEl = getChildByName(articleEl, ABSTRACT);
		StringBuilder absStr = new StringBuilder();
		
		if (abstractEl != null) {
			NodeList abstractChildren = abstractEl.getChildNodes();
			for(int i=0; i<abstractChildren.getLength(); i++) {
				Node child = abstractChildren.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(ABSTRACT_TEXT)) {
					absStr.append( getTextContent( (Element)child));
				}
			}
		}
		if (absStr.length()==0)
			absStr = new StringBuilder("?");
		
		PubMedRecord rec = new PubMedRecord();
		rec.title = title.replace("\t", " ");
		rec.abs = absStr.toString().replace("\t", " ");
		rec.yearCreated = year;
		rec.citation = citation;
		rec.pubMedID = id;
		return rec;
	}
	
	/**
	 * Searches the children of this element for an element whose node-name is the given childName
	 * and returns it, or null if there is no such node
	 * @param el
	 * @param childName
	 * @return
	 */
	private static Element getChildByName(Element el, String childName) {
		NodeList children = el.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(childName) ) {
				return (Element)child;
			}
		}
		return null;
	}
	
	/**
	 * Retrieve all children whose are elements and whose names equal the given name
	 * This returns an empty list if no children whose names match are found
	 * @param el
	 * @param childName
	 * @return
	 */
	private static List<Element> getChildrenByName(Element el, String childName) {
		NodeList children = el.getChildNodes();
		List<Element> elements = new ArrayList<Element>(28);
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(childName) ) {
				elements.add( (Element)child );
			}
		}
		return elements;
	}
	
	/**
	 * Searches the child list of this element and returns the value of the first text-node
	 * @param el
	 * @return
	 */
	private static String getTextContent(Element el) {
		NodeList children = el.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				return child.getNodeValue();
			}
		}
		return null;
	}


	class XMLParseException extends Exception {
		
		public XMLParseException(String message) {
			super(message);
		}
	}
	
}
