package org.openlca.app.results.analysis.sankey.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.results.analysis.sankey.model.Node;
import org.openlca.app.results.analysis.sankey.model.ProcessFigure;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.Sankey;

public class TreeLayout {

	/**
	 * set of process keys that have been added to a node already (important in case
	 * of loops, so no process is added twice)
	 */
	private final Set<ProcessProduct> containing = new HashSet<>();

	/**
	 * XY location in grid -> process key
	 */
	private final Map<Point, ProcessProduct> locations = new HashMap<>();

	/**
	 * The processes painted as process nodes
	 */
	private final Set<ProcessProduct> paintedProcesses = new HashSet<>();

	public void layout(ProductSystemNode systemNode) {
		prepare(systemNode);
		var editor = systemNode.editor;
		var sankey = systemNode.editor.sankey;
		if (sankey.root == null)
			return;

		// TODO: make sure that a node for the root is
		// always added, also when there are no links

		var nodeMap = new HashMap<ProcessProduct, TreeNode>();
		Function<Sankey.Node, TreeNode> mapFn = sankeyNode -> {
			return nodeMap.computeIfAbsent(sankeyNode.product, product -> {
				var processNode = editor.createdNodes.get(product);
				processNode.setXyLayoutConstraints(new Rectangle(
						0,
						0,
						processNode.figure.getSize().width,
						processNode.figure.getSize().height));
				return processNode == null
						? null
						: new TreeNode(processNode);
			});
		};
		var root = mapFn.apply(sankey.root);
		if (root == null)
			return;

		var expanded = new HashSet<ProcessProduct>();
		sankey.traverse(n -> {
			if (expanded.contains(n.product))
				return;
			expanded.add(n.product);
			var tree = mapFn.apply(n);
			if (tree == null)
				return;
			for (var provider : n.providers) {
				if (expanded.contains(provider.product))
					continue;
				var left = mapFn.apply(provider);
				if (left == null)
					continue;
				tree.leftChildren.add(left);
			}
		});

		var nodes = new ArrayList<>(nodeMap.values());
		nodes.forEach(TreeNode::sort);

		/*
		 * for (Object o : systemNode.children) { if (!(o instanceof ProcessNode))
		 * continue; var processNode = (ProcessNode) o; if
		 * (containing.contains(processNode.product)) continue; TreeNode node = new
		 * TreeNode(processNode); build(systemNode.productSystem, new TreeNode[]{node});
		 * node.sort(); nodes.add(node); }
		 * 
		 */

		int additionalHeight = 0;
		for (TreeNode node : nodes) {
			int newAdditionalHeight = 0;
			locations.clear();
			applyLayout(node, 0, node.getLeftDepth(), root.getLeftDepth());

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

			Map<ProcessProduct, ProcessFigure> figures = new HashMap<>();
			for (Object n : systemNode.children) {
				if (n instanceof ProcessNode) {
					ProcessFigure figure = ((ProcessNode) n).figure;
					figures.put(figure.node.product, figure);
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
					var product = locations.get(new Point(x, y));
					if (y > minimumY) {
						yPosition += ProcessFigure.HEIGHT
								+ GraphLayoutManager.verticalSpacing;
					}
					if (product != null) {
						ProcessFigure figure = figures.get(product);
						if (figure != null) {
							figure.node.setXyLayoutConstraints(
									new Rectangle(xPosition, yPosition
											+ additionalHeight,
											figure.getSize().width,
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

	private void applyLayout(
			TreeNode node,
			int addition,
			int actualDepth,
			int maximalDepth) {

		int y = maximalDepth - actualDepth + 3;
		final int x = node.getSize() / 2 + addition;
		while (locations.get(new Point(x, y)) != null) {
			y++;
			addition++;
		}
		locations.put(new Point(x, y), node.processNode.product);
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

	/*
	 * private TreeNode build(final ProductSystem productSystem) { final TreeNode
	 * node = new TreeNode(); node.processId = productSystem.referenceProcess.id;
	 * build(productSystem, new TreeNode[]{node}); return node; }
	 */

	private void build(ProductSystem system, TreeNode[] nodes) {
		// TODO: remove dead code
		/*
		 * List<TreeNode> childs = new ArrayList<>();
		 * 
		 * for (TreeNode node : nodes) { node.processNode. long processId =
		 * node.processId; for (ProcessLink link : linkSearchMap.getLinks(processId)) {
		 * if (link.processId == processId) { if (!containing.contains(link.providerId)
		 * && paintedProcesses.contains(link.providerId)) { TreeNode child = new
		 * TreeNode(); child.processId = link.providerId; node.leftChildren.add(child);
		 * containing.add(child.processId); childs.add(child); } } } }
		 * 
		 * if (childs.size() > 0) { build(system, childs.toArray(new TreeNode[0])); }
		 * childs.clear(); for (TreeNode node : nodes) { long providerId =
		 * node.processId; for (ProcessLink link : linkSearchMap.getLinks(providerId)) {
		 * if (link.providerId != providerId) continue; if
		 * (!containing.contains(link.processId) &&
		 * paintedProcesses.contains(link.processId)) { TreeNode child = new TreeNode();
		 * child.processId = link.processId; node.rightChildren.add(child);
		 * containing.add(child.processId); childs.add(child); } } }
		 * 
		 * if (childs.size() > 0) { build(system, childs.toArray(new
		 * TreeNode[childs.size()])); }
		 * 
		 */
	}

	private void prepare(ProductSystemNode productSystemNode) {
		for (Node node : productSystemNode.children) {
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

		final List<TreeNode> leftChildren = new ArrayList<>();
		final List<TreeNode> rightChildren = new ArrayList<>();

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
			if (rightChildren.size() == 0 && leftChildren.size() == 0)
				return 1;

			int size = 0;
			for (var rightChild : rightChildren) {
				size += rightChild.getSize();
			}
			for (var leftChild : leftChildren) {
				size += leftChild.getSize();
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
