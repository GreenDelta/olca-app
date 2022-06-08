package org.openlca.app.editors.graphical_legacy.model;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

/**
 * A class that handles appropriate object creation (the {@link EditPart}s)
 * depending on what is to be obtained, no matter what is the object class.
 */
public class GraphEditPartFactory implements EditPartFactory {

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
