package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.CHILDREN_PROP;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.util.Question;
import org.openlca.core.model.ProcessLink;

public class RemoveNodeCommand extends Command {

	private final Node node;
	private final Graph graph;
	private final GraphEditor editor;
	private int answer;

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
		answer = Question.ask(M.DeletingDots,
				DeleteManager.QUESTION,
				Arrays.stream(DeleteManager.Answer.values())
						.map(Enum::name)
						.toArray(String[]::new));

		if (answer != DeleteManager.Answer.Cancel.ordinal()) {
			redo();
		}
	}

	@Override
	public void redo() {
		var deleteManager = DeleteManager.on(graph);
		var connectedNodes = new ArrayList<Node>();

		// Remove the links to the suppliers
		var providerLinks = graph.linkSearch.getProviderLinks(node.descriptor.id);
		for (ProcessLink link : providerLinks) {
			var recipient = graph.getNode(link.processId);
			if (recipient != null) {
				connectedNodes.add(recipient);
			}
			graph.removeLink(link);
		}

		// Remove the links to the providers
		var connectionLinks = graph.linkSearch.getConnectionLinks(node.descriptor.id);
		for (ProcessLink link : connectionLinks) {
			var provider = deleteManager.link(link, answer);
			if (provider != null) {
				connectedNodes.add(provider);
			}
		}

		deleteManager.process(node.descriptor.id);
		for (Node node : connectedNodes) {
			deleteManager.removeNodeChains(node);
		}

		graph.firePropertyChange(CHILDREN_PROP, null, null);
		editor.setDirty();
	}

}
