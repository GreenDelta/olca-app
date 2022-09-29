package org.openlca.app.results.analysis.sankey.model;

import org.openlca.app.App;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.core.results.Sankey;

public class SankeyFactory {

	private final SankeyEditor editor;

	public SankeyFactory(SankeyEditor editor) {
		this.editor = editor;
	}

	public Diagram createDiagram() {
		var config = editor.config;
		if (config.selection() == null
				|| config.cutoff() < 0d
				|| config.cutoff() > 1d
				|| config.maxCount() < 0)
			return new Diagram(editor, config.orientation());

		var diagram = new Diagram(editor, config.orientation());
		App.runWithProgress("Calculate sankey results",
				() -> editor.setSankey(Sankey.of(config.selection(), editor.result)
						.withMinimumShare(config.cutoff())
						.withMaximumNodeCount(config.maxCount())
						.build()),
				() -> {
					if (editor.getSankey() == null)
						return;

					// create the nodes
					editor.getSankey().traverse(n -> {
						var node = new SankeyNode(n, editor.getSankey(), editor.result);
						diagram.addChild(node);
					});

					// create the links
					editor.getSankey().traverse(n -> {
						var target = diagram.getNode(n);
						if (target == null)
							return;
						for (var provider : n.providers) {
							var source = diagram.getNode(provider);
							if (source == null)
								continue;
							var linkShare = editor.getSankey().getLinkShare(provider, n);
							var share = linkShare * provider.share;
							new SankeyLink(source, target, share);
						}
					});
				});

		return diagram;
	}

}
