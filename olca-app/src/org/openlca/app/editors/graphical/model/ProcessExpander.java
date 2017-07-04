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
import org.openlca.core.model.FlowType;
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
		ProductSystemNode sysNode = node.parent();
		MutableProcessLinkSearchMap linkSearch = sysNode.linkSearch;
		long processId = node.process.getId();
		for (ProcessLink link : linkSearch.getLinks(processId)) {
			FlowType type = sysNode.flowTypes.get(link.flowId);
			boolean isProvider = link.providerId == processId;
			if (side == Side.INPUT) {
				if (type == FlowType.PRODUCT_FLOW && !isProvider)
					return true;
				if (type == FlowType.WASTE_FLOW && isProvider)
					return true;
			} else if (side == Side.OUTPUT) {
				if (type == FlowType.PRODUCT_FLOW && isProvider)
					return true;
				if (type == FlowType.WASTE_FLOW && !isProvider)
					return true;
			}
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
		ProductSystemNode sysNode = node.parent();
		MutableProcessLinkSearchMap linkMap = sysNode.linkSearch;
		long processId = node.process.getId();
		List<ProcessLink> links = linkMap.getLinks(processId);
		Map<Long, ProcessDescriptor> map = processMap(links);
		for (ProcessLink pLink : links) {
			FlowType type = sysNode.flowTypes.get(pLink.flowId);
			if (type == null || type == FlowType.ELEMENTARY_FLOW)
				continue;
			ProcessNode outNode;
			ProcessNode inNode;
			if (side == Side.INPUT) {
				inNode = this.node;
				long otherID = type == FlowType.PRODUCT_FLOW
						? pLink.providerId
						: pLink.processId;
				outNode = node(otherID, sysNode, map);
			} else {
				outNode = this.node;
				long otherID = type == FlowType.PRODUCT_FLOW
						? pLink.processId
						: pLink.providerId;
				inNode = node(otherID, sysNode, map);
			}
			Link link = new Link();
			link.outputNode = outNode;
			link.inputNode = inNode;
			link.processLink = pLink;
			link.link();
		}
	}

	private ProcessNode node(long processID, ProductSystemNode sysNode,
			Map<Long, ProcessDescriptor> map) {
		ProcessNode node = sysNode.getProcessNode(processID);
		if (node != null)
			return node;
		ProcessDescriptor d = map.get(processID);
		node = new ProcessNode(d);
		sysNode.add(node);
		return node;
	}

	private Map<Long, ProcessDescriptor> processMap(List<ProcessLink> links) {
		HashSet<Long> processIds = new HashSet<>();
		for (ProcessLink link : links) {
			processIds.add(link.providerId);
			processIds.add(link.processId);
		}
		return Cache.getEntityCache().getAll(ProcessDescriptor.class, processIds);
	}

	void collapse(ProcessNode initialNode) {
		if (isCollapsing)
			return;
		isCollapsing = true;
		Link[] links = node.links.toArray(
				new Link[node.links.size()]);
		for (Link link : links) {
			ProcessNode thisNode = side == Side.INPUT ? link.inputNode : link.outputNode;
			ProcessNode otherNode = side == Side.INPUT ? link.outputNode : link.inputNode;
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
		ProcessNode source = link.outputNode;
		ProcessNode target = link.inputNode;
		if (side == Side.INPUT)
			if (target.equals(node))
				if (!source.equals(node))
					return source;
		if (side == Side.OUTPUT)
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
		if (!link.outputNode.isVisible())
			return false;
		if (!link.inputNode.isVisible())
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
		INPUT, OUTPUT;
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
			if (side == Side.INPUT) {
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
