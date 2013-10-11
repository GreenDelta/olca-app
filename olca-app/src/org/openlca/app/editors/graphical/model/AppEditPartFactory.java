package org.openlca.app.editors.graphical.model;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

public class AppEditPartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		AbstractGraphicalEditPart part = null;
		if (model instanceof ProductSystemNode)
			part = new ProductSystemPart();
		else if (model instanceof ProcessNode)
			part = new ProcessPart();
		else if (model instanceof ExchangeNode)
			part = new ExchangePart();
		else if (model instanceof InputOutputNode)
			part = new InputOutputPart();
		else if (model instanceof ConnectionLink)
			part = new ConnectionLinkPart();

		if (part != null)
			part.setModel(model);
		return part;
	}

}
