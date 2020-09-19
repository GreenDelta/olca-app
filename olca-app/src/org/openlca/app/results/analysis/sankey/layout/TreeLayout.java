package org.openlca.app.results.analysis.sankey.layout;

import java.util.ArrayList;
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
import org.openlca.core.matrix.ProcessProduct;
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
	private final Set<ProcessProduct> paintedProcesses = new HashSet<>();

	private ProcessLinkSearchMap linkSearchMap;

	public void layout(ProductSystemNode productSystemNode) {
		linkSearchMap = productSystemNode.editor.linkSearchMap;
		prepare(productSystemNode);
		List<TreeNode> nodes = new ArrayList<>();
		TreeNode mainNode = build(productSystemNode.productSystem);
		mainNode.sort();
		nodes.add(mainNode);
		for (Object o : productSystemNode.children) {
			if (o instanceof ProcessNode) {
				ProcessNode processNode = (ProcessNode) o;
				if (!containing.contains(processNode.process.id)) {
					TreeNode node = new TreeNode();
					node.processId = processNode.process.id;
					build(productSystemNode.productSystem, new TreeNode[]{node});
					node.sort();
					nodes.add(node);
				}
			}
		}
		int additionalHeight = 0;
		for (final TreeNode node : nodes) {
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
			for (Object n : productSystemNode.children) {
				if (n instanceof ProcessNode) {
					ProcessFigure figure = ((ProcessNode) n).figure;
					figures.put(figure.node.process.id, figure);
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
							figure.node.setXyLayoutConstraints(
									new Rectangle(xPosition, yPosition
											+ additionalHeight,
											figure
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

	private void applyLayout(final TreeNode node, int addition,
							 final int actualDepth, final int maximalDepth) {
		int y = maximalDepth - actualDepth + 3;
		final int x = node.getSize() / 2 + addition;
		while (locations.get(new Point(x, y)) != null) {
			y++;
			addition++;
		}
		locations.put(new Point(x, y), node.processId);
		for (int i = 0; i < node.leftChildren.size(); i++) {
			final TreeNode child = node.leftChildren.get(i);
			int sizeAddition = 0;
			for (int j = 0; j < i; j++) {
				sizeAddition += node.leftChildren.get(j).getSize();
			}
			applyLayout(child, addition + sizeAddition, actualDepth - 1,
					maximalDepth);
		}
		for (int i = 0; i < node.rightChildren.size(); i++) {
			final TreeNode child = node.rightChildren.get(i);
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

	private TreeNode build(final ProductSystem productSystem) {
		final TreeNode node = new TreeNode();
		node.processId = productSystem.referenceProcess.id;
		build(productSystem, new TreeNode[]{node});
		return node;
	}

	private void build(ProductSystem system, TreeNode[] nodes) {

		List<TreeNode> childs = new ArrayList<>();

		for (TreeNode node : nodes) {
			node.processNode.
			long processId = node.processId;
			for (ProcessLink link : linkSearchMap.getLinks(processId)) {
				if (link.processId == processId) {
					if (!containing.contains(link.providerId)
							&& paintedProcesses.contains(link.providerId)) {
						TreeNode child = new TreeNode();
						child.processId = link.providerId;
						node.leftChildren.add(child);
						containing.add(child.processId);
						childs.add(child);
					}
				}
			}
		}

		if (childs.size() > 0) {
			build(system, childs.toArray(new TreeNode[0]));
		}
		childs.clear();
		for (TreeNode node : nodes) {
			long providerId = node.processId;
			for (ProcessLink link : linkSearchMap.getLinks(providerId)) {
				if (link.providerId != providerId)
					continue;
				if (!containing.contains(link.processId)
						&& paintedProcesses.contains(link.processId)) {
					TreeNode child = new TreeNode();
					child.processId = link.processId;
					node.rightChildren.add(child);
					containing.add(child.processId);
					childs.add(child);
				}
			}
		}

		if (childs.size() > 0) {
			build(system, childs.toArray(new TreeNode[childs.size()]));
		}
	}

	private void prepare(ProductSystemNode productSystemNode) {
		for (var node : productSystemNode.children) {
			if (node instanceof ProcessNode) {
				var processNode = (ProcessNode) node;
				paintedProcesses.add(processNode.product);
				processNode.setXyLayoutConstraints(new Rectangle(
						0,
						0,
						processNode.figure.getSize().width,
						processNode.figure.getSize().height));
			}
		}
	}

	static class TreeNode {

		final ProcessNode processNode;

		List<TreeNode> leftChildren = new ArrayList<>();
		List<TreeNode> rightChildren = new ArrayList<>();

		TreeNode(ProcessNode processNode) {
			this.processNode = processNode;
		}

		int getLeftDepth() {
			int depth = 0;
			if (leftChildren.size() > 0) {
				depth = 1;
				int depthAdd = 0;
				for (var leftChild : leftChildren) {
					depthAdd = Math.max(depthAdd, leftChild.getLeftDepth());
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
				for (var rightChild : rightChildren) {
					size += rightChild.getSize();
				}
				for (var leftChild : leftChildren) {
					size += leftChild.getSize();
				}
			}
			return size;
		}

		void sort() {
			var temp = new ArrayList<>(leftChildren);
			temp.sort((n1, n2) -> Integer.compare(n2.getSize(), n1.getSize()));
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
