/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.command;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class LayoutCommand extends Command {

	private ProductSystemNode model;
	private GraphLayoutManager layoutManager;
	private GraphLayoutType type;
	private Map<IFigure, Rectangle> oldConstraints = new HashMap<>();

	LayoutCommand() {

	}

	@Override
	public boolean canExecute() {
		if (type == null)
			return false;
		if (layoutManager == null)
			return false;
		if (model == null)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		for (Node node : this.model.getChildren())
			if (node.getFigure().isVisible())
				oldConstraints.put(node.getFigure(), node.getFigure()
						.getBounds().getCopy());
		layoutManager.layout(model.getFigure(), type);
	}

	@Override
	public String getLabel() {
		return Messages.Systems_LayoutCommand_LayoutText
				+ type.getDisplayName();
	}

	@Override
	public void redo() {
		layoutManager.layout(model.getFigure(), type);
	}

	@Override
	public void undo() {
		for (ProcessNode node : model.getChildren())
			if (oldConstraints.get(node.getFigure()) != null)
				node.setXyLayoutConstraints(oldConstraints.get(node.getFigure()));
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

	void setLayoutManager(GraphLayoutManager layoutManager) {
		this.layoutManager = layoutManager;
	}

	void setLayoutType(GraphLayoutType type) {
		this.type = type;
	}

}
