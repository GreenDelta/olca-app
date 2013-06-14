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
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.core.editors.productsystem.graphical.TreeConnectionRouter;
import org.openlca.core.editors.productsystem.graphical.actions.ProcessLinkDeleteCommand;

/**
 * EditPart for {@link ConnectionLink}.
 * 
 * @see AbstractConnectionEditPart
 * 
 * @author Sebastian Greve
 * 
 */
public class ConnectionLinkEditPart extends AbstractConnectionEditPart
		implements PropertyChangeListener {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
		});
	}

	@Override
	protected IFigure createFigure() {
		final ConnectionLink model = (ConnectionLink) getModel();
		final ProductSystemNode productSystemNode = (ProductSystemNode) model
				.getSourceNode().getParentProcessNode().getParent();

		// create connection figure
		final PolylineConnection conn = new PolylineConnection() {

			@Override
			public void setVisible(final boolean visible) {
				firePropertyChange("SELECT", true, false);
				super.setVisible(visible);
			}

		};
		conn.setForegroundColor(ConnectionLink.COLOR);
		conn.setConnectionRouter(productSystemNode.getEditor().isRoute() ? new TreeConnectionRouter()
				: ConnectionRouter.NULL);

		// set target decoration
		final boolean avoided = model.getTargetNode().getExchange()
				.isAvoidedProduct();
		if (!avoided) {
			conn.setTargetDecoration(new PolygonDecoration());
		}
		if (!model.getSourceNode().getParentProcessNode().getFigure()
				.isVisible()
				|| !model.getTargetNode().getParentProcessNode().getFigure()
						.isVisible()) {
			conn.setVisible(false);
		}
		if (avoided) {
			conn.setLineStyle(Graphics.LINE_DASH);
		}
		model.setFigure(conn);
		conn.addPropertyChangeListener(this);
		return conn;
	}

	@Override
	public void activate() {
		((ConnectionLink) getModel()).addPropertyChangeListener(this);
		super.activate();
	}

	@Override
	public void deactivate() {
		((ConnectionLink) getModel()).removePropertyChangeListener(this);
		super.deactivate();
	}

	@Override
	public void eraseSourceFeedback(final Request request) {
		((ProductSystemNode) ((ExchangeNode) getSource().getModel())
				.getParentProcessNode().getParent())
				.unhighlightMatchingExchangeLabels();
		((ExchangeNode) getSource().getModel()).getFigure().setHighlight(false);
		((ExchangeNode) getTarget().getModel()).getFigure().setHighlight(false);
		super.eraseSourceFeedback(request);
	}

	@Override
	public Command getCommand(final Request request) {
		if (request instanceof GroupRequest && request.getType() == REQ_DELETE) {
			return new ProcessLinkDeleteCommand((ConnectionLink) getModel());
		}
		return super.getCommand(request);
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(ConnectionLink.REFRESH_SOURCE_ANCHOR)) {
			refreshSourceAnchor();
		} else if (evt.getPropertyName().equals(
				ConnectionLink.REFRESH_TARGET_ANCHOR)) {
			refreshTargetAnchor();
		} else if (evt.getPropertyName().equals(ConnectionLink.HIGHLIGHT)) {
			setSelected((Integer) evt.getNewValue());
		} else if (evt.getPropertyName().equals("SELECT")) {
			if (evt.getNewValue().toString().equals("false")) {
				setSelected(EditPart.SELECTED_NONE);
			}
		}

	}

	@Override
	public void setSelected(final int value) {
		if (getFigure().isVisible()) {
			if (value != EditPart.SELECTED_NONE) {
				((PolylineConnection) getFigure()).setLineWidth(2);
				((PolylineConnection) getFigure())
						.setForegroundColor(ConnectionLink.HIGHTLIGHT_COLOR);
			} else {
				((PolylineConnection) getFigure()).setLineWidth(1);
				((PolylineConnection) getFigure())
						.setForegroundColor(ConnectionLink.COLOR);
			}
			super.setSelected(value);
		}
	}

	@Override
	public void showSourceFeedback(final Request request) {
		if (request instanceof ReconnectRequest) {
			final ConnectionLink link = (ConnectionLink) ((ReconnectRequest) request)
					.getConnectionEditPart().getModel();
			final ExchangeNode node = ((ReconnectRequest) request)
					.isMovingStartAnchor() ? link.getTargetNode() : link
					.getSourceNode();
			final ExchangeNode node2 = ((ReconnectRequest) request)
					.isMovingStartAnchor() ? link.getSourceNode() : link
					.getTargetNode();
			((ProductSystemNode) node.getParentProcessNode().getParent())
					.highlightMatchingExchangeLabels(node);
			node2.getFigure().setHighlight(true);
			node.getFigure().setHighlight(true);
		}
		super.showSourceFeedback(request);
	}

}
