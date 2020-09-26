package org.openlca.app.results.analysis.sankey.model;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

public class SankeyEditPartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		EditPart part = createEditPart(model);
		if (part != null)
			part.setModel(model);
		return part;
	}

	private EditPart createEditPart(Object model) {
		if (model instanceof ProductSystemNode)
			return new ProductSystemPart();
		if (model instanceof ProcessNode)
			return new ProcessPart();
		if (model instanceof Link)
			return new LinkPart();
		return null;
	}
}
