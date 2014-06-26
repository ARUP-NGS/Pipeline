package math.graph;

import java.util.HashMap;
import java.util.List;

public interface GraphNode {

	/**
	 * An arbitrary but unique identifier for this node
	 * @return
	 */
	public String getLabel();
	
	/**
	 * Create a new connection between the this node and the destination node, with weight given
	 * @param destination
	 * @param weight
	 * @return
	 */
	public void addEdge(GraphNode destination, double weight);
	
	/**
	 * Removes the edge connecting this node to the given node
	 * @param destination
	 * @return
	 */
	public void removeEdge(GraphNode destination);
	
	/**
	 * Obtain the weight for an edge connecting this node to the destination node. By convention
	 * if there is no edge connecting the nodes this returns zero
	 * @param destination
	 * @return
	 */
	public double getWeight(GraphNode destination);
	
	public int getNeighborCount();
	
	public GraphNode getNeighbor(int which);

	
	
	public void setVisited(boolean visited);
	
	public boolean isVisited();
	
	public void addAnnotation(String key, Double val);
	
	public Double getAnnotation(String key);

}
