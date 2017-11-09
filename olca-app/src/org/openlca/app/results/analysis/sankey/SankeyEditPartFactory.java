package org.openlca.app.results.analysis.sankey;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.results.analysis.sankey.model.Link;
import org.openlca.app.results.analysis.sankey.model.LinkPart;
import org.openlca.app.results.analysis.sankey.model.ProcessPart;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemPart;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;

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
