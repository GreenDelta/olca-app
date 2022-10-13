package org.openlca.app.editors.graphical.layouts;

import org.eclipse.gef.GraphicalViewer;
import org.openlca.app.editors.graphical.edit.GraphEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.figures.NodeFigure;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.layouts.GraphLayout;
import org.openlca.app.tools.graphics.model.Component;

public class Layout extends GraphLayout {

	private final Graph graph;

	public Layout(GraphEditPart graphEditPart, int orientation) {
		super(orientation);
		this.graph = graphEditPart.getModel();
	}

	@Override
	public ComponentFigure figureOf(Component node) {
		var viewer = (GraphicalViewer) graph.editor.getAdapter(
				GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();
		if (registry.get(node) instanceof NodeEditPart nodeEditPart)
			return nodeEditPart.getFigure();
		else
			return null;
	}

	@Override
	public Component getReferenceNode() {
		return graph.getReferenceNode();
	}

	@Override
	public ComponentFigure getReferenceFigure() {
		for (var child : getParentFigure().getChildren()) {
			if (child instanceof NodeFigure figure) {
				if (figure.node == graph.getReferenceNode())
					return figure;
			}
		}
		return null;
	}

	@Override
	protected void focusOnStart() {
		var editor = graph.editor;
		if (!editor.wasFocus) {
			editor.wasFocus = editor.focusOnReferenceNode();
		}
	}

}
