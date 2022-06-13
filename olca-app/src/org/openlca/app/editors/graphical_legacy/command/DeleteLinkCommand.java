package org.openlca.app.editors.graphical_legacy.command;

import java.util.Collections;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.model.Link;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;

public class DeleteLinkCommand extends Command {

	private final List<Link> links;

	public DeleteLinkCommand(Link link) {
		this(Collections.singletonList(link));
	}

	public DeleteLinkCommand(List<Link> links) {
		this.links = links;
	}

	@Override
	public boolean canExecute() {
		return links != null && !links.isEmpty();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		if (links.isEmpty())
			return;
		ProductSystemNode sysNode = links.get(0).outputNode.parent();
		for (Link link : links) {
			sysNode.getProductSystem().processLinks.remove(link.processLink);
			sysNode.linkSearch.remove(link.processLink);
			link.unlink();
		}
		sysNode.editor.setDirty();
	}

	@Override
	public String getLabel() {
		return M.DeleteProcesslink;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		if (links.isEmpty())
			return;
		ProductSystemNode sysNode = links.get(0).outputNode.parent();
		for (Link link : links) {
			sysNode.getProductSystem().processLinks.add(link.processLink);
			sysNode.linkSearch.put(link.processLink);
			link.link();
			link.updateVisibility();
		}
		sysNode.editor.setDirty();
	}

}
