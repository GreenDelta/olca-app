package org.openlca.app.editors.graphical.model.commands;

import java.util.Collections;
import java.util.List;

import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.util.Question;

import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;


public class DeleteLinkCommand extends AbstractRemoveCommand {

	private final List<GraphLink> links;
	private Graph graph;

	public DeleteLinkCommand(GraphLink link, Graph graph) {
		this(Collections.singletonList(link), graph);
	}

	public DeleteLinkCommand(List<GraphLink> links, Graph graph) {
		super(graph);
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
		return false;
	}

	@Override
	public void execute() {
		if (links.isEmpty())
			return;

		for (GraphLink link : links) {
			graph.removeLink(link.processLink);

			var provider = graph.getNode(link.processLink.providerId);
			if (provider.isChainingReferenceNode()
					|| graph.isReferenceProcess(provider))
				continue;

			var b = Question.ask(M.DeleteProcesslink,
					"Do you also want to delete or hide the supply chain?",
					List.of("No", "Delete", "Hide").toArray(new String[0]));

			if (b == 0)
				continue;

			if (b == 1)
				removeEntities(link.processLink, true);
			if (b == 2)
				nodes.add(provider);

			// Remove the supply chain of the nodes that are not graphically
			// connected to the reference node.
			removeNodeChains();
		}

		graph.firePropertyChange(CHILDREN_PROP, null, null);
		editor.setDirty();
	}

	@Override
	public String getLabel() {
		return M.DeleteProcesslink;
	}

	@Override
	public void redo() {
		execute();
	}

}
