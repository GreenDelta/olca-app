/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.model;

import java.util.EventObject;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.openlca.app.editors.graphical.layout.GraphAnimation;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.app.editors.graphical.policy.LayoutPolicy;

class ProductSystemPart extends AppAbstractEditPart<ProductSystemNode> {

	CommandStackListener stackListener = new CommandStackChangedListener();

	@Override
	public void activate() {
		super.activate();
		getViewer().getEditDomain().getCommandStack()
				.addCommandStackListener(stackListener);
	}

	@Override
	public void deactivate() {
		getViewer().getEditDomain().getCommandStack()
				.removeCommandStackListener(stackListener);
		super.deactivate();
	}

	@Override
	protected IFigure createFigure() {
		ProductSystemFigure figure = new ProductSystemFigure(getModel());
		getModel().setEditPart(this);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
		});
		GraphLayoutManager manager = new GraphLayoutManager(getModel());
		getFigure().setLayoutManager(manager);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProcessPart> getChildren() {
		return super.getChildren();
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	private class CommandStackChangedListener implements CommandStackListener {

		@Override
		public void commandStackChanged(EventObject event) {
			if (!GraphAnimation.captureLayout(getFigure()))
				return;
			while (GraphAnimation.step())
				getFigure().getUpdateManager().performUpdate();
			GraphAnimation.end();
		}
	}

}
