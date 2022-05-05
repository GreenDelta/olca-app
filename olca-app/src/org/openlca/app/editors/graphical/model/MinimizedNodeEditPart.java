package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.IFigure;
import org.openlca.app.editors.graphical.view.MinimizedNodeFigure;
import org.openlca.app.util.Labels;

public class MinimizedNodeEditPart extends ProcessPart {

	@Override
	protected IFigure createFigure() {
		ProcessNode node = getModel();
		System.out.println("Creating a minimized " + Labels.name(node.process) + "node.");
		var figure = new MinimizedNodeFigure(node);
		node.figure = figure;
		return figure;
	}
}
