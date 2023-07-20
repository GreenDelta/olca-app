package org.openlca.app.editors.graphical.layouts;

import java.util.Objects;

import org.eclipse.gef.GraphicalViewer;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.figures.NodeFigure;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.tools.graphics.BasicGraphicalEditor;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.layouts.GraphLayout;
import org.openlca.app.tools.graphics.model.Component;


public class Layout extends GraphLayout {

	public Layout(BasicGraphicalEditor editor, int orientation) {
		super(editor, orientation);
	}

	@Override
	public ComponentFigure figureOf(Component node) {
		var viewer = (GraphicalViewer) getEditor().getAdapter(
				GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();
		if (registry.get(node) instanceof NodeEditPart nodeEditPart)
			return nodeEditPart.getFigure();
		else
			return null;
	}

	@Override
	public Component getReferenceNode() {
		return getGraph().getReferenceNode();
	}

	private Graph getGraph() {
		return ((Graph) getEditor().getModel());
	}

	@Override
	public ComponentFigure getReferenceFigure() {
		for (var child : getParentFigure().getChildren()) {
			if (child instanceof NodeFigure figure) {
				if (Objects.equals(figure.node, getReferenceNode()))
					return figure;
			}
		}
		return null;
	}

}
