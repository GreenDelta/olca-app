package org.openlca.app.editors.graphical.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class MassCreationCommand extends Command {

	private final ProductSystemNode sysNode;
	private final List<CategorizedDescriptor> toCreate;
	private final List<ProcessLink> newLinks;
	// for undoing
	private final Map<IFigure, Rectangle> oldConstraints = new HashMap<>();
	private final List<ProcessNode> createdNodes = new ArrayList<>();
	private final List<Link> createdLinks = new ArrayList<>();

	public static MassCreationCommand nextTier(List<CategorizedDescriptor> toCreate,
			List<ProcessLink> newConnections, ProductSystemNode model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.BuildNextTier);
	}

	public static MassCreationCommand providers(List<CategorizedDescriptor> toCreate,
			List<ProcessLink> newConnections, ProductSystemNode model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.ConnectProviders);
	}

	public static MassCreationCommand recipients(List<CategorizedDescriptor> toCreate,
			List<ProcessLink> newConnections, ProductSystemNode model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.ConnectRecipients);
	}

	private MassCreationCommand(
			ProductSystemNode model,
			List<CategorizedDescriptor> toCreate,
			List<ProcessLink> newConnections,
			String label) {
		this.sysNode = model;
		this.toCreate = toCreate;
		this.newLinks = newConnections;
		setLabel(label);
	}

	@Override
	public void execute() {
		for (CategorizedDescriptor process : toCreate)
			addNode(process);
		for (ProcessLink newLink : newLinks)
			link(newLink);
		for (ProcessNode node : sysNode.getChildren())
			if (node.figure.isVisible())
				oldConstraints.put(node.figure, node.figure.getBounds().getCopy());
		((LayoutManager) sysNode.figure.getLayoutManager()).layout(sysNode.figure, sysNode.editor.getLayoutType());
		sysNode.editor.setDirty();
		if (sysNode.editor.getOutline() != null)
			sysNode.editor.getOutline().refresh();
	}

	private void addNode(CategorizedDescriptor process) {
		if (sysNode.getProcessNode(process.id) != null)
			return;
		ProcessNode node = new ProcessNode(sysNode.editor, process);
		sysNode.getProductSystem().processes.add(process.id);
		sysNode.add(node);
		createdNodes.add(node);
	}

	private void link(ProcessLink newLink) {
		ProductSystem system = sysNode.getProductSystem();
		system.processLinks.add(newLink);
		sysNode.linkSearch.put(newLink);
		Link link = new Link();
		link.processLink = newLink;

		FlowType ftype = sysNode.flows.type(newLink.flowId);
		boolean isWaste = ftype == FlowType.WASTE_FLOW;
		link.outputNode = sysNode.getProcessNode(
				isWaste ? newLink.processId
						: newLink.providerId);
		link.inputNode = sysNode.getProcessNode(
				isWaste ? newLink.providerId
						: newLink.processId);
		link.link();
		createdLinks.add(link);
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		for (Link link : createdLinks)
			unlink(link);
		for (ProcessNode node : createdNodes)
			removeNode(node);
		for (ProcessNode node : sysNode.getChildren())
			if (oldConstraints.get(node.figure) != null)
				node.setBox(oldConstraints.get(node.figure));
		createdLinks.clear();
		createdNodes.clear();
		oldConstraints.clear();
		if (sysNode.editor.getOutline() != null)
			sysNode.editor.getOutline().refresh();
		sysNode.editor.setDirty();
	}

	private void removeNode(ProcessNode node) {
		sysNode.getProductSystem().processes.remove(node.process.id);
		sysNode.remove(node);
	}

	private void unlink(Link link) {
		ProductSystem system = sysNode.getProductSystem();
		system.processLinks.remove(link.processLink);
		sysNode.linkSearch.remove(link.processLink);
		link.unlink();
	}
}
