package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Database;
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
		setVisible(isInitiallyVisible());
		addMouseListener(new ExpansionListener());
	}

	private boolean isInitiallyVisible() {
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
				ProcessDescriptor descriptor = Database.getCache().get(
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
		List<ProcessNode> nodes = getNodesToCollapse();
		hideLinksAndNodes(nodes);
		expanded = false;

		for (ProcessNode node : nodes) {
			if (node.equals(this.node))
				continue;
			if (node.isExpandedLeft())
				node.collapseLeft();
			if (node.isExpandedRight())
				node.collapseRight();
		}
	}

	private List<ProcessNode> getNodesToCollapse() {
		List<ProcessNode> nodes = new ArrayList<>();
		for (ConnectionLink link : node.getLinks()) {
			ProcessNode match = getMatchingNode(link);
			if (match != null && !nodes.contains(match))
				if (!needsToBeVisible(match))
					nodes.add(match);
		}
		return nodes;
	}

	boolean needsToBeVisible(ProcessNode node) {
		for (ConnectionLink link : node.getLinks()) {
			ProcessNode source = link.getSourceNode();
			ProcessNode target = link.getTargetNode();
			if (source.equals(node))
				if (!target.equals(this.node))
					if (target.isVisible() && target.isExpandedLeft())
						return true;
			if (target.equals(node))
				if (!source.equals(this.node))
					if (source.isVisible() && source.isExpandedRight())
						return true;
		}
		return false;
	}

	private ProcessNode getMatchingNode(ConnectionLink link) {
		ProcessNode source = link.getSourceNode();
		ProcessNode target = link.getTargetNode();
		if (side == Side.LEFT)
			if (source.equals(node))
				if (!target.equals(node))
					return target;
		if (side == Side.RIGHT)
			if (target.equals(node))
				if (!source.equals(node))
					return source;
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

	private void hideLinksAndNodes(List<ProcessNode> nodes) {
		for (ProcessNode node : nodes) {
			node.setVisible(true);
			for (ConnectionLink link : node.getLinks())
				link.setVisible(false);
		}
	}

	private boolean processFiguresVisible(ConnectionLink link) {
		if (!link.getSourceNode().getFigure().isVisible())
			return false;
		if (!link.getTargetNode().getFigure().isVisible())
			return false;
		return true;
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
					command = CommandFactory.createExpandLeftCommand(node);
				else
					command = CommandFactory.createCollapseLeftCommand(node);
			else if (side == Side.RIGHT)
				if (expanded)
					command = CommandFactory.createExpandRightCommand(node);
				else
					command = CommandFactory.createCollapseRightCommand(node);
			node.getParent().getEditor().getCommandStack().execute(command);
		}

		@Override
		public void mouseReleased(MouseEvent me) {

		}
	}

}
