package org.openlca.app.results.analysis.sankey.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ConnectionRouter;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.core.model.RootEntity;

public class ProductSystemNode {

	public final RootEntity calculationTarget;
	public final SankeyDiagram editor;
	public final List<ProcessNode> processNodes = new ArrayList<>();

	public ProductSystemNode(
			RootEntity calculationTarget, SankeyDiagram editor) {
		this.calculationTarget = calculationTarget;
		this.editor = editor;
	}

	public void setRouted(boolean enabled) {
		var router = ConnectionRouter.NULL;
		if (enabled)
			router = LinkPart.ROUTER;
		for (var node : processNodes) {
			var pNode = node;
			for (var link : pNode.links) {
				link.figure.setConnectionRouter(router);
			}
		}
	}
}
