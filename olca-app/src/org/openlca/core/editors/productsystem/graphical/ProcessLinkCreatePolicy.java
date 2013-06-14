/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.core.editors.productsystem.graphical.actions.ProcessLinkCreateCommand;
import org.openlca.core.editors.productsystem.graphical.actions.ProcessLinkReconnectCommand;
import org.openlca.core.editors.productsystem.graphical.model.ConnectionLink;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.ExchangePart;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;

/**
 * Implementation of {@link GraphicalNodeEditPolicy}
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessLinkCreatePolicy extends GraphicalNodeEditPolicy {

	/**
	 * The created connection
	 */
	private PolylineConnection connection;

	/**
	 * The actual selected {@link ExchangeNode}
	 */
	private ExchangeNode selectedExchangeNode;

	@Override
	protected Connection createDummyConnection(final Request req) {
		connection = (PolylineConnection) super.createDummyConnection(req);
		connection.setForegroundColor(ConnectionLink.COLOR);
		if (selectedExchangeNode.getExchange().isAvoidedProduct()) {
			connection.setLineStyle(Graphics.LINE_DASH);
		} else {
			if (selectedExchangeNode.getExchange().isInput()) {
				connection.setSourceDecoration(new PolygonDecoration());
			} else {
				connection.setTargetDecoration(new PolygonDecoration());
			}
		}
		return connection;
	}

	@Override
	protected Command getConnectionCompleteCommand(
			final CreateConnectionRequest arg0) {
		if (arg0.getStartCommand() != null) {
			// get start command
			final ProcessLinkCreateCommand cmd = (ProcessLinkCreateCommand) arg0
					.getStartCommand();
			cmd.setTargetNode(null);
			// if exchange node
			if (arg0.getTargetEditPart().getModel() instanceof ExchangeNode) {
				// target node
				final ExchangeNode targetNode = (ExchangeNode) arg0
						.getTargetEditPart().getModel();
				// node represanting the recipient input
				final ExchangeNode recipientInput = targetNode.getExchange()
						.isInput() ? targetNode : cmd.getSourceNode();

				boolean connectionExists = false;
				if (((ProductSystemNode) targetNode.getParentProcessNode()
						.getParent()).hasConnection(recipientInput)) {
					connectionExists = true;
				}
				if (!connectionExists) {
					cmd.setTargetNode(targetNode);
					arg0.setStartCommand(cmd);
					return cmd;
				}
			}
		}
		return null;
	}

	@Override
	protected Command getConnectionCreateCommand(
			final CreateConnectionRequest arg0) {
		ProcessLinkCreateCommand cmd = null;
		final ExchangeNode sourceNode = (ExchangeNode) arg0.getTargetEditPart()
				.getModel();
		// if source node is output and the product is not already linked
		if (!sourceNode.getExchange().isInput()
				|| !((ProductSystemNode) sourceNode.getParentProcessNode()
						.getParent()).hasConnection(sourceNode)) {
			cmd = new ProcessLinkCreateCommand();
			selectedExchangeNode = sourceNode;
			cmd.setSourceNode(sourceNode);
			arg0.setStartCommand(cmd);
		}
		return cmd;
	}

	@Override
	protected ConnectionRouter getDummyConnectionRouter(
			final CreateConnectionRequest request) {
		return ConnectionRouter.NULL;
	}

	@Override
	protected Command getReconnectSourceCommand(final ReconnectRequest arg0) {
		if (arg0.getTarget() instanceof ExchangePart) {
			// get old link
			final ConnectionLink oldLink = (ConnectionLink) arg0
					.getConnectionEditPart().getModel();
			// get target node
			final ExchangeNode targetNode = oldLink.getTargetNode();
			// get source node
			final ExchangeNode sourceNode = (ExchangeNode) arg0.getTarget()
					.getModel();
			// create reconnect command
			final ProcessLinkReconnectCommand cmd = new ProcessLinkReconnectCommand();
			cmd.setOldLink(oldLink);
			cmd.setSourceNode(sourceNode);
			cmd.setTargetNode(targetNode);
			return cmd;
		}
		return null;
	}

	@Override
	protected Command getReconnectTargetCommand(final ReconnectRequest arg0) {
		if (arg0.getTarget() instanceof ExchangePart) {
			// old link
			final ConnectionLink oldLink = (ConnectionLink) arg0
					.getConnectionEditPart().getModel();
			// source node
			final ExchangeNode sourceNode = oldLink.getSourceNode();
			// target node
			final ExchangeNode targetNode = (ExchangeNode) arg0.getTarget()
					.getModel();
			boolean canConnect = true;

			final ExchangeNode recipientInput = targetNode.getExchange()
					.isInput() ? targetNode : sourceNode;

			// if there already is a connection which is not the actual
			if (!oldLink.getTargetNode().getExchange().getId()
					.equals(targetNode.getExchange().getId())
					&& ((ProductSystemNode) targetNode.getParentProcessNode()
							.getParent()).hasConnection(recipientInput)) {
				canConnect = false;
			}
			if (canConnect) {
				// create reconnect command
				final ProcessLinkReconnectCommand cmd = new ProcessLinkReconnectCommand();
				cmd.setOldLink(oldLink);
				cmd.setSourceNode(sourceNode);
				cmd.setTargetNode(targetNode);
				return cmd;
			}
		}
		return null;
	}

	@Override
	public void eraseSourceFeedback(final Request request) {
		if (getHost() instanceof ExchangePart) {
			// unhighlight matching exchanges
			((ProductSystemNode) ((ExchangeNode) getHost().getModel())
					.getParentProcessNode().getParent())
					.unhighlightMatchingExchangeLabels();
			((ExchangeNode) getHost().getModel()).getFigure().setHighlight(
					false);
		}
		super.eraseSourceFeedback(request);
	}

	@Override
	public void showSourceFeedback(final Request request) {
		if (getHost() instanceof ExchangePart) {
			// highlight matching exchanges
			((ProductSystemNode) ((ExchangeNode) getHost().getModel())
					.getParentProcessNode().getParent())
					.highlightMatchingExchangeLabels(selectedExchangeNode);
			((ExchangeNode) getHost().getModel()).getFigure()
					.setHighlight(true);
		}
		super.showSourceFeedback(request);
	}

}
