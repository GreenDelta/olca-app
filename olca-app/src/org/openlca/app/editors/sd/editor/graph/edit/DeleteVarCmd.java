package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdVarNode;

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
		return graph != null	&& node != null;
	}

	@Override
	public void execute() {
		graph.remove(node);
	}

	@Override
	public void undo() {
		graph.add(node);
	}
}
