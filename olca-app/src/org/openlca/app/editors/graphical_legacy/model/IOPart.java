package org.openlca.app.editors.graphical_legacy.model;

import java.util.List;

import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical_legacy.view.IOFigure;

class IOPart extends AppAbstractEditPart<IONode> {

	@Override
	protected IFigure createFigure() {
		var process = getModel().parent();
		return new IOFigure(process);
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
		getFigure().getParent().setConstraint(
				getFigure(), new GridData(SWT.FILL, SWT.FILL, true, true));
	}

}
