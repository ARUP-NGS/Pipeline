package math.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of GraphNodes, some of which may be connected by a real-valued weight
 * @author brendan
 *
 */
public class Graph {

	protected List<GraphNode> nodes = new ArrayList<GraphNode>();
	
	/**
	 * Create a new node with no edges and the given label and add it to this graph
	 * @param label
	 */
	public GraphNode createNode(String label) {
		if (getNodeForLabel(label) != null) 
			throw new IllegalArgumentException("A node with label " + label + " already exists");
		GenericGraphNode node = new GenericGraphNode(label);
		nodes.add(node);
		return node;
	}
	
	/**
	 * The total number of nodes in the graph
	 * @return
	 */
	public int getNodeCount() {
		return nodes.size();
	}
	
	/**
	 * Obtain the node whose label is equal to the given label, or null if no such node exists
	 * @param label
	 * @return
	 */
	public GraphNode getNodeForLabel(String label) {
		for(GraphNode node : nodes) {
			if (node.getLabel().equals(label))
				return node;
		}
		return null;
	}
	
	/**
	 * Create an edge between the two given nodes with the specified weight
	 * @param a
	 * @param b
	 * @param weight
	 */
	public void createEdge(GraphNode a, GraphNode b, double weight) {
		a.addEdge(b, weight);
		b.addEdge(a, weight);
	}
	
	/**
	 * Create an edge connecting the two nodes with the given labels
	 * @param aLabel
	 * @param bLabel
	 * @param weight
	 */
	public void createEdge(String aLabel, String bLabel, double weight) {
		GraphNode a = getNodeForLabel(aLabel);
		GraphNode b = getNodeForLabel(bLabel);
		a.addEdge(b, weight);
		b.addEdge(a, weight);
	}

	/**
	 * Creates nodes with the given labels if they do not already exist, and connects them
	 * with the given weight
	 * @param aLabel
	 * @param bLabel
	 * @param weight
	 */
	public void createNodesAndEdge(String aLabel, String bLabel, double weight) {
		GraphNode a = getNodeForLabel(aLabel);
		GraphNode b = getNodeForLabel(bLabel);
		if (a == null)
			a = createNode(aLabel);
		
		if (b==null)
			b = createNode(bLabel);
		
		a.addEdge(b, weight);
		b.addEdge(a, weight);
	}

	
	/**
	 * Create an edge connecting the two nodes with weight 1
	 * @param a
	 * @param b
	 */
	public void createEdge(GraphNode a, GraphNode b) {
		a.addEdge(b, 1.0);
		b.addEdge(a, 1.0);
	}
	
	public void removeEdge(GraphNode a, GraphNode b) {
		a.removeEdge(b);
		b.removeEdge(a);
	}
	
	public void clearVisitedFlags() {
		for(GraphNode n : nodes) {
			n.setVisited(false);
		}
	}
	
	/**
	 * Obtain the source list of all nodes in this graph
	 * @return
	 */
	public List<GraphNode> getNodes() {
		return nodes;
	}
	
}
