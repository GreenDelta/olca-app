package org.openlca.app.editors.graphical.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class MinimalTreeLayout {

	private final Map<Integer, Integer> heights = new HashMap<>();
	private final Map<Integer, Integer> widths = new HashMap<>();

	private void applyLayout(ProcessNode[] processFigures, Graph graph) {
		Map<Long, ProcessNode> nodes = new HashMap<>();
		for (ProcessNode node : processFigures)
			nodes.put(node.process.id, node);

		// build "width-map"
		for (int x = graph.minimumX; x <= graph.maximumX; x++) {
			widths.put(x, 0);
			for (int y = graph.minimumY; y <= graph.maximumY; y++) {
				Node node = graph.coordinateSystem.get(new Point(x, y));
				if (node == null)
					continue;
				int width = Math.max(widths.get(x), nodes.get(node.key).getSize().width);
				widths.put(x, width);
			}
		}
		// build "height-map"
		for (int y = graph.minimumY; y <= graph.maximumY; y++) {
			heights.put(y, 0);
			for (int x = graph.minimumX; x <= graph.maximumX; x++) {
				Node node = graph.coordinateSystem.get(new Point(x, y));
				if (node == null)
					continue;
				int height = Math.max(heights.get(y), nodes.get(node.key).getSize().height);
				heights.put(y, height);
			}
		}
		int xPosition = 25;
		for (int x = graph.minimumX; x <= graph.maximumX; x++) {
			if (x > graph.minimumX && widths.get(x - 1) > 0)
				xPosition += widths.get(x - 1) + LayoutManager.H_SPACE;
			int yPosition = 25;
			for (int y = graph.minimumY; y <= graph.maximumY; y++) {
				Node node = graph.coordinateSystem.get(new Point(x, y));
				if (y > graph.minimumY && heights.get(y - 1) > 0)
					yPosition += heights.get(y - 1) + LayoutManager.V_SPACE;
				if (node == null)
					continue;
				ProcessNode pNode = nodes.get(node.key);
				pNode.setXyLayoutConstraints(new Rectangle(xPosition, yPosition,
						pNode.getSize().width, pNode.getSize().height));
			}
		}
	}

	private Graph buildGraph(ProcessNode[] processNodes) {
		Graph graph = new Graph();
		Map<Long, Node> nodes = graph.nodes;
		for (ProcessNode processNode : processNodes) {
			long id = processNode.process.id;
			nodes.put(id, new Node(id));
		}
		for (ProcessNode processNode : processNodes) {
			for (Link link : processNode.links) {
				long sourceId = link.outputNode.process.id;
				long targetId = link.inputNode.process.id;
				if (nodes.get(sourceId) == null || nodes.get(targetId) == null)
					continue;
				// add edge to source node
				nodes.get(sourceId).outgoingEdges.add(new Edge(nodes.get(sourceId), nodes.get(targetId)));
				// add edge to target node
				nodes.get(targetId).incomingEdges.add(new Edge(nodes.get(sourceId), nodes.get(targetId)));
			}
		}
		return graph;
	}

	public void layout(ProcessNode[] processNodes) {
		Graph graph = buildGraph(processNodes);
		graph.initLocations();
		graph.optimize();
		applyLayout(processNodes, graph);
		widths.clear();
		heights.clear();
	}

	class Edge {

		private Node start;
		private Node end;

		public Edge(Node start, Node end) {
			this.start = start;
			this.end = end;
		}

		int getWeight() {
			@SuppressWarnings("deprecation")
			int weight = start.location.getDistanceOrthogonal(end.location);
			if (start.location.x > end.location.x)
				weight += 2;
			return weight;
		}

		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof Edge))
				return false;
			Edge edge = (Edge) obj;
			if (!start.equals(edge.start))
				return false;
			if (!end.equals(edge.end))
				return false;
			return true;
		}
	}

	class Graph {

		private Map<Point, Node> coordinateSystem = new HashMap<>();
		private Map<Long, Node> nodes = new HashMap<>();
		int maximumX = 0;
		int maximumY = 0;
		int minimumX = 0;
		int minimumY = 0;

		private void setLocation(Node node, int xStart, int yStart) {
			int x = xStart;
			int y = yStart;
			int addition = 1;
			boolean add = false;
			while (coordinateSystem.get(new Point(x, y)) != null) {
				if (!add) {
					y = yStart - addition;
					add = true;
				} else {
					y = yStart + addition;
					add = false;
					addition++;
				}
			}
			node.location = new Point(x, y);
			checkNewMinimumsAndMaximums(x, y);
			coordinateSystem.put(new Point(x, y), node);
			for (Edge edge : node.incomingEdges)
				if (edge.start.location == null)
					setLocation(edge.start, x - 1, y);
			for (Edge edge : node.outgoingEdges)
				if (edge.end.location == null)
					setLocation(edge.end, x + 1, y);
		}

		void checkNewMinimumsAndMaximums(int x, int y) {
			if (x < minimumX)
				minimumX = x;
			else if (x > maximumX)
				maximumX = x;
			if (y < minimumY)
				minimumY = y;
			else if (y > maximumY)
				maximumY = y;
		}

		void initLocations() {
			for (Node node : nodes.values()) {
				if (node.location != null)
					continue;
				setLocation(node, 0, 0);
			}
		}

		void optimize() {
			boolean stopOptimizing = false;
			while (!stopOptimizing) {
				stopOptimizing = true;
				for (Node node : nodes.values()) {
					boolean stop = false;
					while (!stop) {
						boolean result = optimize(node);
						if (!result) {
							stopOptimizing = false;
						} else {
							stop = true;
						}
					}
				}
			}
		}

		private boolean optimize(Node node) {
			Edge heaviestEdge = node.getHeaviestEdge();
			if (heaviestEdge == null)
				return true;
			int oldWeightSum = node.getEdgesWeightSum();
			Point oldLocation = node.location.getCopy();
			Edge lightestEdge = node.getLightestEdge();
			boolean nodeIsStart = heaviestEdge.start.equals(node);
			Node n = nodeIsStart ? heaviestEdge.end : heaviestEdge.start;
			int x = n.location.x + (nodeIsStart ? 1 : -1);
			int y = n.location.y;
			boolean stopTrying = false;
			boolean canMove = false;
			int divisor = 2;
			while (!stopTrying) {
				Node nearest = lightestEdge.start.key == node.key ? lightestEdge.end : lightestEdge.start;
				Node farest = heaviestEdge.start.key == node.key ? heaviestEdge.end : heaviestEdge.start;
				if (!lightestEdge.equals(heaviestEdge)) {
					x = Math.min(nearest.location.x, farest.location.x);
					x += Math.abs(nearest.location.x - farest.location.x) / divisor;
					y = Math.min(nearest.location.y, farest.location.y);
					y += Math.abs(nearest.location.y - farest.location.y) / divisor;
				}
				int tempY = y;
				int addition = 1;
				boolean add = true;
				// find empty location
				while (coordinateSystem.get(new Point(x, y)) != null) {
					if (add) {
						y = tempY + addition;
						add = false;
					} else {
						y = tempY - addition;
						add = true;
						addition++;
					}
				}
				node.location = new Point(x, y);
				int newWeightSum = node.getEdgesWeightSum();
				// if old weight > new weight we found a better
				// location
				if (oldWeightSum > newWeightSum) {
					stopTrying = true;
					canMove = true;
				}
				divisor++;
				if (divisor >= Math.abs(nearest.location.y - farest.location.y)) {
					stopTrying = true;
				}
			}
			if (canMove) {
				// move nodes
				coordinateSystem.put(node.location.getCopy(), node);
				coordinateSystem.put(oldLocation.getCopy(), null);
				checkNewMinimumsAndMaximums(x, y);
				return false; // stop optimizing
			} else {
				// no better spot found, stop optimizing, best
				// location is already set
				node.location = oldLocation.getCopy();
				return true; // stop
			}
		}

	}

	class Node {

		private final long key;
		private final List<Edge> incomingEdges = new ArrayList<>();
		private final List<Edge> outgoingEdges = new ArrayList<>();
		private Point location;

		public Node(final long key) {
			this.key = key;
		}

		int getEdgesWeightSum() {
			int weightSum = 0;
			for (Edge edge : incomingEdges) {
				weightSum += edge.getWeight();
			}
			for (Edge edge : outgoingEdges) {
				weightSum += edge.getWeight();
			}
			return weightSum;
		}

		Edge getHeaviestEdge() {
			Edge heaviestEdge = null;
			int heaviestWeight = 0;
			for (Edge edge : incomingEdges) {
				int weight = edge.getWeight();
				if (weight <= heaviestWeight)
					continue;
				heaviestWeight = weight;
				heaviestEdge = edge;
			}
			for (Edge edge : outgoingEdges) {
				int weight = edge.getWeight();
				if (weight <= heaviestWeight)
					continue;
				heaviestWeight = weight;
				heaviestEdge = edge;

			}
			return heaviestEdge;
		}

		Edge getLightestEdge() {
			Edge lightestEdge = null;
			int lightestWeight = Integer.MAX_VALUE;
			for (Edge edge : incomingEdges) {
				int weight = edge.getWeight();
				if (weight >= lightestWeight)
					continue;
				lightestWeight = weight;
				lightestEdge = edge;
			}
			for (Edge edge : outgoingEdges) {
				int weight = edge.getWeight();
				if (weight >= lightestWeight)
					continue;
				lightestWeight = weight;
				lightestEdge = edge;
			}
			return lightestEdge;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof Node)
				if (key == ((Node) obj).key)
					return true;
			return false;
		}
	}

}
