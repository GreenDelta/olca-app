package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdVarLink;
import org.openlca.app.editors.sd.editor.graph.model.SdVarNode;

import java.util.ArrayList;

public class DeleteVarCmd extends Command {

	private final SdGraph graph;
	private final SdVarNode node;

	public DeleteVarCmd(SdGraph graph, SdVarNode node) {
		this.graph = graph;
		this.node = node;
		setLabel("Delete Variable");
	}

	@Override
	public boolean canExecute() {
		return graph != null
			&& node != null
			&& node.variable() != null
			&& graph.model() != null;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {

		for (SdVarLink link : new ArrayList<>(node.sourceLinks())) {
			var target = link.target();
			if (target != null) {
				target.targetLinks().remove(link);
			}
			node.sourceLinks().remove(link);
		}

		for (SdVarLink link : new ArrayList<>(node.targetLinks())) {
			var source = link.source();
			if (source != null) {
				source.sourceLinks().remove(link);
			}
			node.targetLinks().remove(link);
		}

		graph.model().vars().remove(node.variable());
		graph.model().positions().remove(node.variable().name());
		graph.remove(node);
	}
}
