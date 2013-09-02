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
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.outline.ProcessTreeEditPart;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class HideShowAction extends Action {

	final static int SHOW = 1;
	final static int HIDE = 2;

	private ProductSystemNode model;
	private TreeViewer viewer;
	private int type;

	HideShowAction(int type) {
		if (type == SHOW) {
			setId(ActionIds.SHOW_ACTION_ID);
			setText(Messages.Systems_HideShowAction_ShowText);
		} else if (type == HIDE) {
			setId(ActionIds.HIDE_ACTION_ID);
			setText(Messages.Systems_HideShowAction_HideText);
		}
		this.type = type;
	}

	@Override
	public void run() {
		if (viewer.getSelection().isEmpty())
			return;
		Command command = null;
		for (Object o : ((StructuredSelection) viewer.getSelection()).toArray()) {
			if (o instanceof ProcessTreeEditPart) {
				ProcessTreeEditPart part = (ProcessTreeEditPart) o;
				if (command == null)
					command = createCommand(part.getModel());
				else
					command = command.chain(createCommand(part.getModel()));
			}
		}
		if (command != null)
			model.getEditor().getCommandStack().execute(command);
	}

	private Command createCommand(ProcessDescriptor process) {
		if (type == SHOW)
			return CommandFactory.createShowCommand(process, model);
		if (type == HIDE)
			return CommandFactory.createHideCommand(process, model);
		return null;
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

	void setViewer(TreeViewer viewer) {
		this.viewer = viewer;
	}

}
