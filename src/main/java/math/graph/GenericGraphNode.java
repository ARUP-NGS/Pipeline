package math.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple and generic implementation of graph node that stores neighbors in a list 
 * @author brendan
 *
 */
public class GenericGraphNode implements GraphNode {

	private static int nodeCount = 0; //Total number of nodes created
	private int myNumber = nodeCount; //Unique number for this node
	protected List<WeightedNeighbor> neighbors = new ArrayList<WeightedNeighbor>(4);
	
	private String label = "?";
	private Map<String, Double> annotations = null;
	private boolean visited = false;
	
	public GenericGraphNode() {
		nodeCount++;
		setLabel("Node #" + myNumber);
	}
	
	public GenericGraphNode(String label) {
		nodeCount++;
		setLabel(label);
	}
	
	
	
	public void addAnnotation(String key, Double val) {
		if (annotations == null)
			annotations = new HashMap<String, Double>();
		
		annotations.put(key,val);
	}
	
	public Double getAnnotation(String key) {
		if (annotations == null)
			return null;
		else
			return annotations.get(key);
	}
	
	public boolean hasAnnotation(String key) {
		if (annotations == null)
			return false;
		else 
			return annotations.get(key) != null;
	}
	
	
	private void setLabel(String label) {
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void addEdge(GraphNode destination, double weight) {
		removeEdge(destination);
		WeightedNeighbor n = new WeightedNeighbor(destination, weight);
		neighbors.add(n);
	}

	@Override
	public void removeEdge(GraphNode destination) {
		WeightedNeighbor toRemove = null;
		for(WeightedNeighbor n : neighbors) {
			if (n.neighbor == destination) {
				toRemove = n;
			}
		}
		
		if (toRemove != null)
			neighbors.remove(toRemove);
	}
	
	@Override
	public double getWeight(GraphNode destination) {
		for(WeightedNeighbor n : neighbors) {
			if (n.neighbor == destination) {
				return n.weight;
			}
		}
		return 0;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	
	public boolean isVisited() {
		return visited;
	}
	
	class WeightedNeighbor {
		final GraphNode neighbor;
		double weight;
		
		public WeightedNeighbor(GraphNode neighbor, double w) {
			this.neighbor = neighbor;
			this.weight = w;
		}
	}

	public String toString() {
		return getLabel();
	}

	@Override
	public int getNeighborCount() {
		return neighbors.size();
	}

	@Override
	public GraphNode getNeighbor(int which) {
		return neighbors.get(which).neighbor;
	}


	
}
