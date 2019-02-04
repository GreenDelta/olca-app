package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class ReconnectLinkCommand extends Command {

	private final ProcessNode sourceNode;
	private final ExchangeNode targetNode;
	private final Link oldLink;
	private Link link;

	public ReconnectLinkCommand(ProcessNode sourceNode, ExchangeNode targetNode, Link oldLink) {
		this.sourceNode = sourceNode;
		this.targetNode = targetNode;
		this.oldLink = oldLink;
	}

	@Override
	public boolean canExecute() {
		if (sourceNode == null)
			return false;
		if (targetNode == null)
			return false;
		if (oldLink == null)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		ProductSystemNode systemNode = sourceNode.parent();
		oldLink.unlink();
		systemNode.getProductSystem().processLinks.remove(oldLink.processLink);
		systemNode.linkSearch.remove(oldLink.processLink);
		ProcessLink processLink = new ProcessLink();
		processLink.providerId = sourceNode.process.id;
		processLink.flowId = oldLink.processLink.flowId;
		processLink.processId = targetNode.parent().process.id;
		processLink.exchangeId = targetNode.exchange.id;
		systemNode.getProductSystem().processLinks.add(processLink);
		systemNode.linkSearch.put(processLink);
		link = new Link();
		link.outputNode = sourceNode;
		link.inputNode = targetNode.parent();
		link.processLink = processLink;
		link.link();
		systemNode.editor.setDirty(true);
	}

	@Override
	public String getLabel() {
		return M.ReconnectProcesslink;
	}

	@Override
	public void redo() {
		ProductSystemNode systemNode = sourceNode.parent();
		ProductSystem system = systemNode.getProductSystem();
		oldLink.unlink();
		system.processLinks.remove(oldLink.processLink);
		systemNode.linkSearch.remove(oldLink.processLink);
		system.processLinks.add(link.processLink);
		systemNode.linkSearch.put(link.processLink);
		link.link();
		systemNode.editor.setDirty(true);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = sourceNode.parent();
		ProductSystem system = systemNode.getProductSystem();
		link.unlink();
		system.processLinks.remove(link.processLink);
		systemNode.linkSearch.remove(link.processLink);
		system.processLinks.add(oldLink.processLink);
		systemNode.linkSearch.put(oldLink.processLink);
		oldLink.link();
		sourceNode.parent().editor.setDirty(true);
	}

}
