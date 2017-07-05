package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.tools.ConnectionDragCreationTool;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.policy.ProcessLinkCreatePolicy;

class ExchangePart extends AbstractNodeEditPart<ExchangeNode> {

	@Override
	protected IFigure createFigure() {
		ExchangeNode node = getModel();
		ExchangeFigure figure = new ExchangeFigure(node);
		node.figure = figure;
		String name = node.getName();
		figure.setText(name);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.NODE_ROLE, new ProcessLinkCreatePolicy());
	}

	@Override
	public DragTracker getDragTracker(Request request) {
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
		int hAlign = getModel().exchange.isInput ? SWT.LEFT : SWT.RIGHT;
		getFigure().getParent().setConstraint(getFigure(), new GridData(hAlign, SWT.TOP, true, false));
	}

}
