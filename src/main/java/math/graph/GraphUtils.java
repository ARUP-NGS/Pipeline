package math.graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains a handful of static methods for computing various statistics from a Graph
 * @author brendan
 *
 */
public class GraphUtils {

	public static double weightToDist(double weight) {
		return 1.0/(weight + 0.0001);
	}
	
	/**
	 * Use Dijkstra's algorithm to compute the shortest paths connecting all nodes in the graph
	 * to the given source node 
	 * @param g
	 */
	public static void computeShortestPaths(Graph g, String sourceLabel) {
		g.clearVisitedFlags();
		List<GraphNode> unvisited = new ArrayList<GraphNode>(g.getNodeCount());
		
		unvisited.addAll(g.getNodes());
		
		String distanceKey = "distance.to." + sourceLabel;
		
		GraphNode source = g.getNodeForLabel(sourceLabel);
		if (source == null) {
			System.err.println("ERROR : No node found with label : " + sourceLabel + " cannot compute shortest paths for this gene");
			return;
		}

		unvisited.remove(source);
		source.addAnnotation(distanceKey, 0.0);

		GraphNode current = source;
		
		
		while(current != null) {
			Double thisDist = current.getAnnotation(distanceKey);
			for(int i=0; i<current.getNeighborCount(); i++) {
				GraphNode neighbor = current.getNeighbor(i);
				if (neighbor.isVisited())
					continue;
				
				Double currentDist = neighbor.getAnnotation(distanceKey);
				Double proposedDist = thisDist + weightToDist(current.getWeight(neighbor));
				if (proposedDist < 0.01)
					System.out.println("Whoa, dist is : " + proposedDist + " weight is : " + current.getWeight(neighbor));
				
				if (currentDist == null || proposedDist < currentDist) {
					neighbor.addAnnotation(distanceKey, proposedDist);
				}
				
			}
			
			current.setVisited(true);
			unvisited.remove(current);
			//Next 'current' node is the unvisited one with the smallest distance found so far
			current = null;
			double minDist= Double.POSITIVE_INFINITY;
			
			for(GraphNode n : unvisited) {
				Double dist = n.getAnnotation(distanceKey);
				if (dist != null && dist < minDist) {
					current = n;
					minDist = dist;
				}
			}
		}
		
//		for(GraphNode node : g.getNodes()) {
//			System.out.println("Node : " + node.getLabel() + " " + distanceKey + " : " + node.getAnnotation(distanceKey));
//		}
	}
	
//	public static void main(String[] args) throws IOException {
//		File file = new File("/media/MORE_DATA/HHT/HHT-proteins500.csv");
//		Graph g = GraphFactory.constructGraphFromFile(file);
//		System.out.println("Nodes in graph : " + g.getNodeCount());
//		GraphUtils.computeShortestPaths(g, "ACVRL1");
//	}
}
