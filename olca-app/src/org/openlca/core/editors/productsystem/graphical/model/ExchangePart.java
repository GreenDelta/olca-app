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
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.eclipse.gef.tools.ConnectionDragCreationTool;
import org.eclipse.swt.SWT;
import org.openlca.core.editors.productsystem.graphical.ProcessLinkCreatePolicy;
import org.openlca.core.editors.productsystem.graphical.actions.ProcessLinkCreateCommand;

/**
 * EditPart of an {@link ExchangeNode}.
 * 
 * @see AppAbstractEditPart
 * 
 * @author Sebastian Greve
 * 
 */
public class ExchangePart extends AppAbstractEditPart implements NodeEditPart {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.NODE_ROLE, new ProcessLinkCreatePolicy());
	}

	@Override
	protected IFigure createFigure() {
		final ExchangeNode exchangeNode = (ExchangeNode) getModel();
		final ExchangeFigure figure = new ExchangeFigure(exchangeNode);
		exchangeNode.setFigure(figure);
		final String name = exchangeNode.getExchange().getFlow().getName();
		figure.setText(name);
		figure.addPropertyChangeListener(this);
		return figure;
	}

	@Override
	protected List<ConnectionLink> getModelSourceConnections() {
		return ((ExchangeNode) getModel()).getLinks();
	}

	@Override
	protected List<ConnectionLink> getModelTargetConnections() {
		return ((ExchangeNode) getModel()).getLinks();
	}

	@Override
	protected void refreshVisuals() {
		final ExchangeFigure figure = (ExchangeFigure) getFigure();
		final ExchangeNode exchangeNode = (ExchangeNode) getModel();
		if (exchangeNode.getExchange().isInput()) {
			figure.setLayout(new GridData(SWT.LEFT, SWT.TOP, true, false));
		}
		if (!exchangeNode.getExchange().isInput()) {
			figure.setLayout(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		}
	}

	@Override
	public DragTracker getDragTracker(final Request request) {
		return new ConnectionDragCreationTool();
	}

	/**
	 * Gets the process part to which this exchange part belongs
	 * 
	 * @return The parent process part
	 */
	public ProcessPart getParentProcessPart() {
		return (ProcessPart) getParent().getParent();
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(
			final ConnectionEditPart connection) {
		final ConnectionLink link = (ConnectionLink) connection.getModel();
		ConnectionAnchor anchor = null;
		if (link.getSourceNode().getParentProcessNode().isMinimized()) {
			anchor = new ExchangeAnchor(link.getSourceNode()
					.getParentProcessNode().getFigure(), true, false);
		} else {
			anchor = new ExchangeAnchor(link.getSourceNode().getFigure(),
					false, false);
		}
		return anchor;
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(final Request request) {
		ConnectionAnchor anchor = null;
		if (request instanceof CreateConnectionRequest) {
			final ProcessLinkCreateCommand cmd = (ProcessLinkCreateCommand) ((CreateConnectionRequest) request)
					.getStartCommand();
			if (cmd.getSourceNode().getExchange().isInput()) {
				anchor = new ExchangeAnchor(cmd.getSourceNode().getFigure(),
						false, true);
			} else {
				anchor = new ExchangeAnchor(cmd.getSourceNode().getFigure(),
						false, false);
			}
		} else {
			final ReconnectRequest req = (ReconnectRequest) request;
			final ConnectionLink link = (ConnectionLink) req
					.getConnectionEditPart().getModel();
			final ExchangePart sourcePart = (ExchangePart) req.getTarget();
			final ExchangeNode sourceNode = (ExchangeNode) sourcePart
					.getModel();
			final ExchangeFigure sourceFigure = (ExchangeFigure) sourcePart
					.getFigure();
			if (link.getTargetNode().matches(sourceNode)) {
				if (sourceNode.getExchange().isInput()) {
					anchor = new ExchangeAnchor(sourceFigure, false, true);
				} else {
					anchor = new ExchangeAnchor(sourceFigure, false, false);
				}
			}
		}
		return anchor;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(
			final ConnectionEditPart connection) {
		final ConnectionLink link = (ConnectionLink) connection.getModel();
		ConnectionAnchor anchor = null;
		if (link.getTargetNode().getParentProcessNode().isMinimized()) {
			anchor = new ExchangeAnchor(link.getTargetNode()
					.getParentProcessNode().getFigure(), true, true);
		} else {
			anchor = new ExchangeAnchor(link.getTargetNode().getFigure(),
					false, true);
		}
		return anchor;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(final Request request) {
		ConnectionAnchor anchor = null;
		if (request instanceof CreateConnectionRequest) {
			final ProcessLinkCreateCommand cmd = (ProcessLinkCreateCommand) ((CreateConnectionRequest) request)
					.getStartCommand();
			if (cmd.getTargetNode() != null) {
				if (cmd.getSourceNode().matches(cmd.getTargetNode())) {
					if (cmd.getTargetNode().getExchange().isInput()) {
						anchor = new ExchangeAnchor(cmd.getTargetNode()
								.getFigure(), false, true);
					} else {
						anchor = new ExchangeAnchor(cmd.getTargetNode()
								.getFigure(), false, false);
					}
				}
			}
		} else {
			final ReconnectRequest req = (ReconnectRequest) request;
			final ConnectionLink link = (ConnectionLink) req
					.getConnectionEditPart().getModel();
			final ExchangePart targetPart = (ExchangePart) req.getTarget();
			final ExchangeNode targetNode = (ExchangeNode) targetPart
					.getModel();
			final ExchangeFigure targetFigure = (ExchangeFigure) targetPart
					.getFigure();
			if (link.getSourceNode().matches(targetNode)) {
				boolean canConnect = !((ProductSystemNode) targetNode
						.getParentProcessNode().getParent())
						.hasConnection(targetNode);
				if (link.getTargetNode().getExchange().getId()
						.equals(targetNode.getExchange().getId())) {
					canConnect = true;
				}
				if (canConnect) {
					if (targetNode.getExchange().isInput()) {
						anchor = new ExchangeAnchor(targetFigure, false, true);
					} else {
						anchor = new ExchangeAnchor(targetFigure, false, false);
					}
				}
			}
		}
		return anchor;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(ExchangeNode.CONNECTION)) {
			if (!((ExchangeNode) getModel()).getExchange().isInput()) {
				refreshSourceConnections();
			} else {
				refreshTargetConnections();
			}
		}
	}

}
