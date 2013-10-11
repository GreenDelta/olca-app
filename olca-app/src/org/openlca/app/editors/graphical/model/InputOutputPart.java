package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.SWT;

class InputOutputPart extends AppAbstractEditPart<InputOutputNode> {

	@Override
	protected IFigure createFigure() {
		return new InputOutputFigure();
	}

	@Override
	protected void createEditPolicies() {

	}

	@Override
	public ProcessPart getParent() {
		return (ProcessPart) super.getParent();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ExchangePart> getChildren() {
		return super.getChildren();
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	@Override
	protected void refreshVisuals() {
		getFigure().getParent().setConstraint(getFigure(),
				new GridData(SWT.FILL, SWT.FILL, true, true));
	}

}
