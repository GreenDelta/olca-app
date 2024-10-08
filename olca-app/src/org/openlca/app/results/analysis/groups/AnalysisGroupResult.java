package org.openlca.app.results.analysis.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.app.results.ResultEditor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.UpstreamNode;
import org.openlca.core.results.UpstreamTree;

record AnalysisGroupResult(
		ImpactDescriptor impact,
		Map<String, Double> values
) {

	static List<AnalysisGroupResult> calculate(
			ResultEditor editor, List<AnalysisGroup> groups
	) {
		return new Calc(editor, groups).compute();
	}

	private record Calc(
			ResultEditor editor, List<AnalysisGroup> groups
	) {

		List<AnalysisGroupResult> compute() {
			if (editor == null
					|| !editor.items().hasImpacts()
					|| groups == null
					|| groups.isEmpty())
				return Collections.emptyList();

			var results = new ArrayList<AnalysisGroupResult>();
			var provider = editor.result().provider();
			for (var impact : editor.items().impacts()) {
				var map = new HashMap<String, Double>();
				var tree = UpstreamTree.of(provider, impact);
				traverse(tree, tree.root, map, 0);
				results.add(new AnalysisGroupResult(impact, map));
			}
			return results;
		}

		private void traverse(
				UpstreamTree tree,
				UpstreamNode node,
				Map<String, Double> map,
				int depth
		) {
			var group = groupOf(node);
			if (group != null) {
				map.compute(group,
						($, sum) -> sum != null
								? sum + node.result()
								: node.result());
				return;
			}
			if (depth < 50) {
				for (var c : tree.childs(node)) {
					traverse(tree, c, map, depth + 1);
				}
			}
		}

		private String groupOf(UpstreamNode node) {
			if (node == null || node.provider() == null)
				return null;
			long id = node.provider().providerId();
			for (var g : groups) {
				if (g.processes().contains(id))
					return g.name();
			}
			return null;
		}
	}
}

