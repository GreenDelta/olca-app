package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;

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
		linkWasVisible = link.isVisible();
		this.link.unlink();
		link.getSourceNode().getParent().getProductSystem().getProcessLinks()
				.remove(link.getProcessLink());
		link.getSourceNode().getParent().getEditor().setDirty(true);
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessLinkDeleteCommand_Text;
	}

	@Override
	public void redo() {
		link.getSourceNode().getParent().getProductSystem().getProcessLinks()
				.remove(link.getProcessLink());
		this.link.unlink();
		link.getSourceNode().getParent().getEditor().setDirty(true);
	}

	@Override
	public void undo() {
		link.getSourceNode().getParent().getProductSystem().getProcessLinks()
				.add(link.getProcessLink());
		link.link();
		link.setVisible(linkWasVisible);
		link.getSourceNode().getParent().getEditor().setDirty(true);
	}

	void setLink(ConnectionLink link) {
		this.link = link;
	}

}
