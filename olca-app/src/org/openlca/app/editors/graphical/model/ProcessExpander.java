package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.resources.ImageType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class ProcessExpander extends ImageFigure {

	private ProcessNode node;
	private Side side;
	private boolean expanded;

	ProcessExpander(ProcessNode node, Side side) {
		this.node = node;
		this.side = side;
		setImage(ImageType.PLUS_ICON.get());
		setVisible(shouldBeVisible());
		addMouseListener(new ExpansionListener());
	}

	boolean shouldBeVisible() {
		ProductSystem system = node.getParent().getProductSystem();
		long processId = node.getProcess().getId();
		for (ProcessLink link : system.getProcessLinks(processId))
			if (side == Side.LEFT && link.getRecipientId() == processId)
				return true;
			else if (side == Side.RIGHT && link.getProviderId() == processId)
				return true;
		return false;
	}

	void expand() {
		createNecessaryNodes();
		showLinksAndNodes();
		expanded = true;
		setImage(ImageType.MINUS_ICON.get());
	}

	private List<ProcessNode> getNodesToShow() {
		List<ProcessNode> nodes = new ArrayList<>();
		for (ConnectionLink link : node.getLinks()) {
			ProcessNode match = getMatchingNode(link);
			if (match != null && !nodes.contains(match))
				nodes.add(match);
		}
		return nodes;
	}

	private void createNecessaryNodes() {
		ProductSystemNode systemNode = node.getParent();
		ProcessLink[] links = side == Side.LEFT ? systemNode.getProductSystem()
				.getIncomingLinks(node.getProcess().getId()) : systemNode
				.getProductSystem().getOutgoingLinks(node.getProcess().getId());
		for (ProcessLink link : links) {
			long processId = side == Side.LEFT ? link.getProviderId() : link
					.getRecipientId();
			ProcessNode node = systemNode.getProcessNode(processId);
			if (node == null) {
				ProcessDescriptor descriptor = Cache.getEntityCache().get(
						ProcessDescriptor.class, processId);
				node = new ProcessNode(descriptor);
				systemNode.add(node);
			}
			ProcessNode sourceNode = side == Side.LEFT ? node : this.node;
			ProcessNode targetNode = side == Side.LEFT ? this.node : node;
			ConnectionLink connectionLink = new ConnectionLink();
			connectionLink.setSourceNode(sourceNode);
			connectionLink.setTargetNode(targetNode);
			connectionLink.setProcessLink(link);
			connectionLink.link();
		}
	}

	void collapse() {
		// TODO something is still not working 100%
		ConnectionLink[] links = node.getLinks().toArray(
				new ConnectionLink[node.getLinks().size()]);
		for (ConnectionLink link : links) {
			ProcessNode thisNode = side == Side.LEFT ? link.getTargetNode()
					: link.getSourceNode();
			ProcessNode otherNode = side == Side.LEFT ? link.getSourceNode()
					: link.getTargetNode();
			if (thisNode.equals(node)) {
				link.unlink();
				boolean hasOtherConnections = side == Side.LEFT ? otherNode
						.hasOutgoingConnections() : otherNode
						.hasIncomingConnections();
				if (!hasOtherConnections) {
					otherNode.collapseLeft();
					otherNode.collapseRight();
					node.getParent().remove(otherNode);
				}
			}
		}
		expanded = false;
		setImage(ImageType.PLUS_ICON.get());
	}

	private ProcessNode getMatchingNode(ConnectionLink link) {
		ProcessNode source = link.getSourceNode();
		ProcessNode target = link.getTargetNode();
		if (side == Side.LEFT)
			if (target.equals(node))
				if (!source.equals(node))
					return source;
		if (side == Side.RIGHT)
			if (source.equals(node))
				if (!target.equals(node))
					return target;
		return null;
	}

	private void showLinksAndNodes() {
		List<ProcessNode> nodes = getNodesToShow();
		for (ProcessNode node : nodes) {
			node.setVisible(true);
			for (ConnectionLink link : node.getLinks())
				if (processFiguresVisible(link))
					link.setVisible(true);
		}
	}

	private boolean processFiguresVisible(ConnectionLink link) {
		if (!link.getSourceNode().getFigure().isVisible())
			return false;
		if (!link.getTargetNode().getFigure().isVisible())
			return false;
		return true;
	}

	void refresh() {
		setVisible(shouldBeVisible());
		
	}

	boolean isExpanded() {
		return expanded;
	}

	enum Side {
		LEFT, RIGHT;
	}

	private class ExpansionListener implements MouseListener {

		@Override
		public void mouseDoubleClicked(MouseEvent me) {

		}

		@Override
		public void mousePressed(MouseEvent me) {
			Command command = null;
			if (side == Side.LEFT)
				if (expanded)
					command = CommandFactory.createCollapseLeftCommand(node);
				else
					command = CommandFactory.createExpandLeftCommand(node);
			else if (side == Side.RIGHT)
				if (expanded)
					command = CommandFactory.createCollapseRightCommand(node);
				else
					command = CommandFactory.createExpandRightCommand(node);
			node.getParent().getEditor().getCommandStack().execute(command);
		}

		@Override
		public void mouseReleased(MouseEvent me) {

		}
	}

}
