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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.core.editors.productsystem.graphical.LayoutPolicy;
import org.openlca.core.editors.productsystem.graphical.actions.ProcessDeleteCommand;
import org.openlca.core.editors.productsystem.graphical.actions.XYLayoutCommand;

/**
 * EditPart of a {@link ProcessNode}
 * 
 * @see AppAbstractEditPart
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessPart extends AppAbstractEditPart {

	@Override
	protected void addChildVisual(final EditPart childEditPart, final int index) {
		super.addChildVisual(childEditPart, getContentPane().getChildren()
				.size());
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
		});
	}

	@Override
	protected IFigure createFigure() {
		final ProcessFigure figure = new ProcessFigure((ProcessNode) getModel());
		final ProcessNode pNode = (ProcessNode) getModel();
		pNode.setFigure(figure);
		pNode.addPropertyChangeListener(figure);
		figure.addPropertyChangeListener(this);
		return figure;
	}

	@Override
	public Command getCommand(final Request request) {
		if (request instanceof GroupRequest && request.getType() == REQ_DELETE) {
			// Creates a ProcessDeleteCommand on delete request
			final ProcessDeleteCommand command = new ProcessDeleteCommand();
			command.setProcessNode((ProcessNode) getModel());
			command.setProductSystemNode((ProductSystemNode) getParent()
					.getModel());
			return command;
		} else if (request instanceof ChangeBoundsRequest) {
			// Creates a LayoutCommand on ChangeBoundsRequest
			final ChangeBoundsRequest req = (ChangeBoundsRequest) request;
			Command commandChain = null;
			for (final Object o : req.getEditParts()) {
				if (o instanceof ProcessPart) {
					final ProcessPart part = (ProcessPart) o;
					final XYLayoutCommand command = new XYLayoutCommand();
					command.setProcessNode((ProcessNode) part.getModel());

					final Rectangle bounds = ((ProcessNode) part.getModel())
							.getFigure().getBounds().getCopy();
					((ProcessNode) part.getModel()).getFigure()
							.translateToAbsolute(bounds);
					final Rectangle moveResize = new Rectangle(
							req.getMoveDelta(), req.getSizeDelta());
					bounds.resize(moveResize.getSize());
					bounds.translate(moveResize.getLocation());
					((ProcessNode) part.getModel()).getFigure()
							.translateToRelative(bounds);
					if (req.getSizeDelta().height != 0) {
						command.setResizeHeight(true);
					}
					if (req.getMoveDelta().x != 0 || req.getMoveDelta().y != 0) {
						command.setMove(true);
					}
					command.setConstraint(bounds);
					if (commandChain == null) {
						commandChain = command;
					} else {
						commandChain = commandChain.chain(command);
					}
				}
			}
			return commandChain;
		}
		return super.getCommand(request);
	}

	/**
	 * Gets the edit parts which belong to the exchanges
	 * 
	 * @return The exchange part children of the process part
	 */
	public ExchangePart[] getExchangeParts() {
		final List<ExchangePart> parts = new ArrayList<>();
		for (final Object o : getChildren()) {
			for (final Object o2 : ((ExchangeContainerPart) o).getChildren()) {
				if (o2 instanceof ExchangePart) {
					parts.add((ExchangePart) o2);
				}
			}
		}
		final ExchangePart[] result = new ExchangePart[parts.size()];
		parts.toArray(result);
		return result;
	}

	@Override
	public List<Node> getModelChildren() {
		return ((ProcessNode) getModel()).getChildrenArray();
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("SELECT")) {
			if (evt.getNewValue().toString().equals("true")) {
				setSelected(EditPart.SELECTED);
			} else {
				setSelected(EditPart.SELECTED_NONE);
			}
		}
		((GraphicalEditPart) getViewer().getContents()).getFigure()
				.revalidate();
	}

	@Override
	public void setSelected(final int value) {
		if (getFigure().isVisible()) {
			super.setSelected(value);
			for (final ExchangePart o : getExchangeParts()) {
				final ExchangeNode node = (ExchangeNode) o.getModel();
				for (final ConnectionLink link : node.getLinks()) {
					if (link.getFigure().isVisible()) {
						link.setHighlight(value);
					}
				}
			}
		}
	}

}
