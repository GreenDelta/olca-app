package org.openlca.app.tools.graphics.actions;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.BasicGraphicalEditor;

public class FocusAction extends WorkbenchPartAction {

	private final BasicGraphicalEditor editor;
	private GraphicalViewer viewer;

	public FocusAction(BasicGraphicalEditor part) {
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
			&& editor.getModel().getFocusComponent() != null;
	}

	@Override
	public void run() {
		var viewport = ((FigureCanvas) viewer.getControl()).getViewport();

		var viewportLocation = viewport.getViewLocation().getCopy();
		// The target location of the center of the top of the focused component.
		var targetInViewport =
			new Point(
				viewport.getBounds().width / 2,
				viewport.getBounds().height / 3);
		var focusInViewport = getFocusInViewport();

		var translation = focusInViewport.getTranslated(targetInViewport
			.getNegated());

		viewport.setViewLocation(viewportLocation.translate(translation));
		}

	// Return the location of the center of the top of the focused component.
	private Point getFocusInViewport() {
		var focus = editor.getModel().getFocusComponent();
		var focusEditPart = (AbstractGraphicalEditPart) viewer.getEditPartRegistry()
			.get(focus);
		var parentEditPart = (AbstractGraphicalEditPart) viewer
				.getEditPartRegistry().get(editor.getModel());

		var focusLocation = focusEditPart.getFigure().getBounds().getTop()
			.getCopy();

		var figure = parentEditPart.getFigure().getParent();
		figure.translateToParent(focusLocation);
		figure.translateToAbsolute(focusLocation);
		return focusLocation;
	}

}
