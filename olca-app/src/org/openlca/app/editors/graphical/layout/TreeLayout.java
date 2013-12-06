package org.openlca.app.editors.graphical.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.matrix.ProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class TreeLayout {

	private Map<Long, Integer> containing = new HashMap<>();
	private Map<Integer, Integer> heights = new HashMap<>();
	private Map<Integer, Integer> widths = new HashMap<>();
	private Map<Point, Long> locations = new HashMap<>();
	private ProcessLinkSearchMap linkSearch;

	private void applyLayout(Node node, int addition, int actualDepth) {
		int x = actualDepth;
		int y = node.getSize() / 2 + addition;
		while (locations.get(new Point(x, y)) != null) {
			y++;
			addition++;
		}
		locations.put(new Point(x, y), node.processId);
		for (int i = 0; i < node.leftChildren.size(); i++) {
			Node child = node.leftChildren.get(i);
			int sizeAddition = 0;
			for (int j = 0; j < i; j++)
				sizeAddition += node.leftChildren.get(j).getSize();
			applyLayout(child, addition + sizeAddition, actualDepth - 1);
		}
		for (int i = 0; i < node.rightChildren.size(); i++) {
			Node child = node.rightChildren.get(i);
			int sizeAddition = 0;
			for (int a = 0; a < node.leftChildren.size(); a++)
				sizeAddition += node.leftChildren.get(a).getSize();
			for (int j = 0; j < i; j++)
				sizeAddition += node.rightChildren.get(j).getSize();
			applyLayout(child, addition + sizeAddition, actualDepth + 1);
		}
	}

	private Node build(ProductSystem productSystem) {
		Node node = new Node();
		node.processId = productSystem.getReferenceProcess().getId();
		build(productSystem, new Node[] { node });
		return node;
	}

	private void build(ProductSystem productSystem, Node[] nodes) {
		List<Node> children = new ArrayList<>();
		for (Node node : nodes) {
			long processId = node.processId;
			for (ProcessLink link : linkSearch.getLinks(processId)) {
				if (link.getRecipientId() != processId)
					continue;
				long providerId = link.getProviderId();
				if (containing.get(providerId) != null)
					continue;
				Node child = new Node();
				child.processId = providerId;
				node.leftChildren.add(child);
				containing.put(child.processId, 1);
				children.add(child);
			}
		}
		if (children.size() > 0)
			build(productSystem, children.toArray(new Node[children.size()]));
		children.clear();
		for (Node node : nodes) {
			long processId = node.processId;
			for (ProcessLink link : linkSearch.getLinks(processId)) {
				if (link.getProviderId() != processId)
					continue;
				long recipientId = link.getRecipientId();
				if (containing.get(recipientId) != null)
					continue;
				Node child = new Node();
				child.processId = recipientId;
				node.rightChildren.add(child);
				containing.put(child.processId, 1);
				children.add(child);
			}
		}
		if (children.size() > 0)
			build(productSystem, children.toArray(new Node[children.size()]));
	}

	private void prepare(ProductSystemNode productSystemNode) {
		for (ProcessNode processNode : productSystemNode.getChildren()) {
			long id = processNode.getProcess().getId();
			if (!processNode.isVisible()) {
				Dimension size = processNode.getSize();
				containing.put(id, 1);
				processNode.setXyLayoutConstraints(new Rectangle(0, 0,
						size.width, size.height));
			}
		}
		for (long processId : productSystemNode.getProductSystem()
				.getProcesses())
			if (productSystemNode.getProcessNode(processId) == null)
				containing.put(processId, 1);
	}

	public Map<Integer, Integer> getHeights() {
		return heights;
	}

	public Map<Integer, Integer> getWidths() {
		return widths;
	}

	public void layout(ProductSystemNode productSystemNode) {
		this.linkSearch = productSystemNode.getLinkSearch();
		prepare(productSystemNode);
		List<Node> nodes = new ArrayList<>();
		Node mainNode = build(productSystemNode.getProductSystem());
		mainNode.sort();
		nodes.add(mainNode);
		for (ProcessNode processNode : productSystemNode.getChildren()) {
			if (containing.get(processNode.getProcess().getId()) == null) {
				Node node = new Node();
				node.processId = processNode.getProcess().getId();
				build(productSystemNode.getProductSystem(), new Node[] { node });
				node.sort();
				nodes.add(node);
			}
		}
		int additionalHeight = 0;
		for (Node node : nodes) {
			int newAdditionalHeight = 0;
			locations.clear();
			applyLayout(node, 0, node.getLeftDepth());

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

			Map<Long, ProcessNode> processNodes = new HashMap<>();
			for (ProcessNode processNode : productSystemNode.getChildren())
				processNodes.put(processNode.getProcess().getId(), processNode);

			for (int x = minimumX; x <= maximumX; x++) {
				widths.put(x, 0);
				for (int y = minimumY; y <= maximumY; y++) {
					Long processId = locations.get(new Point(x, y));
					if (processId != null) {
						int width = Math.max(widths.get(x),
								processNodes.get(processId).getSize().width);
						widths.put(x, width);
					}
				}
			}
			for (int y = minimumY; y <= maximumY; y++) {
				heights.put(y, 0);
				for (int x = minimumX; x <= maximumX; x++) {
					Long processId = locations.get(new Point(x, y));
					if (processId != null) {
						int height = Math.max(heights.get(y),
								processNodes.get(processId).getSize().height);
						heights.put(y, height);
					}
				}
			}
			int xPosition = GraphLayoutManager.HORIZONTAL_SPACING;
			for (int x = minimumX; x <= maximumX; x++) {
				if (x > minimumX)
					if (widths.get(x - 1) > 0)
						xPosition += widths.get(x - 1)
								+ GraphLayoutManager.HORIZONTAL_SPACING;
				int yPosition = GraphLayoutManager.VERTICAL_SPACING;
				for (int y = minimumY; y <= maximumY; y++) {
					Long processId = locations.get(new Point(x, y));
					if (y > minimumY)
						if (heights.get(y - 1) > 0)
							yPosition += heights.get(y - 1)
									+ GraphLayoutManager.VERTICAL_SPACING;
					if (processId != null) {
						ProcessNode processNode = processNodes.get(processId);
						Dimension size = processNode.getSize();
						processNode.setXyLayoutConstraints(new Rectangle(
								xPosition, yPosition + additionalHeight,
								size.width, size.height));
						newAdditionalHeight = Math.max(newAdditionalHeight,
								yPosition + additionalHeight + size.height);
					}
				}
			}
			additionalHeight = newAdditionalHeight
					+ GraphLayoutManager.VERTICAL_SPACING;
		}
		containing.clear();
		widths.clear();
		heights.clear();
		locations.clear();
	}

	private class Node {

		long processId;
		List<Node> leftChildren = new ArrayList<>();
		List<Node> rightChildren = new ArrayList<>();

		int getLeftDepth() {
			int depth = 0;
			if (leftChildren.size() > 0) {
				depth = 1;
				int depthAdd = 0;
				for (int i = 0; i < leftChildren.size(); i++)
					depthAdd = Math.max(depthAdd, leftChildren.get(i)
							.getLeftDepth());
				depth += depthAdd;
			}
			return depth;
		}

		int getSize() {
			int size = 0;
			if (rightChildren.size() == 0 && leftChildren.size() == 0)
				size = 1;
			else {
				for (int i = 0; i < rightChildren.size(); i++)
					size += rightChildren.get(i).getSize();
				for (int i = 0; i < leftChildren.size(); i++)
					size += leftChildren.get(i).getSize();
			}
			return size;
		}

		void sort() {
			List<Node> temp = new ArrayList<>();
			temp.addAll(leftChildren);
			Collections.sort(temp, new Comparator<Node>() {

				@Override
				public int compare(Node o1, Node o2) {
					return Integer.compare(o2.getSize(), o1.getSize());
				}

			});
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
