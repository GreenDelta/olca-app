package org.openlca.app.editors.graph.layouts;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.search.MutableProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

import java.util.*;

public class TreeLayoutProcessor {

	public static int H_SPACE = 25;
	public static int V_SPACE = 25;

	private final Graph graph;
	private final Map<Long, Integer> containing = new HashMap<>();
	private final Map<Integer, Integer> heights = new HashMap<>();
	private final Map<Integer, Integer> widths = new HashMap<>();
	private final Map<Point, Long> locations = new HashMap<>();
	private MutableProcessLinkSearchMap linkSearch;

	public TreeLayoutProcessor(Graph graph) {
		this.graph = graph;
	}

	private void applyLayout(Vertex node, int addition, int actualDepth) {
		int x = actualDepth;
		int y = node.getSize() / 2 + addition;
		while (locations.get(new Point(x, y)) != null) {
			y++;
			addition++;
		}
		locations.put(new Point(x, y), node.processId);
		for (int i = 0; i < node.leftChildren.size(); i++) {
			Vertex child = node.leftChildren.get(i);
			int sizeAddition = 0;
			for (int j = 0; j < i; j++)
				sizeAddition += node.leftChildren.get(j).getSize();
			applyLayout(child, addition + sizeAddition, actualDepth - 1);
		}
		for (int i = 0; i < node.rightChildren.size(); i++) {
			Vertex child = node.rightChildren.get(i);
			int sizeAddition = 0;
			for (int a = 0; a < node.leftChildren.size(); a++)
				sizeAddition += node.leftChildren.get(a).getSize();
			for (int j = 0; j < i; j++)
				sizeAddition += node.rightChildren.get(j).getSize();
			applyLayout(child, addition + sizeAddition, actualDepth + 1);
		}
	}

	private Vertex build(ProductSystem productSystem) {
		Vertex vertex = new Vertex();
		vertex.processId = productSystem.referenceProcess.id;
		build(new Vertex[] { vertex });
		return vertex;
	}

	private void build(Vertex[] nodes) {
		List<Vertex> children = new ArrayList<>();
		for (Vertex node : nodes) {
			long processId = node.processId;
			for (ProcessLink link : linkSearch.getLinks(processId)) {
				if (link.processId != processId)
					continue;
				long providerId = link.providerId;
				if (containing.get(providerId) != null)
					continue;
				Vertex child = new Vertex();
				child.processId = providerId;
				node.leftChildren.add(child);
				containing.put(child.processId, 1);
				children.add(child);
			}
		}
		if (children.size() > 0)
			build(children.toArray(new Vertex[children.size()]));
		children.clear();
		for (Vertex node : nodes) {
			long processId = node.processId;
			for (ProcessLink link : linkSearch.getLinks(processId)) {
				if (link.providerId != processId)
					continue;
				long recipientId = link.processId;
				if (containing.get(recipientId) != null)
					continue;
				Vertex child = new Vertex();
				child.processId = recipientId;
				node.rightChildren.add(child);
				containing.put(child.processId, 1);
				children.add(child);
			}
		}
		if (children.size() > 0)
			build(children.toArray(new Vertex[0]));
	}

	private void prepare(Graph graph) {
		for (long processId : graph.getProductSystem().processes)
			if (graph.getProcessNode(processId) == null)
				containing.put(processId, 1);
	}

	public Map<Integer, Integer> getHeights() {
		return heights;
	}

	public Map<Integer, Integer> getWidths() {
		return widths;
	}

