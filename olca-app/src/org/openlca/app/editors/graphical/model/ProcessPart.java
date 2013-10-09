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
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.command.DeleteProcessCommand;
import org.openlca.app.editors.graphical.policy.LayoutPolicy;

class ProcessPart extends AbstractNodeEditPart<ProcessNode> {

	@Override
	protected void addChildVisual(EditPart childEditPart, int index) {
		super.addChildVisual(childEditPart, getContentPane().getChildren()
				.size());
	}

	@Override
	protected IFigure createFigure() {
		ProcessFigure figure = new ProcessFigure(getModel());
		ProcessNode pNode = getModel();
		pNode.setFigure(figure);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
		});
	}

	@Override
	public ProductSystemPart getParent() {
		return (ProductSystemPart) super.getParent();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<InputOutputPart> getChildren() {
		return super.getChildren();
	}

	@Override
	public Command getCommand(Request request) {
		if (request instanceof GroupRequest && request.getType() == REQ_DELETE) {
			DeleteProcessCommand command = CommandFactory
					.createDeleteProcessCommand(getModel());
			return command;
		} else if (request instanceof ChangeBoundsRequest) {
			ChangeBoundsRequest req = (ChangeBoundsRequest) request;
			Command commandChain = null;
			for (Object o : req.getEditParts()) {
				if (o instanceof ProcessPart) {
					ProcessPart part = (ProcessPart) o;

					Rectangle bounds = part.getModel().getFigure().getBounds()
							.getCopy();
					part.getModel().getFigure().translateToAbsolute(bounds);
					Rectangle moveResize = new Rectangle(req.getMoveDelta(),
							req.getSizeDelta());
					bounds.resize(moveResize.getSize());
					bounds.translate(moveResize.getLocation());
					part.getModel().getFigure().translateToRelative(bounds);
					Command command = null;
					if (req.getSizeDelta().height != 0
							|| req.getSizeDelta().width != 0)
						command = CommandFactory.createResizeCommand(
								part.getModel(), bounds);
					if (req.getMoveDelta().x != 0 || req.getMoveDelta().y != 0)
						command = CommandFactory.createMoveCommand(
								part.getModel(), bounds);

					if (commandChain == null)
						commandChain = command;
					else
						commandChain = commandChain.chain(command);
				}
			}
			return commandChain;
		}
		return super.getCommand(request);
	}

	@Override
	protected List<ConnectionLink> getModelSourceConnections() {
		return getModel().getLinks();
	}

	@Override
	protected List<ConnectionLink> getModelTargetConnections() {
		return getModel().getLinks();
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void setSelected(int value) {
		if (getFigure().isVisible()) {
			super.setSelected(value);
			for (ConnectionLink link : getModel().getLinks())
				if (link.isVisible())
					link.setSelected(value);
		}
	}

	@Override
	public void refreshSourceConnections() {
		super.refreshSourceConnections();
	}

	@Override
	public void refreshTargetConnections() {
		super.refreshTargetConnections();
	}

	void revalidate() {
		((GraphicalEditPart) getViewer().getContents()).getFigure()
				.revalidate();
	}

}
