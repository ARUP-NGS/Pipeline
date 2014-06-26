package buffer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import math.graph.Graph;
import math.graph.GraphFactory;
import ncbi.GeneInfoDB;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import stringdb.FetchInteractions;

/**
 * A weighted graph of genes, where the weights connecting genes represent interaction intensities / strengths
 * @author brendan
 *
 */
public class GeneInteractionGraph extends PipelineObject {

	protected Graph graph = null;
	protected Map<String, String> props = new HashMap<String, String>(); 
	protected FileBuffer source = null;
	protected GeneInfoDB geneInfo = null; //Used to look up synonyms for genes
	
	public static final String KEY_GENES = "key.genes";
	public static final String GRAPH_SIZE = "graph.size";
	
	
	public Graph getGraph() throws IOException {
		if (graph == null) {
			constructGraph();
		}
		return graph;
	}
	
	/**
	 * Construct a graph, either from a CSV file if the source file is set, or dynamically, by looking
	 * fetching info from string-db based on a list of 'key genes' and an (optional) graph size attribute 
	 * @throws IOException
	 */
	private void constructGraph() throws IOException {
		if (source != null) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Constructing gene graph from " + source.getAbsolutePath());
			graph = GraphFactory.constructGraphFromFile(source.getFile()); 
		}
		else {
			
			geneInfo = GeneInfoDB.getDB();
			if (geneInfo == null)
				geneInfo = new GeneInfoDB( new File( GeneInfoDB.defaultDBPath ));
	
			String genesAttr = this.getAttribute(KEY_GENES);
			if (genesAttr == null) {
				throw new IllegalArgumentException("You must specify a list of genes if not are provided to the Gene Graph");
			}
			
			Logger.getLogger(Pipeline.primaryLoggerName).info("Constructing gene graph dynamically from gene list : "+ genesAttr);
			
			List<String> geneList = parseGenes(genesAttr);
			
			String graphSizeAttr = this.getAttribute(GRAPH_SIZE);
			int graphSize = 100;
			if (graphSizeAttr != null) {
				graphSize = Integer.parseInt(graphSizeAttr);
			}
			
			graph = FetchInteractions.constructGraphForGenes(geneList, graphSize);
			
		}
	}


	private List<String> parseGenes(String genesAttr) {
		String[] arr = genesAttr.split(",");
		List<String> genes = new ArrayList<String>();
		for(int i=0; i<arr.length; i++) {
			String symbol = geneInfo.symbolForSynonym( arr[i].trim() );
			if (symbol == null) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not find symbol for gene name : " + arr[i].trim());
			}
			genes.add( symbol );
		}
		return genes;
	}


	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof FileBuffer) {
					source = (FileBuffer)obj;
				}
				else {
					throw new IllegalArgumentException("Found non-FileBuffer object in input list for Operator " + getObjectLabel());
				}
			}
		}
	}
	
	
	@Override
	public void setAttribute(String key, String value) {
		props.put(key, value);
	}

	@Override
	public String getAttribute(String key) {
		return props.get(key);
	}

	@Override
	public Collection<String> getAttributeKeys() {
		return props.keySet();
	}

}
