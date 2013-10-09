package org.openlca.app.editors.graphical.action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class RemoveSupplyChainAction extends EditorAction {

	private ProcessNode node;

	RemoveSupplyChainAction() {
		setId(ActionIds.REMOVE_SUPPLY_CHAIN_ACTION_ID);
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
		ProductSystem system = getEditor().getModel().getProductSystem();
		ProcessNode node = getEditor().getModel().getProcessNode(processId);
		ProcessLink[] incomingLinks = system.getIncomingLinks(processId);
		for (ProcessLink link : incomingLinks) {
			if (node != null) {
				ConnectionLink l = node.getLink(link);
				if (l != null)
					links.add(l);
			}
			system.getProcessLinks().remove(link);
			if (system.getOutgoingLinks(link.getProviderId()).length == 0) {
				removeSupplyChain(link.getProviderId(), links, nodes);
				system.getProcesses().remove(link.getProviderId());
				ProcessNode providerNode = getEditor().getModel()
						.getProcessNode(link.getProviderId());
				if (providerNode != null)
					nodes.add(providerNode);
			}
		}
	}

	@Override
	protected boolean accept(ISelection selection) {
		node = null;
		if (selection == null)
			return false;
		if (selection.isEmpty())
			return false;
		if (!(selection instanceof IStructuredSelection))
			return false;

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() > 1)
			return false;
		if (!(sel.getFirstElement() instanceof EditPart))
			return false;
		Object model = ((EditPart) sel.getFirstElement()).getModel();
		if (!(model instanceof ProcessNode))
			return false;
		node = (ProcessNode) model;
		return true;
	}

	private class RemoveCommand extends Command {

		private Set<ProcessNode> nodes;
		private Set<ConnectionLink> links;
		private Map<Long, Rectangle> layouts = new HashMap<>();
		private Map<ProcessLink, Boolean> visibility = new HashMap<>();

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
			for (ConnectionLink link : links) {
				visibility.put(link.getProcessLink(), link.isVisible());
				link.unlink();
				link.getSourceNode().getParent().getProductSystem()
						.getProcessLinks().remove(link.getProcessLink());
			}
			for (ProcessNode node : nodes) {
				layouts.put(node.getProcess().getId(),
						node.getXyLayoutConstraints());
				node.getParent().getProductSystem().getProcesses()
						.remove(node.getProcess().getId());
				node.getParent().remove(node);
				if (node.getParent().getEditor().getOutline() != null)
					node.getParent().getEditor().getOutline().refresh();
			}
		}

		@Override
		public boolean canUndo() {
			return true;
		}

		@Override
		public void undo() {
			for (ProcessNode node : nodes) {
				node.getParent().add(node);
				node.setXyLayoutConstraints(layouts.remove(node.getProcess()
						.getId()));
				node.getParent().getProductSystem().getProcesses()
						.add(node.getProcess().getId());
				if (node.getParent().getEditor().getOutline() != null)
					node.getParent().getEditor().getOutline().refresh();
			}
			for (ConnectionLink link : links) {
				link.getSourceNode().getParent().getProductSystem()
						.getProcessLinks().add(link.getProcessLink());
				link.link();
				link.setVisible(visibility.remove(link.getProcessLink()));
			}
		}

		@Override
		public void redo() {
			execute();
		}

	}

}
