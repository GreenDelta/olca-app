package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.results.analysis.sankey.edit.DiagramEditPart;
import org.openlca.app.results.analysis.sankey.edit.SankeyNodeEditPart;

public class FocusAction extends WorkbenchPartAction {

	private final SankeyEditor editor;
	private GraphicalViewer viewer;

	public FocusAction(SankeyEditor part) {
		super(part);
		editor = part;
		setText(M.Focus);
		setId(ActionIds.FOCUS);
		setImageDescriptor(Icon.TARGET.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		if (editor == null)
			return false;
		viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		return viewer != null && editor.getModel() != null
				&& editor.getSankey() != null
				&& editor.getModel().getNode(editor.getSankey().root) != null;
	}

	@Override
	public void run() {
		var viewport = ((FigureCanvas) viewer.getControl()).getViewport();

		var viewportLocation = viewport.getViewLocation().getCopy();
		// The target location of the center of the top of the reference node.
		var targetInViewport =
			new Point(
				viewport.getBounds().width / 2,
				viewport.getBounds().height / 5);
		var refNodeInViewport = getRefNodeInViewport();

		var translation = refNodeInViewport.getTranslated(targetInViewport
			.getNegated());

		viewport.setViewLocation(viewportLocation.translate(translation));
		}

	// Return the location of the center of the top of the reference node.
	private Point getRefNodeInViewport() {
		var refNode = editor.getModel().getNode(editor.getSankey().root);
		var refNodeEditPart = (SankeyNodeEditPart) viewer.getEditPartRegistry()
			.get(refNode);
		var graphEditPart = (DiagramEditPart) viewer.getEditPartRegistry()
			.get(editor.getModel());

		var refNodeLocation = refNodeEditPart.getFigure().getBounds().getTop()
			.getCopy();

		var figure = graphEditPart.getFigure().getParent();
		figure.translateToParent(refNodeLocation);
		figure.translateToAbsolute(refNodeLocation);
		return refNodeLocation;
	}

}
