package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.openlca.app.editors.graphical.figures.ExchangeFigure;

public class ExchangeSelectionEditPolicy extends SelectionEditPolicy {

	private ExchangeFigure getFigure() {
		var figure = ((ExchangeEditPart) getHost()).getFigure();
		return (ExchangeFigure) figure;
	}

	@Override
	protected void hideSelection() {
		getFigure().setSelected(false);

	}

	@Override
	protected void showPrimarySelection() {
		getFigure().setSelected(true);
	}

	@Override
	protected void showSelection() {
		getFigure().setSelected(false);
	}

}
