package org.openlca.app.results.analysis.sankey.layouts;

import org.eclipse.gef.GraphicalViewer;
import org.openlca.app.results.analysis.sankey.edit.DiagramEditPart;
import org.openlca.app.results.analysis.sankey.edit.SankeyNodeEditPart;
import org.openlca.app.results.analysis.sankey.figures.SankeyNodeFigure;
import org.openlca.app.results.analysis.sankey.model.Diagram;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.layouts.GraphLayout;
import org.openlca.app.tools.graphics.model.Component;

/**
 * A layout for {@link org.eclipse.draw2d.FreeformFigure FreeformFigures} of
 * Graph.
 * This layout intends to lay out the SankeyNodeFigure in a tree fashion if the
 * model location is not DEFAULT_LOCATION. If the location is not default, it
 * means that the figure has been moved by the user. The figure is thus laid out as a
 * classical XYLayout figure.
 */
public class SankeyLayout extends GraphLayout {

	private final Diagram diagram;

	public SankeyLayout(DiagramEditPart diagramEditPart, int inputDirection) {
		super(inputDirection);
		this.diagram = diagramEditPart.getModel();
	}

	@Override
	public ComponentFigure figureOf(Component node) {
		var viewer = (GraphicalViewer) diagram.editor.getAdapter(
				GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();
		if (registry.get(node) instanceof SankeyNodeEditPart nodeEditPart)
			return nodeEditPart.getFigure();
		else
			return null;
	}

	@Override
	public Component getReferenceNode() {
		return diagram.getNode(diagram.editor.getSankey().root);
	}

	@Override
	public ComponentFigure getReferenceFigure() {
		for (var child : getParentFigure().getChildren()) {
			if (child instanceof SankeyNodeFigure figure) {
				if (figure.getComponent() == getReferenceNode())
					return figure;
			}
		}
		return null;
	}

	@Override
	protected void focusOnStart() {
		var editor = diagram.editor;
		if (!editor.wasFocus) {
			editor.wasFocus = editor.focusOnReferenceNode();
		}
	}

}
