package ncbi;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;



/**
 * A class to obtain ref gene information from NCBI. THis uses NCBI's 'e-utils' protocol
 * to obtain an XML representation of much information for a gene. Right now, we focus on
 * just parsing the gene summary information, and ignore everything else
 * @author brendan
 *
 */
public class FetchGeneInfo {

	final String eutilsURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?";
	
	/**
	 * Obtain an xml-style String representation of the document 
	 * @return
	 * @throws TransformerException
	 * @throws IOException 
	 */
	public String getXMLString(Document doc) throws TransformerException, IOException {
		
		 TransformerFactory transfac = TransformerFactory.newInstance();
         Transformer trans = transfac.newTransformer();
         trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
         trans.setOutputProperty(OutputKeys.INDENT, "yes");

         //create string from xml tree
         StringWriter sw = new StringWriter();
         StreamResult result = new StreamResult(sw);
         DOMSource source = new DOMSource(doc);
         trans.transform(source, result);
         String xmlString = sw.toString();

        return xmlString;
	}
	
	public GeneRecord fetchInfoForGene(String geneID) throws IOException, SAXException, ParserConfigurationException, TransformerException {
		//Grab information from NCBI using eutils
		URL eutils = new URL(eutilsURL + "db=gene&id=" + geneID + "&retmode=xml");
        URLConnection yc = eutils.openConnection();
        
        //Create an DOM xml document from the input stream... note that we do not do 
        //any error checking here
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		Document doc = builder.parse( yc.getInputStream() );
				
		//From the DOM document construct a gene record object
        GeneRecord rec = GeneRecordParser.parse(doc);
        return rec;
	}
	
	public static void main(String[] args) throws Exception {
//        FetchGeneInfo fetcher = new FetchGeneInfo();
//        GeneInfoDB geneInfo = new GeneInfoDB(new File("/home/brendan/resources/LRG_RefSeqGene.txt"));
//        String symbol = "ENG";
//        String id = geneInfo.idForSymbol(symbol);
//        GeneRecord rec = fetcher.fetchInfoForGene(id);
//        System.out.println(rec.getSymbol() + " : " + rec.getSummary());
        
//        CachedGeneSummaryDB summaryDB = new CachedGeneSummaryDB();
//        String[] toGet = new String[]{"ENG", "ACVRL1", "FLT1", "ROBO4", "NRP1"};
//        for(int i=0; i<toGet.length; i++) {
//        	String summary = summaryDB.getSummaryForGene(toGet[i]);
//        	System.out.println("Summary for " + toGet[i] + " : " + summary);
//        }
//        
//        summaryDB.writeMapToFile();
		
		
//		PubMedFetcher fetcher = new PubMedFetcher();
//		PubMedRecord rec = fetcher.getPubMedRecordForID(19028676);
//		System.out.println("Title: "  + rec.title);
//		System.out.println("Abstract: "  + rec.abs);
//		System.out.println("Citation: "  + rec.citation);
		
		
		CachedPubmedAbstractDB abstractCache = new CachedPubmedAbstractDB();
		
		PubMedRecord rec = abstractCache.getRecordForID(16343615);
		System.out.println("Title: "  + rec.title);
		System.out.println("Abstract: "  + rec.abs);
		System.out.println("Citation: "  + rec.getCitation());
		
		
		
		abstractCache.writeMapToFile();
		
    }
	
}
