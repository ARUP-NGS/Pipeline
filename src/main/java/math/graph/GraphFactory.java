package math.graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import stringdb.FetchInteractions;
import stringdb.FetchInteractions.InteractionNode;


/**
 * Reads a list of genes (really just arbitrary labels) and connection weights from a file and 
 * returns a Graph constructed from it
 * Expects tab-delimited columns, and one gene pair and weight per row, ala:
 * geneA	geneB	16.2
 * geneB	geneX	7.4
 * etc etc
 * 
 * @author brendan
 *
 */
public class GraphFactory {

	/**
	 * Construct a Graph object from the given list of InteractionNodes, which are produced by
	 * from the StringDB class FetchInteractions 
	 * @param nodeList
	 * @return
	 * @throws IOException
	 */
	public static Graph constructGraphFromList(List<FetchInteractions.InteractionNode> nodeList) throws IOException {
		Graph graph = new Graph();
		
		for(InteractionNode node : nodeList) {
			graph.createNodesAndEdge(node.first, node.second, node.score);
		}
		return graph;
	}
	
	/**
	 * Construct a Graph object from text stored in a file, the file is supposed to look like
	 * label1	label2	weight
	 * label1	label3	weight
	 * etc.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Graph constructGraphFromFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		Graph graph = new Graph();
		
		String line = reader.readLine();
		while(line != null) {
			if (line.trim().length()==0 || line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			if (toks.length < 3) {
				throw new IllegalArgumentException("Incorrect number of tokens on this line: " + line);
			}
			
			String geneA = toks[0].trim();
			String geneB = toks[1].trim();
			Double weight = Double.parseDouble(toks[2]);
			if (weight > 2) {
				System.out.println("Whoa! Big weight: " + weight);
			}
			graph.createNodesAndEdge(geneA, geneB, weight);
			line = reader.readLine();
		}
		
		return graph;
	}
	
}
