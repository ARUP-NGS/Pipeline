package stringdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import math.graph.Graph;
import math.graph.GraphFactory;

import org.w3c.dom.Document;

public class FetchInteractions {

	static final String stringdbURL = "http://string-db.org/api/psi-mi-tab/interactionsList?";
	protected static final int defaultAdditionalNodeCount = 10;
			
	public static List<InteractionNode> getInteractionsList(List<String> geneIDs) throws IOException {
		return getInteractionsList(geneIDs, defaultAdditionalNodeCount);
	}
			
	public static List<InteractionNode> getInteractionsList(List<String> geneIDs, int additionalNodeCount) throws IOException {
		List<InteractionNode> nodes = new ArrayList<InteractionNode>(geneIDs.size()*2);
		StringBuilder urlStr = new StringBuilder(stringdbURL + "identifiers=");
		for(int i=0; i<geneIDs.size()-1; i++) {
			urlStr.append(geneIDs.get(i) + "%0A");
		}
		urlStr.append(geneIDs.get(geneIDs.size()-1));
		urlStr.append("&species=9606");
		urlStr.append("&additional_network_nodes=" + additionalNodeCount);
	
		System.out.println("URL is :" + urlStr.toString());
		URL stringURL = new URL(urlStr.toString());
		URLConnection yc = stringURL.openConnection();
        
		BufferedReader reader = new BufferedReader(new InputStreamReader( yc.getInputStream() ));
        String line = reader.readLine();
        while(line != null) {
        	String[] toks = line.split("\t");
        	if (toks.length < 14) {
        		System.err.println("Error reading input from line :" + line + ", skipping it");
        		line = reader.readLine();
        		continue;
        	}
        	
        	String geneID1 = toks[2];
        	String geneID2 = toks[3];
        	String scoreStr = toks[14];
        	int index = scoreStr.indexOf("|");
        	if (index < 0)
        		index = scoreStr.length();
        	String scoreSub = scoreStr.substring(6, index);
        	//System.out.println("Found gene1: " + geneID1 + " gene2: " + geneID2 + ", score:" + scoreSub);
        	Double score = Double.parseDouble(scoreSub);
        	InteractionNode node = new InteractionNode();
        	node.first = geneID1;
        	node.second = geneID2;
        	node.score = score;
        	nodes.add(node);
        	
        	line = reader.readLine();
        }
         
        return nodes;
	}

	public static Graph constructGraphForGenes(List<String> geneIDs, int size) throws IOException {
		List<InteractionNode> nodeList = getInteractionsList(geneIDs, size);
		return GraphFactory.constructGraphFromList(nodeList);
	}
	
	public static class InteractionNode {
		public String first;
		public String second;
		public double score;
	}
	
//	public static void main(String[] args) {
//		List<String> ids = new ArrayList<String>();
//		ids.add("ENG");
//		ids.add("ACVRL1");
//		ids.add("BMP9");
//		ids.add("SMAD4");
//		
//		FetchInteractions fetcher = new FetchInteractions();
//		try {
//			fetcher.getInteractionsList(ids);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
}
