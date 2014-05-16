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
		link.unlink();
		systemNode.getProductSystem().getProcessLinks()
				.remove(link.getProcessLink());
		systemNode.getLinkSearch().remove(link.getProcessLink());
		systemNode.refresh();
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessLinkDeleteCommand_Text;
	}

	@Override
	public void redo() {
		link.unlink();
		ProductSystemNode systemNode = link.getSourceNode().getParent();
		systemNode.getProductSystem().getProcessLinks()
				.remove(link.getProcessLink());
		systemNode.getLinkSearch().remove(link.getProcessLink());
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = link.getSourceNode().getParent();
		systemNode.getProductSystem().getProcessLinks()
				.add(link.getProcessLink());
		systemNode.getLinkSearch().put(link.getProcessLink());
		link.link();
		link.setVisible(linkWasVisible);
		systemNode.getEditor().setDirty(true);
	}

	void setLink(ConnectionLink link) {
		this.link = link;
	}

}
