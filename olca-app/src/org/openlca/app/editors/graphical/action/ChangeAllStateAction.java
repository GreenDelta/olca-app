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

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.command.ChangeStateCommand;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.resources.ImageType;

public class ChangeAllStateAction extends Action {

	static final int MINIMIZE = 1;
	static final int MAXIMIZE = 2;

	ChangeAllStateAction(int type) {
		if (type == MINIMIZE) {
			setId(ActionIds.MINIMIZE_ALL_ACTION_ID);
			setText(Messages.Systems_MinimizeAllProcessesAction_Text);
			setImageDescriptor(ImageType.MINIMIZE_ICON.getDescriptor());
		} else if (type == MAXIMIZE) {
			setId(ActionIds.MAXIMIZE_ALL_ACTION_ID);
			setText(Messages.Systems_MaximizeAllProcessesAction_Text);
			setImageDescriptor(ImageType.MAXIMIZE_ICON.getDescriptor());
		}
		this.type = type;
	}

	private ProductSystemNode model;
	private int type;

	@Override
	public void run() {
		Command actualCommand = null;
		for (ProcessNode node : model.getChildren()) {
			boolean minimize = type == MINIMIZE;
			if (node.isMinimized() == minimize) {
				ChangeStateCommand newCommand = CommandFactory
						.createChangeStateCommand(node);
				if (actualCommand == null) {
					actualCommand = newCommand;
				} else {
					actualCommand = actualCommand.chain(newCommand);
				}
			}
		}
		if (actualCommand != null)
			model.getEditor().getCommandStack().execute(actualCommand);
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

}
