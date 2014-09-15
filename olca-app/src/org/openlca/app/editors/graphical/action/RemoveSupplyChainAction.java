package org.openlca.app.editors.graphical.action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

class RemoveSupplyChainAction extends EditorAction {

	private ProcessNode node;
	private Set<ConnectionLink> connections = new HashSet<>();
	private Set<ProcessNode> nodes = new HashSet<>();
	private Set<ProcessLink> links = new HashSet<>();
	private Set<Long> processIds = new HashSet<>();
	private MutableProcessLinkSearchMap linkSearch; // only used in
													// collectSupplyChain

	RemoveSupplyChainAction() {
		setId(ActionIds.REMOVE_SUPPLY_CHAIN);
		setText(Messages.RemoveSupplyChain);
	}

	@Override
	public void run() {
		clear();
		ProductSystem system = getEditor().getModel().getProductSystem();
		long refId = system.getReferenceProcess().getId();
		if (refId == node.getProcess().getId()) {
			for (ProcessNode node : getEditor().getModel().getChildren())
				if (refId != node.getProcess().getId()) {
					nodes.add(node);
					connections.addAll(node.getLinks());
				}
			processIds.addAll(system.getProcesses());
			processIds.remove(refId);
			links.addAll(system.getProcessLinks());
		} else {
			linkSearch = new MutableProcessLinkSearchMap(
					system.getProcessLinks());
			collectSupplyChain(node.getProcess().getId());
		}
		if (connections.size() > 0 || nodes.size() > 0 || links.size() > 0
				|| processIds.size() > 0)
			getEditor().getCommandStack().execute(new RemoveCommand());
	}

	private void clear() {
		processIds.clear();
		links.clear();
		nodes.clear();
		connections.clear();
	}

	private void collectSupplyChain(long processId) {
		ProductSystemNode systemNode = getEditor().getModel();
		ProcessNode node = systemNode.getProcessNode(processId);
		List<ProcessLink> incomingLinks = linkSearch
				.getIncomingLinks(processId);
		for (ProcessLink link : incomingLinks) {
			if (node != null) {
				ConnectionLink l = node.getLink(link);
				if (l != null)
					connections.add(l);
				else
					links.add(link);
			} else
				links.add(link);
			linkSearch.remove(link);
			if (linkSearch.getOutgoingLinks(link.getProviderId()).size() == 0) {
				collectSupplyChain(link.getProviderId());
				ProcessNode providerNode = getEditor().getModel()
						.getProcessNode(link.getProviderId());
				if (providerNode != null)
					nodes.add(providerNode);
				else
					processIds.add(link.getProviderId());
			}
		}
	}

	@Override
	protected boolean accept(ISelection selection) {
		node = getSingleSelectionOfType(selection, ProcessNode.class);
		return node != null;
	}

	private class RemoveCommand extends Command {

		private Map<Long, Rectangle> layouts = new HashMap<>();
		private Map<String, Boolean> visibility = new HashMap<>();

		@Override
		public boolean canExecute() {
			return true;
		}

		@Override
		public String getLabel() {
			return Messages.RemoveSupplyChain;
		}

		@Override
		public void execute() {
			ProductSystemNode systemNode = node.getParent();
			ProductSystem system = systemNode.getProductSystem();
			for (ConnectionLink link : connections) {
				visibility.put(getKey(link.getProcessLink()), link.isVisible());
				link.unlink();
				links.add(link.getProcessLink());
			}
			system.getProcessLinks().removeAll(links);
			systemNode.getLinkSearch().removeAll(links);
			for (ProcessNode processNode : nodes) {
				layouts.put(processNode.getProcess().getId(),
						processNode.getXyLayoutConstraints());
				systemNode.remove(processNode);
				processIds.add(processNode.getProcess().getId());
			}
			system.getProcesses().removeAll(processIds);
			refresh();
		}

		@Override
		public boolean canUndo() {
			return true;
		}

		@Override
		public void undo() {
			ProductSystemNode systemNode = node.getParent();
			for (Long processId : processIds)
				systemNode.getProductSystem().getProcesses().add(processId);
			for (ProcessNode node : nodes) {
				systemNode.add(node);
				node.setXyLayoutConstraints(layouts.remove(node.getProcess()
						.getId()));
				systemNode.getProductSystem().getProcesses()
						.add(node.getProcess().getId());
				if (node.getParent().getEditor().getOutline() != null)
					node.getParent().getEditor().getOutline().refresh();
			}
			for (ProcessLink link : links) {
				systemNode.getProductSystem().getProcessLinks().add(link);
				systemNode.getLinkSearch().put(link);
			}
			for (ConnectionLink link : connections) {
				systemNode.getProductSystem().getProcessLinks()
						.add(link.getProcessLink());
				systemNode.getLinkSearch().put(link.getProcessLink());
				link.link();
				link.setVisible(visibility.remove(getKey(link.getProcessLink())));
			}
			refresh();
		}

		private void refresh() {
			node.refresh();
			getEditor().setDirty(true);
			if (getEditor().getOutline() != null)
				getEditor().getOutline().refresh();
		}

		@Override
		public void redo() {
			execute();
		}

	}

	private String getKey(ProcessLink link) {
		return link.getProviderId() + "_" + link.getRecipientId() + "_"
				+ link.getFlowId();
	}

}
