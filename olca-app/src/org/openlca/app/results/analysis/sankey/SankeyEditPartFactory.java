package org.openlca.app.results.analysis.sankey;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.results.analysis.sankey.model.ConnectionLink;
import org.openlca.app.results.analysis.sankey.model.ConnectionLinkEditPart;
import org.openlca.app.results.analysis.sankey.model.ProcessEditPart;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemEditPart;
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
			return new ProductSystemEditPart();
		if (model instanceof ProcessNode)
			return new ProcessEditPart();
		if (model instanceof ConnectionLink)
			return new ConnectionLinkEditPart();
		return null;
	}
}
