/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
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
			if (node.getProcess().getId() == getModel().getId()) {
				node.setSelected(value);
				break;
			}
		}
	}

}
