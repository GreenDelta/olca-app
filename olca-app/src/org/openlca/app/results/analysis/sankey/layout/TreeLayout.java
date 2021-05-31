package org.openlca.app.results.analysis.sankey.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.results.analysis.sankey.model.ProcessFigure;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.results.Sankey;

public class TreeLayout {

	/**
	 * XY location in grid -> process key
	 */
	private final Map<Point, TechFlow> locations = new HashMap<>();

	public void layout(ProductSystemNode systemNode) {
		prepare(systemNode);
		var editor = systemNode.editor;
		var sankey = systemNode.editor.sankey;
		if (sankey.root == null)
			return;

		var nodeMap = new HashMap<TechFlow, TreeNode>();
		Function<Sankey.Node, TreeNode> mapFn = sankeyNode -> {
			return nodeMap.computeIfAbsent(sankeyNode.product, product -> {
				var processNode = editor.createdNodes.get(product);
				return processNode == null
						? null
						: new TreeNode(processNode);
			});
		};
		var root = mapFn.apply(sankey.root);
		if (root == null)
			return;

		var expanded = new HashSet<TechFlow>();
		var added = new HashSet<TechFlow>();
		sankey.traverse(n -> {
			if (expanded.contains(n.product))
				return;
			expanded.add(n.product);
			var tree = mapFn.apply(n);
			if (tree == null)
				return;
			for (var provider : n.providers) {
				if (added.contains(provider.product))
					continue;
				added.add(provider.product);
				var left = mapFn.apply(provider);
				if (left == null)
					continue;
				tree.childs.add(left);
			}
		});

		var nodes = new ArrayList<>(nodeMap.values());
		nodes.forEach(TreeNode::sort);

		int additionalHeight = 0;
		for (TreeNode node : nodes) {
			int newAdditionalHeight = 0;
			locations.clear();
			applyLayout(node, 0, node.depth(), root.depth());

			int minimumX = Integer.MAX_VALUE;
			int maximumX = Integer.MIN_VALUE;
			int minimumY = Integer.MAX_VALUE;
			int maximumY = Integer.MIN_VALUE;

			for (Point p : locations.keySet()) {
				minimumX = Math.min(minimumX, p.x);
				maximumX = Math.max(maximumX, p.x);
				minimumY = Math.min(minimumY, p.y);
				maximumY = Math.max(maximumY, p.y);
			}

			Map<TechFlow, ProcessFigure> figures = new HashMap<>();
			for (var n : systemNode.processNodes) {
				ProcessFigure figure = n.figure;
				figures.put(figure.node.product, figure);
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
							figure.node.setLayoutConstraints(
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
		for (int i = 0; i < node.childs.size(); i++) {
			TreeNode child = node.childs.get(i);
			int sizeAddition = 0;
			for (int j = 0; j < i; j++) {
				sizeAddition += node.childs.get(j).getSize();
			}
			applyLayout(child, addition + sizeAddition, actualDepth - 1,
					maximalDepth);
		}
	}

	private void prepare(ProductSystemNode productSystemNode) {
		for (var node : productSystemNode.processNodes) {
			node.setLayoutConstraints(new Rectangle(
					0,
					0,
					node.figure.getSize().width,
					node.figure.getSize().height));
		}
	}

	static class TreeNode {

		final ProcessNode processNode;
		final List<TreeNode> childs = new ArrayList<>();

		TreeNode(ProcessNode processNode) {
			this.processNode = processNode;
		}

		int depth() {
			int depth = 0;
			if (childs.size() > 0) {
				depth = 1;
				int depthAdd = 0;
				for (var leftChild : childs) {
					depthAdd = Math.max(depthAdd, leftChild.depth());
				}
				depth += depthAdd;
			}
			return depth;
		}

		int getSize() {
			if (childs.size() == 0)
				return 1;
			int size = 0;
			for (var leftChild : childs) {
				size += leftChild.getSize();
			}
			return size;
		}

		void sort() {
			var temp = new ArrayList<>(childs);
			temp.sort((n1, n2) -> Integer.compare(n2.getSize(), n1.getSize()));
			childs.clear();
			int count = 0;
			int i = 0;
			while (count < temp.size()) {
				childs.add(temp.get(i));
				count++;
				if (count < temp.size()) {
					childs.add(temp.get(temp.size() - i - 1));
					count++;
				}
				i++;
			}
		}

	}

}
