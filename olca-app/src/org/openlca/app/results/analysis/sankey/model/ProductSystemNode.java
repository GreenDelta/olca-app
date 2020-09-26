package org.openlca.app.results.analysis.sankey.model;

import org.eclipse.draw2d.ConnectionRouter;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.core.model.ProductSystem;

public class ProductSystemNode extends Node {

	public final ProductSystem productSystem;
	public final SankeyDiagram editor;
	public final double cutoff;
	final Object selection;

	public ProductSystemNode(
			ProductSystem productSystem,
			SankeyDiagram editor,
			Object selection,
			double cutoff) {
		this.productSystem = productSystem;
		this.editor = editor;
		this.selection = selection;
		this.cutoff = cutoff;
	}

	public void setRouted(boolean enabled) {
		var router = ConnectionRouter.NULL;
		if (enabled)
			router = LinkPart.ROUTER;
		for (Node node : children) {
			if (!(node instanceof ProcessNode))
				continue;
			var pNode = (ProcessNode) node;
			for (var link : pNode.links) {
				link.figure.setConnectionRouter(router);
			}
		}
	}
}
