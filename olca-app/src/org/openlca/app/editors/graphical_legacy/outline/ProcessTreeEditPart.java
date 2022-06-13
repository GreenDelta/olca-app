package org.openlca.app.editors.graphical_legacy.outline;

import org.eclipse.gef.editparts.AbstractTreeEditPart;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.RootDescriptor;

public class ProcessTreeEditPart extends AbstractTreeEditPart {

	private final ProductSystemNode node;

	public ProcessTreeEditPart(ProductSystemNode node) {
		this.node = node;
	}

	@Override
	public RootDescriptor getModel() {
		return (RootDescriptor) super.getModel();
	}

	@Override
	protected String getText() {
		return Labels.name(getModel());
	}

	@Override
	public void setSelected(int value) {
		super.setSelected(value);
		for (ProcessNode node : this.node.getChildren()) {
			if (node.process.id == getModel().id) {
				node.select();
				node.reveal();
				break;
			}
		}
	}

}
