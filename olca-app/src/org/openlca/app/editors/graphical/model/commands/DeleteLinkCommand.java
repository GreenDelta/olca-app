package org.openlca.app.editors.graphical.model.commands;

import java.util.Collections;
import java.util.List;

import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.util.Question;
import org.openlca.util.ProviderChainRemoval;

import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;


public class DeleteLinkCommand extends AbstractRemoveCommand {

	private final List<GraphLink> links;
	private final Graph graph;

	public DeleteLinkCommand(GraphLink link, Graph graph) {
		this(Collections.singletonList(link), graph);
	}

	public DeleteLinkCommand(List<GraphLink> links, Graph graph) {
		super(graph);
		this.graph = graph;
		this.links = links;
	}

	@Override
	public boolean canExecute() {
		if (links == null || links.isEmpty())
			return false;
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
			var provider = graph.getNode(link.processLink.providerId);
			graph.removeGraphLink(link.processLink);

			if (provider.isChainingReferenceNode()
					|| graph.isReferenceProcess(provider)) {
				graph.removeProcessLink(link.processLink);
				continue;
			}

			var answer = Question.ask("Deleting the process link...",
					"Do you want to Delete the supply chain (if the " +
							"supply chain is connected to the reference process, it will be" +
							"hidden) or Hide it?",
					List.of("No", "Delete", "Hide").toArray(new String[0]));

			if (answer == 0) {
				graph.removeProcessLink(link.processLink);
				continue;
			}

			if (answer == 1) {
				var r = ProviderChainRemoval.on(graph.getProductSystem());
				r.remove(link.processLink);
			}

			if (answer == 2) {
				graph.removeProcessLink(link.processLink);
			}

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
