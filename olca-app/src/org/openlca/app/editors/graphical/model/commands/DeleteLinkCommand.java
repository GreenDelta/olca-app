package org.openlca.app.editors.graphical.model.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;

import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public class DeleteLinkCommand extends Command {

	private final List<GraphLink> links;
	private Graph graph;

	public DeleteLinkCommand(GraphLink link) {
		this(Collections.singletonList(link));
	}

	public DeleteLinkCommand(List<GraphLink> links) {
		this.links = links;
	}

	@Override
	public boolean canExecute() {
		if (links == null|| links.isEmpty())
			return false;
		graph = links.get(0).getSourceNode().getGraph();
		return graph != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		if (links.isEmpty())
			return;
		for (GraphLink link : links) {
			graph.removeLink(link.processLink);
			removeUnconnectedNodes(link);
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
		for (GraphLink link : links) {
			graph.getProductSystem().processLinks.add(link.processLink);
			graph.linkSearch.put(link.processLink);
			graph.mapProcessLinkToGraphLink.put(link.processLink, link);
			link.reconnect();
		}
		graph.editor.setDirty();
	}

	/**
	 * This method only operates on graphical objects.
	 */
	private void removeUnconnectedNodes(GraphLink link) {
		for (var node : Arrays.asList(link.getSourceNode(), link.getTargetNode()))
			if (!node.isChainingReferenceNode())
				graph.removeChild(node);
	}

}
