package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.tools.ConnectionDragCreationTool;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.policy.LinkPolicy;

class ExchangePart extends AbstractNodeEditPart<ExchangeNode> {

	@Override
	protected IFigure createFigure() {
		ExchangeFigure figure = new ExchangeFigure(getModel());
		getModel().setFigure(figure);
		String name = getModel().getName();
		figure.setText(name);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.NODE_ROLE, new LinkPolicy());
	}

	@Override
	public DragTracker getDragTracker(Request request) {
		return new ConnectionDragCreationTool();
	}

	@Override
	public InputOutputPart getParent() {
		return (InputOutputPart) super.getParent();
	}

	@Override
	protected void refreshVisuals() {
		if (getModel().isDummy())
			return;
		if (getModel().getExchange().isInput())
			getFigure().getParent().setConstraint(getFigure(),
					new GridData(SWT.LEFT, SWT.TOP, true, false));
		else
			getFigure().getParent().setConstraint(getFigure(),
					new GridData(SWT.RIGHT, SWT.TOP, true, false));
	}

}
