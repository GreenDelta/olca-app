package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.matrix.ProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class HideShowCommand extends Command {

	static final int SHOW = 1;
	static final int HIDE = 2;

	private ProcessDescriptor process;
	private ProductSystemNode model;
	private int type;

	HideShowCommand(int type) {
		this.type = type;
	}

	@Override
	public void execute() {
		ProcessNode node = model.getProcessNode(process.getId());
		if (type == SHOW && node == null) {
			node = new ProcessNode(process);
			model.add(node);
			createNecessaryLinks(node);
		}
		if (type == HIDE)
			for (ConnectionLink link : node.getLinks())
				link.setVisible(false);
		node.setVisible(type == SHOW);
		if (type == SHOW)
			showLinks(node);
		node.layout();
	}

	@Override
	public boolean canExecute() {
		if (process == null)
			return false;
		if (model == null)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public String getLabel() {
		if (type == SHOW)
			return Messages.Systems_HideShowCommand_ShowText;
		else if (type == SHOW)
			return Messages.Systems_HideShowCommand_HideText;
		return null;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		switchType();
		execute();
		switchType();
	}

	private void createNecessaryLinks(ProcessNode node) {
		ProcessLinkSearchMap linkSearch = node.getParent().getLinkSearch();
		for (ProcessLink link : linkSearch.getLinks(process.getId())) {
			long processId = link.getRecipientId() == process.getId() ? link
					.getProviderId() : link.getRecipientId();
			ProcessNode newNode = model.getProcessNode(processId);
			if (newNode != null) {
				ProcessNode sourceNode = link.getRecipientId() == process
						.getId() ? newNode : node;
				ProcessNode targetNode = link.getRecipientId() == process
						.getId() ? node : newNode;
				ConnectionLink connectionLink = new ConnectionLink();
				connectionLink.setSourceNode(sourceNode);
				connectionLink.setTargetNode(targetNode);
				connectionLink.setProcessLink(link);
				connectionLink.link();
			}
		}
	}

	private void showLinks(ProcessNode node) {
		for (ConnectionLink link : node.getLinks()) {
			ProcessNode otherNode = null;
			boolean isSource = false;
			if (link.getSourceNode().equals(node)) {
				otherNode = link.getTargetNode();
				isSource = true;
			} else if (link.getTargetNode().equals(node))
				otherNode = link.getSourceNode();

			if (otherNode.isVisible())
				if (isSource && otherNode.isExpandedLeft())
					link.setVisible(true);
				else if (!isSource && otherNode.isExpandedRight())
					link.setVisible(true);
		}
	}

	private void switchType() {
		if (type == SHOW)
			type = HIDE;
		else if (type == HIDE)
			type = SHOW;
	}

	void setProcess(ProcessDescriptor process) {
		this.process = process;
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

}
