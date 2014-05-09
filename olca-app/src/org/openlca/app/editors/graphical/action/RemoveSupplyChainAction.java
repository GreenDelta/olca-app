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
import org.openlca.core.matrix.ProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

class RemoveSupplyChainAction extends EditorAction {

	private ProcessNode node;

	RemoveSupplyChainAction() {
		setId(ActionIds.REMOVE_SUPPLY_CHAIN);
		setText(Messages.Systems_RemoveSupplyChainAction_Text);
	}

	@Override
	public void run() {
		Set<ConnectionLink> links = new HashSet<>();
		Set<ProcessNode> nodes = new HashSet<>();
		removeSupplyChain(node.getProcess().getId(), links, nodes);
		if (links.size() > 0 || nodes.size() > 0)
			getEditor().getCommandStack().execute(
					new RemoveCommand(nodes, links));
	}

	private void removeSupplyChain(long processId, Set<ConnectionLink> links,
			Set<ProcessNode> nodes) {
		ProductSystemNode systemNode = getEditor().getModel();
		ProductSystem system = systemNode.getProductSystem();
		ProcessNode node = systemNode.getProcessNode(processId);
		ProcessLinkSearchMap linkSearch = systemNode.getLinkSearch();
		List<ProcessLink> incomingLinks = linkSearch
				.getIncomingLinks(processId);
		for (ProcessLink link : incomingLinks) {
			if (node != null) {
				ConnectionLink l = node.getLink(link);
				if (l != null)
					links.add(l);
			}
			system.getProcessLinks().remove(link);
			if (linkSearch.getOutgoingLinks(link.getProviderId()).size() == 0) {
				removeSupplyChain(link.getProviderId(), links, nodes);
				system.getProcesses().remove(link.getProviderId());
				ProcessNode providerNode = getEditor().getModel()
						.getProcessNode(link.getProviderId());
				if (providerNode != null)
					nodes.add(providerNode);
			}
		}
		systemNode.reindexLinks();
	}

	@Override
	protected boolean accept(ISelection selection) {
		node = getSingleSelectionOfType(selection, ProcessNode.class);
		return node != null;
	}

	private class RemoveCommand extends Command {

		private Set<ProcessNode> nodes;
		private Set<ConnectionLink> links;
		private Map<Long, Rectangle> layouts = new HashMap<>();
		private Map<String, Boolean> visibility = new HashMap<>();

		private RemoveCommand(Set<ProcessNode> nodes, Set<ConnectionLink> links) {
			this.nodes = nodes;
			this.links = links;
		}

		@Override
		public boolean canExecute() {
			return true;
		}

		@Override
		public String getLabel() {
			return Messages.Systems_RemoveSupplyChainAction_Text;
		}

		@Override
		public void execute() {
			ProductSystemNode systemNode = node.getParent();
			ProductSystem system = systemNode.getProductSystem();
			for (ConnectionLink link : links) {
				visibility.put(getKey(link.getProcessLink()), link.isVisible());
				link.unlink();
				system.getProcessLinks().remove(link.getProcessLink());
			}
			for (ProcessNode processNode : nodes) {
				layouts.put(processNode.getProcess().getId(),
						processNode.getXyLayoutConstraints());
				system.getProcesses().remove(processNode.getProcess().getId());
				systemNode.remove(processNode);
			}
			refresh(systemNode);
		}

		@Override
		public boolean canUndo() {
			return true;
		}

		@Override
		public void undo() {
			ProductSystemNode systemNode = node.getParent();
			for (ProcessNode node : nodes) {
				systemNode.add(node);
				node.setXyLayoutConstraints(layouts.remove(node.getProcess()
						.getId()));
				systemNode.getProductSystem().getProcesses()
						.add(node.getProcess().getId());
				if (node.getParent().getEditor().getOutline() != null)
					node.getParent().getEditor().getOutline().refresh();
			}
			for (ConnectionLink link : links) {
				systemNode.getProductSystem().getProcessLinks()
						.add(link.getProcessLink());
				link.link();
				link.setVisible(visibility.remove(getKey(link.getProcessLink())));
			}
			refresh(systemNode);
		}

		private void refresh(ProductSystemNode systemNode) {
			systemNode.reindexLinks();
			if (systemNode.getEditor().getOutline() != null)
				systemNode.getEditor().getOutline().refresh();
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
