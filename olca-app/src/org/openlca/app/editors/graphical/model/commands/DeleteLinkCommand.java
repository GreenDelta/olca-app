package org.openlca.app.editors.graphical.model.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.util.Question;

public class DeleteLinkCommand extends Command {

	private final List<GraphLink> graphLinks;
	private final Graph graph;
	private int answer;

	public DeleteLinkCommand(GraphLink link, Graph graph) {
		this(Collections.singletonList(link), graph);
	}

	public DeleteLinkCommand(List<GraphLink> links, Graph graph) {
		this.graph = graph;
		this.graphLinks = links;
	}

	@Override
	public boolean canExecute() {
		return graphLinks != null && !graphLinks.isEmpty() && graph != null;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		if (graphLinks.isEmpty())
			return;

		answer = Question.ask("Deleting the link...",
				DeleteManager.QUESTION,
				Arrays.stream(DeleteManager.Answer.values()).map(Enum::name).toArray(String[]::new));

		if (answer != DeleteManager.Answer.Cancel.ordinal()) {
			redo();
		}
	}

	@Override
	public String getLabel() {
		return M.DeleteProcesslink;
	}

	@Override
	public void redo() {
		DeleteManager.on(graph).graphLinks(graphLinks, answer);
	}

}
