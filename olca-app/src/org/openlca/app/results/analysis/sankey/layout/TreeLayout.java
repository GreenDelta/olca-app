package org.openlca.app.results.analysis.sankey.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.results.analysis.sankey.model.ProcessFigure;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.openlca.core.matrix.ProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class TreeLayout {

	/**
	 * set of process keys that have been added to a node already (important in
	 * case of loops, so no process is added twice)
	 */
	private final Set<Long> containing = new HashSet<>();

	/**
	 * XY location in grid -> process key
	 */
	private final Map<Point, Long> locations = new HashMap<>();

	/**
	 * The processes painted as process nodes
	 */
	private final Set<Long> paintedProcesses = new HashSet<>();

	private ProcessLinkSearchMap linkSearchMap;

	public void layout(final ProductSystemNode productSystemNode) {
		linkSearchMap = productSystemNode.getEditor().getLinkSearchMap();
		prepare(productSystemNode);
		final List<Node> nodes = new ArrayList<>();
		final Node mainNode = build(productSystemNode.getProductSystem());
		mainNode.sort();
		nodes.add(mainNode);
		for (final Object o : productSystemNode.getChildrenArray()) {
			if (o instanceof ProcessNode) {
				final ProcessNode processNode = (ProcessNode) o;
				if (!containing.contains(processNode.getProcess().getId())) {
					final Node node = new Node();
					node.processKey = processNode.getProcess().getId();
					build(productSystemNode.getProductSystem(),
							new Node[] { node });
					node.sort();
					nodes.add(node);
				}
			}
		}
		int additionalHeight = 0;
		for (final Node node : nodes) {
			int newAdditionalHeight = 0;
			locations.clear();
			applyLayout(node, 0, node.getLeftDepth(), mainNode.getLeftDepth());

			int minimumX = Integer.MAX_VALUE;
			int maximumX = Integer.MIN_VALUE;
			int minimumY = Integer.MAX_VALUE;
			int maximumY = Integer.MIN_VALUE;

			for (final Point p : locations.keySet()) {
				if (p.x < minimumX) {
					minimumX = p.x;
				}
				if (p.x > maximumX) {
					maximumX = p.x;
				}
				if (p.y < minimumY) {
					minimumY = p.y;
				}
				if (p.y > maximumY) {
					maximumY = p.y;
				}
			}
			final Map<Long, ProcessFigure> figures = new HashMap<>();
			for (final Object n : productSystemNode.getChildrenArray()) {
				if (n instanceof ProcessNode) {
					final ProcessFigure figure = ((ProcessNode) n).getFigure();
					figures.put(figure.getProcessNode().getProcess().getId(),
							figure);
				}
			}

			// apply layout
			int xPosition = GraphLayoutManager.horizontalSpacing;
			for (int x = minimumX; x <= maximumX; x++) {
				if (x > minimumX) {
					xPosition += ProcessFigure.WIDTH
							+ GraphLayoutManager.horizontalSpacing;
				}
				int yPosition = GraphLayoutManager.verticalSpacing;
				for (int y = minimumY; y <= maximumY; y++) {
					final Long processKey = locations.get(new Point(x, y));
					if (y > minimumY) {
						yPosition += ProcessFigure.HEIGHT
								+ GraphLayoutManager.verticalSpacing;
					}
					if (processKey != null) {
						final ProcessFigure figure = figures.get(processKey);
						if (figure != null) {
							figure.getProcessNode().setXyLayoutConstraints(
									new Rectangle(xPosition, yPosition
											+ additionalHeight, figure
											.getSize().width,
											figure.getSize().height));
							newAdditionalHeight = Math.max(
									newAdditionalHeight,
									yPosition + additionalHeight
											+ figure.getSize().height);
						}
					}
				}
			}
			additionalHeight = newAdditionalHeight
					+ GraphLayoutManager.verticalSpacing;
		}
		containing.clear();
		locations.clear();
	}

	private void applyLayout(final Node node, int addition,
			final int actualDepth, final int maximalDepth) {
		int y = maximalDepth - actualDepth + 3;
		final int x = node.getSize() / 2 + addition;
		while (locations.get(new Point(x, y)) != null) {
			y++;
			addition++;
		}
		locations.put(new Point(x, y), node.processKey);
		for (int i = 0; i < node.leftChildren.size(); i++) {
			final Node child = node.leftChildren.get(i);
			int sizeAddition = 0;
			for (int j = 0; j < i; j++) {
				sizeAddition += node.leftChildren.get(j).getSize();
			}
			applyLayout(child, addition + sizeAddition, actualDepth - 1,
					maximalDepth);
		}
		for (int i = 0; i < node.rightChildren.size(); i++) {
			final Node child = node.rightChildren.get(i);
			int sizeAddition = 0;
			for (int a = 0; a < node.leftChildren.size(); a++) {
				sizeAddition += node.leftChildren.get(a).getSize();
			}
			for (int j = 0; j < i; j++) {
				sizeAddition += node.rightChildren.get(j).getSize();
			}
			applyLayout(child, addition + sizeAddition, actualDepth + 1,
					maximalDepth);
		}
	}

	private Node build(final ProductSystem productSystem) {
		final Node node = new Node();
		node.processKey = productSystem.getReferenceProcess().getId();
		build(productSystem, new Node[] { node });
		return node;
	}

	private void build(final ProductSystem productSystem, final Node[] nodes) {
		List<Node> children = new ArrayList<>();
		for (Node node : nodes) {
			long processKey = node.processKey;
			for (ProcessLink link : linkSearchMap.getLinks(processKey)) {
				if (link.getRecipientId() == processKey) {
					if (!containing.contains(link.getProviderId())
							&& paintedProcesses.contains(link.getProviderId())) {
						Node child = new Node();
						child.processKey = link.getProviderId();
						node.leftChildren.add(child);
						containing.add(child.processKey);
						children.add(child);
					}
				}
			}
		}
		if (children.size() > 0) {
			build(productSystem, children.toArray(new Node[children.size()]));
		}
		children.clear();
		for (Node node : nodes) {
			long processKey = node.processKey;
			for (ProcessLink link : linkSearchMap.getLinks(processKey)) {
				if (link.getProviderId() != processKey)
					continue;
				if (!containing.contains(link.getRecipientId())
						&& paintedProcesses.contains(link.getRecipientId())) {
					Node child = new Node();
					child.processKey = link.getRecipientId();
					node.rightChildren.add(child);
					containing.add(child.processKey);
					children.add(child);
				}
			}
		}
		if (children.size() > 0) {
			build(productSystem, children.toArray(new Node[children.size()]));
		}
	}

	private void prepare(final ProductSystemNode productSystemNode) {
		for (final Object node : productSystemNode.getChildrenArray()) {
			if (node instanceof ProcessNode) {
				final ProcessNode processNode = (ProcessNode) node;
				paintedProcesses.add(processNode.getProcess().getId());
				processNode.setXyLayoutConstraints(new Rectangle(0, 0,
						processNode.getFigure().getSize().width, processNode
								.getFigure().getSize().height));
			}
		}
	}

	class Node {

		List<Node> leftChildren = new ArrayList<>();
		long processKey;
		List<Node> rightChildren = new ArrayList<>();

		int getLeftDepth() {
			int depth = 0;
			if (leftChildren.size() > 0) {
				depth = 1;
				int depthAdd = 0;
				for (int i = 0; i < leftChildren.size(); i++) {
					depthAdd = Math.max(depthAdd, leftChildren.get(i)
							.getLeftDepth());
				}
				depth += depthAdd;
			}
			return depth;
		}

		int getSize() {
			int size = 0;
			if (rightChildren.size() == 0 && leftChildren.size() == 0) {
				size = 1;
			} else {
				for (int i = 0; i < rightChildren.size(); i++) {
					size += rightChildren.get(i).getSize();
				}
				for (int i = 0; i < leftChildren.size(); i++) {
					size += leftChildren.get(i).getSize();
				}
			}
			return size;
		}

		void sort() {
			final List<Node> temp = new ArrayList<>();
			temp.addAll(leftChildren);
			Collections.sort(temp, new Comparator<Node>() {

				@Override
				public int compare(final Node o1, final Node o2) {
					return ((Integer) o2.getSize()).compareTo(o1.getSize());
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
