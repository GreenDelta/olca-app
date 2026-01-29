package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.*;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.util.Question;

public class RemoveNodeCommand extends Command {

	private final Node node;
	private final Graph graph;
	private final GraphEditor editor;

	public RemoveNodeCommand(Node node, Graph graph) {
		this.node = node;
		this.graph = graph;
		editor = graph.getEditor();
		setLabel(M.Remove);
	}

	@Override
	public boolean canExecute() {
		return graph != null && !graph.isReferenceProcess(node);
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		// TODO translate
		var title = "Remove selected process?";
		var message = "Do you want to remove the selected process"
				+ " from the product system?";
		if (Question.ask(title, message)) {
			redo();
		}
	}

	@Override
	public void redo() {
		// expand both sides to reveal any hidden connected nodes
		if (!node.isExpanded(Side.INPUT)) {
			new ExpandCommand(node, Side.INPUT, true).execute();
		}
		if (!node.isExpanded(Side.OUTPUT)) {
			new ExpandCommand(node, Side.OUTPUT, true).execute();
		}

		// disconnect and remove it
		var providerLinks = graph.linkSearch.getAllLinks(node.descriptor.id);
		for (var link : providerLinks) {
			graph.removeLink(link);
		}
		graph.getProductSystem().processes.remove(node.descriptor.id);
		editor.removeDirty(node.getEntity());
		graph.removeChildQuietly(node);
		graph.notifyChange(CHILDREN_PROP, node, null);
		editor.setDirty();
	}

}
