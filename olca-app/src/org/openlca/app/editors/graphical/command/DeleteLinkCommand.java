package org.openlca.app.editors.graphical.command;

import java.util.Collections;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

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
		ProductSystemNode systemNode = links.get(0).outputNode.parent();
		for (Link link : links) {
			systemNode.getProductSystem().processLinks.remove(link.processLink);
			systemNode.linkSearch.remove(link.processLink);
			link.unlink();
		}
		systemNode.editor.setDirty(true);
	}

	@Override
	public String getLabel() {
		return M.DeleteProcesslink;
	}

	@Override
	public void redo() {
		ProductSystemNode systemNode = links.get(0).outputNode.parent();
		for (Link link : links) {
			link.unlink();
			systemNode.getProductSystem().processLinks.remove(link.processLink);
			systemNode.linkSearch.remove(link.processLink);
		}
		systemNode.editor.setDirty(true);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = links.get(0).outputNode.parent();
		for (Link link : links) {
			systemNode.getProductSystem().processLinks.add(link.processLink);
			systemNode.linkSearch.put(link.processLink);
			link.link();
			link.updateVisibilty();
		}
		systemNode.editor.setDirty(true);
	}

}
