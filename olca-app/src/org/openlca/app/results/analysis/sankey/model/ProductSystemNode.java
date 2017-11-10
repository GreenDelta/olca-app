package org.openlca.app.results.analysis.sankey.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.ConnectionRouter;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.core.model.ProductSystem;

public class ProductSystemNode extends Node implements PropertyChangeListener {

	public final ProductSystem productSystem;
	public final double cutoff;
	public final SankeyDiagram editor;
	final Object selection;

	public ProductSystemNode(ProductSystem productSystem, SankeyDiagram editor, Object selection, double cutoff) {
		this.productSystem = productSystem;
		this.editor = editor;
		this.selection = selection;
		this.cutoff = cutoff;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		listeners.firePropertyChange(evt);
	}

	public void setRouted(boolean enabled) {
		ConnectionRouter router = ConnectionRouter.NULL;
		if (enabled)
			router = LinkPart.ROUTER;
		for (Node node : children) {
			if (!(node instanceof ProcessNode))
				continue;
			ProcessNode pNode = (ProcessNode) node;
			for (Link link : pNode.links) {
				link.figure.setConnectionRouter(router);
			}
		}
	}

}
