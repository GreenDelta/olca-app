package org.openlca.app.editors.graphical.model.commands;

import java.util.Collections;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Link;

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
		Graph graph = links.get(0).getSourceNode().getGraph();
		for (Link link : links) {
			graph.getProductSystem().processLinks.remove(link.processLink);
			graph.linkSearch.remove(link.processLink);
			link.disconnect();
		}
		graph.editor.setDirty();
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
		Graph graph = links.get(0).getSourceNode().getGraph();
		for (Link link : links) {
			graph.getProductSystem().processLinks.add(link.processLink);
			graph.linkSearch.put(link.processLink);
			link.reconnect();
		}
		graph.editor.setDirty();
	}

}
