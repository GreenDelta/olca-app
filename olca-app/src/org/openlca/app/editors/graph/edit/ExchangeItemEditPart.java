package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.IFigure;
import org.openlca.app.editors.graph.figures.ExchangeItemFigure;
import org.openlca.app.editors.graph.model.ExchangeItem;

import java.beans.PropertyChangeEvent;

public class ExchangeItemEditPart extends AbstractNodeEditPart<ExchangeItem> {

	@Override
	protected IFigure createFigure() {
		return new ExchangeItemFigure(getModel().getExchange());
	}

	@Override
	protected void createEditPolicies() {

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {

	}

}
