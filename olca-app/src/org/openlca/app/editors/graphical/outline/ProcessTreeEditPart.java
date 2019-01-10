package org.openlca.app.editors.graphical.outline;

import org.eclipse.gef.editparts.AbstractTreeEditPart;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProcessTreeEditPart extends AbstractTreeEditPart {

	private ProductSystemNode node;

	public ProcessTreeEditPart(ProductSystemNode node) {
		this.node = node;
	}

	@Override
	public ProcessDescriptor getModel() {
		return (ProcessDescriptor) super.getModel();
	}

	@Override
	protected String getText() {
		return Labels.getDisplayName(getModel());
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