	public Map<Node, Point> getMoveDeltas() {
		this.linkSearch = graph.linkSearch;
		Map<Node, Point> mapNodeToLocation = new HashMap<>();
		prepare(graph);
		List<Vertex> vertices = new ArrayList<>();
		Vertex mainVertex = build(graph.getProductSystem());
		mainVertex.sort();
		vertices.add(mainVertex);
		for (Node node : graph.getChildren()) {
			if (containing.get(node.descriptor.id) != null)
				continue;
			var vertex = new Vertex();
			vertex.processId = node.descriptor.id;
			build(new Vertex[] { vertex });
			vertex.sort();
			vertices.add(vertex);
		}
		int additionalHeight = 0;
		for (Vertex vertex : vertices) {
			int newAdditionalHeight = 0;
			locations.clear();
			applyLayout(vertex, 0, vertex.getLeftDepth());
			int minimumX = Integer.MAX_VALUE;
			int maximumX = Integer.MIN_VALUE;
			int minimumY = Integer.MAX_VALUE;
			int maximumY = Integer.MIN_VALUE;
			for (Point p : locations.keySet()) {
				if (p.x < minimumX)
					minimumX = p.x;
				if (p.x > maximumX)
					maximumX = p.x;
				if (p.y < minimumY)
					minimumY = p.y;
				if (p.y > maximumY)
					maximumY = p.y;
			}
			Map<Long, Node> mapIDToNode = new HashMap<>();
			for (Node node : graph.getChildren())
				mapIDToNode.put(node.descriptor.id, node);
			for (int x = minimumX; x <= maximumX; x++) {
				widths.put(x, 0);
				for (int y = minimumY; y <= maximumY; y++) {
					Long processId = locations.get(new Point(x, y));
					if (processId == null)
						continue;
					var node = mapIDToNode.get(processId);
					if (node == null)
						continue;
					Dimension size = node.getSize();
					if (size == null)
						continue;
					int width = Math.max(widths.get(x), size.width);
					widths.put(x, width);
				}
			}
			for (int y = minimumY; y <= maximumY; y++) {
				heights.put(y, 0);
				for (int x = minimumX; x <= maximumX; x++) {
					Long processId = locations.get(new Point(x, y));
					if (processId == null)
						continue;
					var node = mapIDToNode.get(processId);
					if (node == null)
						continue;
					Dimension size = node.getSize();
					if (size == null)
						continue;
					int height = Math.max(heights.get(y), size.height);
					heights.put(y, height);
				}
			}
			int xPosition = H_SPACE;
			for (int x = minimumX; x <= maximumX; x++) {
				if (x > minimumX && widths.get(x - 1) > 0)
					xPosition += widths.get(x - 1) + H_SPACE;
				int yPosition = V_SPACE;
				for (int y = minimumY; y <= maximumY; y++) {
					Long processId = locations.get(new Point(x, y));
					if (y > minimumY && heights.get(y - 1) > 0)
						yPosition += heights.get(y - 1) + V_SPACE;
					if (processId == null)
						continue;
					var node = mapIDToNode.get(processId);
					if (node == null)
						continue;
					var newLoc = new Point(xPosition, yPosition + additionalHeight);
					var oldLoc = node.getLocation();
					var moveDelta = newLoc.getTranslated(oldLoc.getNegated());
					mapNodeToLocation.put(node, moveDelta);
					newAdditionalHeight = Math.max(newAdditionalHeight, yPosition + additionalHeight + node.getSize().height);
				}
			}
			additionalHeight = newAdditionalHeight + V_SPACE;
		}
		containing.clear();
		widths.clear();
		heights.clear();
		locations.clear();
		return mapNodeToLocation;
	}

	private static class Vertex {

		long processId;
		List<Vertex> leftChildren = new ArrayList<>();
		List<Vertex> rightChildren = new ArrayList<>();

		int getLeftDepth() {
			if (leftChildren.size() == 0)
				return 0;
			int depth = 1;
			int depthAdd = 0;
			for (Vertex leftChild : leftChildren)
				depthAdd = Math.max(depthAdd, leftChild.getLeftDepth());
			depth += depthAdd;
			return depth;
		}

		int getSize() {
			if (rightChildren.size() == 0 && leftChildren.size() == 0)
				return 1;
			int size = 0;
			for (Vertex rightChild : rightChildren) size += rightChild.getSize();
			for (Vertex leftChild : leftChildren) size += leftChild.getSize();
			return size;
		}

		void sort() {
			List<Vertex> temp = new ArrayList<>(leftChildren);
			temp.sort((o1, o2) -> Integer.compare(o2.getSize(), o1.getSize()));
			leftChildren.clear();
			int count = 0;
			int i = 0;
			while (count < temp.size()) {
				leftChildren.add(temp.get(i));
				count++;
				if (count < temp.size()) {
					leftChildren.add(temp.get(temp.size() - i - 1));
					count++;
				}
				i++;
			}
		}

	}

}
