package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.CHILDREN_PROP;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.util.Question;

/// Removes a selected link, and only that link, from the product system.
public class RemoveLinkCommand extends Command {

	private final GraphLink link;
	private final Graph graph;

	public RemoveLinkCommand(GraphLink link, Graph graph) {
		this.link = link;
		this.graph = graph;
	}

	@Override
	public boolean canExecute() {
		return link != null && graph != null;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		// TODO translate
		var title = "Remove selected link?";
		var message = "Do you want to remove the selected" +
			" link from the product system?";
		if (Question.ask(title, message)) {
			redo();
		}
	}

	@Override
	public String getLabel() {
		return M.DeleteProcessLink;
	}

	@Override
	public void redo() {
		graph.removeGraphLink(link.processLink);
		graph.removeProcessLink(link.processLink);
		graph.firePropertyChange(CHILDREN_PROP, link, null);
		graph.getEditor().setDirty();
	}
}
