package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.ConnectionDragCreationTool;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.policy.ProcessLinkCreatePolicy;

class ExchangePart extends AbstractNodeEditPart<ExchangeNode> {

	@Override
	protected IFigure createFigure() {
		var node = getModel();
		var figure = new ExchangeFigure(node);
		node.figure = figure;
		figure.setText(node.getName());
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.NODE_ROLE, new ProcessLinkCreatePolicy());
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
		if (getModel().isDummy())
			return;
		int hAlign = getModel().exchange.isInput
				? SWT.LEFT
				: SWT.RIGHT;
		var layout = new GridData(hAlign, SWT.TOP, true, false);
		getFigure()
				.getParent()
				.setConstraint(getFigure(), layout);
	}

	@Override
	public boolean isSelectable() {
		return true;
	}
}
