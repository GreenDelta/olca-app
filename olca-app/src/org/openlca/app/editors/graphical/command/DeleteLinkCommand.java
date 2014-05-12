package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class DeleteLinkCommand extends Command {

	private ConnectionLink link;
	private boolean linkWasVisible = false;

	DeleteLinkCommand() {
	}

	@Override
	public boolean canExecute() {
		return link != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		ProductSystemNode systemNode = link.getSourceNode().getParent();
		linkWasVisible = link.isVisible();
		systemNode.getProductSystem().getProcessLinks()
				.remove(link.getProcessLink());
		systemNode.reindexLinks();
		this.link.unlink();
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessLinkDeleteCommand_Text;
	}

	@Override
	public void redo() {
		ProductSystemNode systemNode = link.getSourceNode().getParent();
		systemNode.getProductSystem().getProcessLinks()
				.remove(link.getProcessLink());
		this.link.unlink();
		systemNode.getEditor().setDirty(true);
		systemNode.reindexLinks();
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = link.getSourceNode().getParent();
		systemNode.getProductSystem().getProcessLinks()
				.add(link.getProcessLink());
		link.link();
		link.setVisible(linkWasVisible);
		systemNode.getEditor().setDirty(true);
		systemNode.reindexLinks();
	}

	void setLink(ConnectionLink link) {
		this.link = link;
	}

}
