package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.graphical.GraphUtil;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class ProcessExpander extends ImageFigure {

	private ProcessNode node;
	private Side side;
	private boolean expanded;
	// isCollapsing is used to prevent endless recursion in collapse()
	private boolean isCollapsing;

	ProcessExpander(ProcessNode node, Side side) {
		this.node = node;
		this.side = side;
		setImage(Icon.PLUS.get());
		setVisible(shouldBeVisible());
		addMouseListener(new ExpansionListener());
	}

	boolean shouldBeVisible() {
		MutableProcessLinkSearchMap linkSearch = node.getParent()
				.getLinkSearch();
		long processId = node.getProcess().getId();
		for (ProcessLink link : linkSearch.getLinks(processId))
			if (side == Side.LEFT && link.processId == processId)
				return true;
			else if (side == Side.RIGHT && link.providerId == processId)
				return true;
		return false;
	}

	void expand() {
		createNecessaryNodes();
		showLinksAndNodes();
		expanded = true;
		setImage(Icon.MINUS.get());
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
		MutableProcessLinkSearchMap linkSearch = systemNode.getLinkSearch();
		long processId = node.getProcess().getId();
		List<ProcessLink> links = side == Side.LEFT ? linkSearch
				.getIncomingLinks(processId) : linkSearch
						.getOutgoingLinks(processId);
		Map<Long, ProcessDescriptor> map = getLinkedProcesses(links);
		for (ProcessLink link : links) {
			long linkedProcessId = side == Side.LEFT ? link.providerId
					: link.processId;
			ProcessNode node = systemNode.getProcessNode(linkedProcessId);
			if (node == null) {
				ProcessDescriptor descriptor = map.get(linkedProcessId);
				node = new ProcessNode(descriptor);
				node.loadExchangeNodes();
				systemNode.add(node);
			}
			ProcessNode sourceNode = side == Side.LEFT ? node : this.node;
			ProcessNode targetNode = side == Side.LEFT ? this.node : node;
			ConnectionLink connection = new ConnectionLink();
			connection.provider = sourceNode.getProviderNode(link.flowId);
			connection.exchange = targetNode.getExchangeNode(link.exchangeId);
			connection.processLink = link;
			connection.link();
		}
	}

	private Map<Long, ProcessDescriptor> getLinkedProcesses(
			List<ProcessLink> links) {
		HashSet<Long> processIds = new HashSet<>();
		for (ProcessLink link : links)
			if (side == Side.LEFT)
				processIds.add(link.providerId);
			else
				processIds.add(link.processId);
		return Cache.getEntityCache().getAll(ProcessDescriptor.class,
				processIds);
	}

	void collapse(ProcessNode initialNode) {
		if (isCollapsing)
			return;
		isCollapsing = true;
		ConnectionLink[] links = node.getLinks().toArray(
				new ConnectionLink[node.getLinks().size()]);
		for (ConnectionLink link : links) {
			ProcessNode thisNode = GraphUtil.getProcessNode(side == Side.LEFT
					? link.exchange : link.provider);
			ProcessNode otherNode = GraphUtil.getProcessNode(side == Side.LEFT
					? link.provider : link.exchange);
			if (!thisNode.equals(node))
				continue;
			link.unlink();
			otherNode.collapseLeft(initialNode);
			otherNode.collapseRight(initialNode);
			if (otherNode.equals(initialNode))
				continue;
			if (!otherNode.getLinks().isEmpty())
				continue;
			node.getParent().remove(otherNode);
		}
		expanded = false;
		setImage(Icon.PLUS.get());
		isCollapsing = false;
	}

	private ProcessNode getMatchingNode(ConnectionLink link) {
		ProcessNode source = GraphUtil.getProcessNode(link.provider);
		ProcessNode target = GraphUtil.getProcessNode(link.exchange);
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
		if (!link.provider.getFigure().isVisible())
			return false;
		if (!link.exchange.getFigure().isVisible())
			return false;
		return true;
	}

	void refresh() {
		setVisible(shouldBeVisible());
		if (expanded)
			setImage(Icon.MINUS.get());
		else
			setImage(Icon.PLUS.get());
	}

	boolean isExpanded() {
		return expanded;
	}

	void setExpanded(boolean value) {
		expanded = value;
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
