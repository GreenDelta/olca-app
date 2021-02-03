package org.openlca.app.editors.graphical.model;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

public class AppEditPartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		var part = editPartOf(model);
		if (part == null)
			return null;
		part.setModel(model);
		return part;
	}

	private EditPart editPartOf(Object model) {
		if (model instanceof ProductSystemNode)
			return new ProductSystemPart();
		if (model instanceof ProcessNode)
			return new ProcessPart();
		if (model instanceof ExchangeNode)
			return new ExchangePart();
		if (model instanceof IONode)
			return new IOPart();
		if (model instanceof Link)
			return new LinkPart();
		return null;
	}

}
