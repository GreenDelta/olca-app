package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.IFigure;
import org.openlca.app.editors.graphical.view.MaximizedNodeFigure;
import org.openlca.app.util.Labels;

public class MaximizedNodeEditPart extends ProcessPart {

	@Override
	protected IFigure createFigure() {
		ProcessNode node = getModel();
		System.out.println("Creating a maximised " + Labels.name(node.process) + "node.");
		var figure = new MaximizedNodeFigure(node);
		node.figure = figure;
		return figure;
	}
}
