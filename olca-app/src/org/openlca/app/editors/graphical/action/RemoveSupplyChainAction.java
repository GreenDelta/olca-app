package org.openlca.app.editors.graphical.action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

class RemoveSupplyChainAction extends EditorAction {

	private ProcessNode node;
	private Set<Link> connections = new HashSet<>();
	private Set<ProcessNode> nodes = new HashSet<>();
	private Set<ProcessLink> links = new HashSet<>();
	private Set<Long> processIds = new HashSet<>();
	// only used in collectSupplyChain
	private MutableProcessLinkSearchMap linkSearch;

	RemoveSupplyChainAction() {
		setId(ActionIds.REMOVE_SUPPLY_CHAIN);
		setText(M.RemoveSupplyChain);
		setImageDescriptor(Icon.REMOVE_SUPPLY_CHAIN.descriptor());
	}

	@Override
	public void run() {
		clear();
		ProductSystem system = editor.getModel().getProductSystem();
		long refId = system.referenceProcess.id;
		if (refId == node.process.id) {
			for (ProcessNode node : editor.getModel().getChildren())
				if (refId != node.process.id) {
					nodes.add(node);
					connections.addAll(node.links);
				}
			processIds.addAll(system.processes);
			processIds.remove(refId);
			links.addAll(system.processLinks);
		} else {
			linkSearch = new MutableProcessLinkSearchMap(
					system.processLinks);
			collectSupplyChain(node.process.id);
		}
		if (connections.size() > 0 || nodes.size() > 0 || links.size() > 0
				|| processIds.size() > 0)
			editor.getCommandStack().execute(new RemoveCommand());
	}

	private void clear() {
		processIds.clear();
		links.clear();
		nodes.clear();
		connections.clear();
	}

	private void collectSupplyChain(long processId) {
		ProductSystemNode systemNode = editor.getModel();
		ProcessNode node = systemNode.getProcessNode(processId);
		List<ProcessLink> incomingLinks = linkSearch
				.getConnectionLinks(processId);
		for (ProcessLink link : incomingLinks) {
			if (node != null) {
				Link l = node.getLink(link);
				if (l != null)
					connections.add(l);
				else
					links.add(link);
			} else {
				links.add(link);
			}
			linkSearch.remove(link);
			if (linkSearch.getProviderLinks(link.providerId).size() == 0) {
				collectSupplyChain(link.providerId);
				ProcessNode providerNode = editor.getModel().getProcessNode(link.providerId);
				if (providerNode != null)
					nodes.add(providerNode);
				else
					processIds.add(link.providerId);
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
			return M.RemoveSupplyChain;
		}

		@Override
		public void execute() {
			ProductSystemNode systemNode = node.parent();
			ProductSystem system = systemNode.getProductSystem();
			for (Link link : connections) {
				visibility.put(getKey(link.processLink), link.isVisible());
				link.unlink();
				links.add(link.processLink);
			}
			system.processLinks.removeAll(links);
			systemNode.linkSearch.removeAll(links);
			for (ProcessNode processNode : nodes) {
				layouts.put(processNode.process.id, processNode.getXyLayoutConstraints());
				systemNode.remove(processNode);
				processIds.add(processNode.process.id);
			}
			system.processes.removeAll(processIds);
			refresh();
		}

		@Override
		public boolean canUndo() {
			return true;
		}

		@Override
		public void undo() {
			ProductSystemNode systemNode = node.parent();
			for (Long processId : processIds)
				systemNode.getProductSystem().processes.add(processId);
			for (ProcessNode node : nodes) {
				systemNode.add(node);
				node.setXyLayoutConstraints(layouts.remove(node.process.id));
				systemNode.getProductSystem().processes.add(node.process.id);
				if (node.parent().editor.getOutline() == null)
					continue;
				node.parent().editor.getOutline().refresh();
			}
			for (ProcessLink link : links) {
				systemNode.getProductSystem().processLinks.add(link);
				systemNode.linkSearch.put(link);
			}
			for (Link link : connections) {
				systemNode.getProductSystem().processLinks.add(link.processLink);
				systemNode.linkSearch.put(link.processLink);
				link.link();
				link.setVisible(visibility.remove(getKey(link.processLink)));
			}
			refresh();
		}

		private void refresh() {
			node.refresh();
			editor.setDirty(true);
			if (editor.getOutline() != null)
				editor.getOutline().refresh();
		}

		@Override
		public void redo() {
			execute();
		}

	}

	private String getKey(ProcessLink link) {
		return link.providerId + "->" + link.flowId + "->" + link.processId + "->" + link.exchangeId;
	}

}
