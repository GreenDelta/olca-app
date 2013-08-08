/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.model;

import java.beans.PropertyChangeEvent;
import java.util.EventObject;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.openlca.core.editors.productsystem.graphical.GraphAnimation;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutManager;
import org.openlca.core.editors.productsystem.graphical.LayoutPolicy;

/**
 * EditPart of a {@link ProductSystemNode}
 * 
 * @see AppAbstractEditPart
 * 
 * @author Sebastian Greve
 * 
 */
public class ProductSystemPart extends AppAbstractEditPart {

	/**
	 * Listener for layout activities, calls the graphical animation class
	 */
	CommandStackListener stackListener = new CommandStackListener() {
		@Override
		public void commandStackChanged(final EventObject event) {
			if (!GraphAnimation.captureLayout(getFigure())) {
				return;
			}
			while (GraphAnimation.step()) {
				getFigure().getUpdateManager().performUpdate();
			}
			GraphAnimation.end();
		}
	};

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
		});
		final GraphLayoutManager manager = new GraphLayoutManager(this);
		getFigure().setLayoutManager(manager);
	}

	@Override
	protected IFigure createFigure() {
		final ProductSystemFigure figure = new ProductSystemFigure(
				(ProductSystemNode) getModel());
		figure.addPropertyChangeListener(((ProductSystemNode) getModel())
				.getEditor());
		((ProductSystemNode) getModel()).setPart(this);
		return figure;
	}

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
		((GraphLayoutManager) getFigure().getLayoutManager()).dispose();
		((ProductSystemFigure) getFigure()).dispose();
		super.deactivate();
	}

	@Override
	public List<Node> getModelChildren() {
		return ((ProductSystemNode) getModel()).getChildrenArray();
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(Node.PROPERTY_ADD)
				|| evt.getPropertyName().equals(Node.PROPERTY_REMOVE)) {
			refreshChildren();
		} else if (evt.getPropertyName().equals("SELECT")) {
			if (evt.getNewValue().toString().equals("true")) {
				setSelected(EditPart.SELECTED);
			} else {
				setSelected(EditPart.SELECTED_NONE);
			}
		}
	}

}
