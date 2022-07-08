package org.openlca.app.editors.graphical.actions;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.search.LinkSearchMap;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

import java.util.*;

public class RemoveSupplyChainAction extends SelectionAction {

	private final GraphEditor editor;
	private Node node;
	private final Set<Link> connections = new HashSet<>();
	private final Set<Node> nodes = new HashSet<>();
	private final Set<ProcessLink> links = new HashSet<>();
	private final Set<Long> processIds = new HashSet<>();
	// only used in collectSupplyChain
	private LinkSearchMap linkSearch;

	public RemoveSupplyChainAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(ActionIds.REMOVE_SUPPLY_CHAIN);
		setText(M.RemoveSupplyChain);
		setImageDescriptor(Icon.REMOVE_SUPPLY_CHAIN.descriptor());
	}

	@Override
	public void run() {
		clear();
		ProductSystem system = editor.getModel().getProductSystem();
		long refId = system.referenceProcess.id;
		if (refId == node.descriptor.id) {
			for (var node : editor.getModel().getChildren())
				if (refId != node.descriptor.id) {
					nodes.add(node);
					connections.addAll(node.getAllLinks());
				}
			processIds.addAll(system.processes);
			processIds.remove(refId);
			links.addAll(system.processLinks);
		} else {
			linkSearch = new LinkSearchMap(
					system.processLinks);
			collectSupplyChain(node.descriptor.id);
		}
		if (connections.size() > 0 || nodes.size() > 0 || links.size() > 0
				|| processIds.size() > 0)
			((CommandStack) editor.getAdapter(CommandStack.class))
				.execute(new RemoveCommand());
	}

	private void clear() {
		processIds.clear();
		links.clear();
		nodes.clear();
		connections.clear();
	}

	private void collectSupplyChain(long processId) {
		var systemNode = editor.getModel();
		var node = systemNode.getNode(processId);
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
				var providerNode = editor.getModel().getNode(link.providerId);
				if (providerNode != null)
					nodes.add(providerNode);
				else
					processIds.add(link.providerId);
			}
		}
	}

	@Override
	protected boolean calculateEnabled() {
		if (getSelectedObjects().size() != 1)
			return false;

		var object = getSelectedObjects().get(0);
		if (NodeEditPart.class.isAssignableFrom(object.getClass()))
				node = ((NodeEditPart) object).getModel();
		return node != null;
	}

	private class RemoveCommand extends Command {

		private final Map<Long, Rectangle> bounds = new HashMap<>();

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
			Graph graph = node.getGraph();
			ProductSystem system = graph.getProductSystem();
			for (Link link : connections) {
				link.disconnect();
				links.add(link.processLink);
			}
			system.processLinks.removeAll(links);
			graph.linkSearch.removeAll(links);
			for (Node node : nodes) {
				bounds.put(node.descriptor.id,
					new Rectangle(node.getLocation(), node.getSize()));
				graph.removeChild(node);
				processIds.add(node.descriptor.id);
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
			Graph graph = node.getGraph();
			for (Long processId : processIds)
				graph.getProductSystem().processes.add(processId);
			for (Node node : nodes) {
				graph.addChild(node);
				var r = bounds.remove(node.descriptor.id);
				node.setLocation(r.getLocation());
				node.setSize(r.getSize());
				graph.getProductSystem().processes.add(node.descriptor.id);
			}
			for (ProcessLink link : links) {
				graph.getProductSystem().processLinks.add(link);
				graph.linkSearch.put(link);
			}
			for (Link link : connections) {
				graph.getProductSystem().processLinks.add(link.processLink);
				graph.linkSearch.put(link.processLink);
				link.reconnect();
			}
			refresh();
		}

		@Override
		public void redo() {
			execute();
		}

	}

}
