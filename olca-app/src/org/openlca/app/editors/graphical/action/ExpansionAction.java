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
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.resources.ImageType;

public class ExpansionAction extends Action {

	static final int EXPAND = 1;
	static final int COLLAPSE = 2;

	private int type;

	ExpansionAction(int type) {
		if (type == EXPAND) {
			setId(ActionIds.EXPAND_ALL_ACTION_ID);
			setText(Messages.Systems_ExpandAllAction_Text);
			setImageDescriptor(ImageType.EXPAND_ICON.getDescriptor());
		} else if (type == COLLAPSE) {
			setId(ActionIds.COLLAPSE_ALL_ACTION_ID);
			setText(Messages.Systems_CollapseAllAction_Text);
			setImageDescriptor(ImageType.COLLAPSE_ICON.getDescriptor());
		}
		this.type = type;
	}

	private ProductSystemNode model;

	@Override
	public void run() {
		// TODO adjust
		// if (type == EXPAND)
		// model.getEditor().reset(true);
		// else if (type == COLLAPSE)
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

}
