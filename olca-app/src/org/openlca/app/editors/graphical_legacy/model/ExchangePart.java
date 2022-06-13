package org.openlca.app.editors.graphical_legacy.model;

import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.ConnectionDragCreationTool;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical_legacy.policy.CreateLinkPolicy;
import org.openlca.app.editors.graphical_legacy.view.ExchangeFigure;

/**
 * Corresponding {@link org.eclipse.gef.EditPart} of {@link ExchangeNode}.
 */
class ExchangePart extends AbstractNodeEditPart<ExchangeNode> {

	@Override
	protected IFigure createFigure() {
		var node = getModel();
		var figure = new ExchangeFigure(node);
		node.figure = figure;
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(
				EditPolicy.NODE_ROLE,
				new CreateLinkPolicy(getModel().editor));
	}

	@Override
	public DragTracker getDragTracker(Request req) {
		if (req instanceof SelectionRequest) {
			var sel = (SelectionRequest) req;
			if (sel.getLastButtonPressed() == 3)
				return super.getDragTracker(req);
		}
		return new ConnectionDragCreationTool();
	}

	@Override
	public IOPart getParent() {
		return (IOPart) super.getParent();
	}

	@Override
	protected void refreshVisuals() {
		var node = getModel();
		if (!(node.figure instanceof ExchangeFigure))
			return;
		var figure = (ExchangeFigure) node.figure;
		var layout = new GridData(
				SWT.LEFT, SWT.TOP, true, false);
		figure.getParent().setConstraint(figure, layout);
	}

	@Override
	public boolean isSelectable() {
		return true;
	}
}
