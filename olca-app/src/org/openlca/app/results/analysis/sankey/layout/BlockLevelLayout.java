package org.openlca.app.results.analysis.sankey.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.results.Sankey;

public class BlockLevelLayout {

	public void applyOn(ProductSystemNode systemNode) {
		var editor = systemNode.editor;
		var sankey = systemNode.editor.sankey;
		if (sankey.root == null)
			return;
		var nodeMap = new HashMap<TechFlow, Tree>();
		Function<Sankey.Node, Tree> mapFn = sankeyNode ->
			nodeMap.computeIfAbsent(sankeyNode.product, product -> {
				var processNode = editor.createdNodes.get(product);
				return processNode == null
					? null
					: new Tree(processNode);
			});

		var root = mapFn.apply(sankey.root);
		if (root == null)
			return;
		var expanded = new HashSet<TechFlow>();
		var added = new HashSet<TechFlow>();
		added.add(sankey.root.product);
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
				var child = mapFn.apply(provider);
				if (child == null)
					continue;
				tree.childs.add(child);
			}
		});

		var levels = root.levels();

		// calculate the width of the graph
		var nodeSize = root.node.figure.getSize();
		var hspace = GraphLayoutManager.horizontalSpacing;
		int totalWidth = 800;
		for (var level : levels) {
			int n = level.size();
			int width = n * nodeSize.width + (n - 1) * hspace;
			totalWidth = Math.max(totalWidth, width);
		}

		// center and draw the levels as blocks
		int vspace = GraphLayoutManager.verticalSpacing;
		for (int i = 0; i < levels.length; i++) {
			var level = levels[i];
			var n = level.size();
			int width = n * nodeSize.width + (n - 1) * hspace;
			int y = vspace + i * (vspace + nodeSize.height);
			int x = totalWidth / 2 - width / 2;
			for (var node : level) {
				node.node.setLayoutConstraints(new Rectangle(
					x, y, nodeSize.width, nodeSize.height));
				x += nodeSize.width + hspace;
			}
		}
	}

	private static class Tree {

		private final ProcessNode node;
		private final List<Tree> childs = new ArrayList<>();

		Tree(ProcessNode node) {
			this.node = node;
		}

		@SuppressWarnings("unchecked")
		List<Tree>[] levels() {
			if (childs.size() == 0) {
				var level = new List[1];
				level[0] = List.of(this);
				return level;
			}

			var childLevels = childs.stream()
				.map(Tree::levels)
				.toList();
			int len = 1;
			for (var cl : childLevels) {
				len = Math.max(len, cl.length + 1);
			}

			var levels = new List[len];
			levels[0] = List.of(this);
			for (var cl : childLevels) {
				for (int i = 0; i < cl.length; i++) {
					int pos = i + 1;
					if (levels[pos] == null) {
						levels[pos] = cl[i];
					} else {
						levels[pos] = (List<Tree>) Stream
							.concat(levels[pos].stream(), cl[i].stream())
							.collect(Collectors.toList());
					}
				}
			}
			return levels;
		}
	}

}
