package org.openlca.app.editors.graphical_legacy.outline;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class AppTreeEditPartFactory implements EditPartFactory {

	private final ProductSystemNode model;

	public AppTreeEditPartFactory(ProductSystemNode model) {
		this.model = model;
	}

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		EditPart part = null;
		if (model instanceof ProductSystem)
			part = new ProductSystemTreeEditPart();
		else if (model instanceof ProcessDescriptor)
			part = new ProcessTreeEditPart(this.model);
		if (part != null)
			part.setModel(model);
		return part;
	}
}
