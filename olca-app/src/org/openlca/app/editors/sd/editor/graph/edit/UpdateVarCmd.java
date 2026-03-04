package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdVarNode;
import org.openlca.sd.model.Var;

public class UpdateVarCmd extends Command {

	private final SdGraph graph;
	private final SdVarNode node;

	public UpdateVarCmd(SdGraph graph, SdVarNode node) {
		this.graph = graph;
		this.node = node;
		setLabel("Update variable");
	}

	@Override
	public boolean canExecute() {
		return graph != null && node != null;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		Var variable = node.variable();

		node.notifier().fire();
		graph.notifier().fire();
	}
}
