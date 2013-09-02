/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.command.LayoutCommand;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.resources.ImageType;

public class LayoutAction extends Action {

	private ProductSystemNode model;
	private GraphLayoutType layoutType;

	LayoutAction(GraphLayoutType layoutType) {
		setText(NLS.bind(Messages.Systems_LayoutAction_Text,
				layoutType.getDisplayName()));
		switch (layoutType) {
		case TREE_LAYOUT:
			setId(ActionIds.LAYOUT_TREE_ACTION_ID);
			setImageDescriptor(ImageType.TREE_LAYOUT_ICON.getDescriptor());
		case MINIMAL_TREE_LAYOUT:
			setId(ActionIds.LAYOUT_MINIMAL_TREE_ACTION_ID);
			setImageDescriptor(ImageType.MINIMAL_TREE_LAYOUT_ICON
					.getDescriptor());
		}
		this.layoutType = layoutType;
	}

	@Override
	public void run() {
		LayoutCommand command = CommandFactory.createLayoutCommand(model,
				(GraphLayoutManager) model.getFigure().getLayoutManager(),
				layoutType);
		model.getEditor().getCommandStack().execute(command);
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

}
