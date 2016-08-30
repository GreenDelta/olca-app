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
import org.openlca.app.editors.graphical.command.ExpansionCommand;
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
		MutableProcessLinkSearchMap linkSearch = node.parent().linkSearch;
		long processId = node.process.getId();
		for (ProcessLink link : linkSearch.getLinks(processId)) {
			if (side == Side.LEFT && link.processId == processId)
				return true;
			if (side == Side.RIGHT && link.providerId == processId)
				return true;
		}
		return false;
	}

	void expand() {
		createNecessaryNodes();
		showLinksAndNodes();
		setImage(Icon.MINUS.get());
		expanded = true;
	}

	private List<ProcessNode> getNodesToShow() {
		List<ProcessNode> nodes = new ArrayList<>();
		for (Link link : node.links) {
			ProcessNode match = getMatchingNode(link);
			if (match == null || nodes.contains(match))
				continue;
			nodes.add(match);
		}
		return nodes;
	}

	private void createNecessaryNodes() {
		ProductSystemNode systemNode = node.parent();
		MutableProcessLinkSearchMap linkSearch = systemNode.linkSearch;
		long processId = node.process.getId();
		List<ProcessLink> links = null;
		if (side == Side.LEFT)
			links = linkSearch.getIncomingLinks(processId);
		else
			links = linkSearch.getOutgoingLinks(processId);
		Map<Long, ProcessDescriptor> map = getLinkedProcesses(links);
		for (ProcessLink link : links) {
			long linkedProcessId = side == Side.LEFT ? link.providerId : link.processId;
			ProcessNode node = systemNode.getProcessNode(linkedProcessId);
			if (node == null) {
				ProcessDescriptor descriptor = map.get(linkedProcessId);
				node = new ProcessNode(descriptor);
				systemNode.add(node);
			}
			ProcessNode sourceNode = side == Side.LEFT ? node : this.node;
			ProcessNode targetNode = side == Side.LEFT ? this.node : node;
			Link connectionLink = new Link();
			connectionLink.sourceNode = sourceNode;
			connectionLink.targetNode = targetNode;
			connectionLink.processLink = link;
			connectionLink.link();
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
		return Cache.getEntityCache().getAll(ProcessDescriptor.class, processIds);
	}

	void collapse(ProcessNode initialNode) {
		if (isCollapsing)
			return;
		isCollapsing = true;
		Link[] links = node.links.toArray(
				new Link[node.links.size()]);
		for (Link link : links) {
			ProcessNode thisNode = side == Side.LEFT ? link.targetNode : link.sourceNode;
			ProcessNode otherNode = side == Side.LEFT ? link.sourceNode : link.targetNode;
			if (!thisNode.equals(node))
				continue;
			link.unlink();
			otherNode.collapseLeft(initialNode);
			otherNode.collapseRight(initialNode);
			if (otherNode.equals(initialNode))
				continue;
			if (!otherNode.links.isEmpty())
				continue;
			node.parent().remove(otherNode);
		}
		setImage(Icon.PLUS.get());
		isCollapsing = false;
		expanded = false;
	}

	private ProcessNode getMatchingNode(Link link) {
		ProcessNode source = link.sourceNode;
		ProcessNode target = link.targetNode;
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
			for (Link link : node.links) {
				if (!processFiguresVisible(link))
					continue;
				link.setVisible(true);
			}
		}
	}

	private boolean processFiguresVisible(Link link) {
		if (!link.sourceNode.isVisible())
			return false;
		if (!link.targetNode.isVisible())
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
			Command command = getCommand();
			node.parent().editor.getCommandStack().execute(command);
		}

		private Command getCommand() {
			if (side == Side.LEFT) {
				if (expanded)
					return ExpansionCommand.collapseLeft(node);
				return ExpansionCommand.expandLeft(node);
			}
			if (expanded)
				return ExpansionCommand.collapseRight(node);
			return ExpansionCommand.expandRight(node);
		}

		@Override
		public void mouseReleased(MouseEvent me) {

		}
	}

}
