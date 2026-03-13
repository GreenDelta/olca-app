package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.VarNode;

public class VarDeleteCmd extends Command {

	private final SdGraph graph;
	private final VarNode node;

	public VarDeleteCmd(SdGraph graph, VarNode node) {
		this.graph = graph;
		this.node = node;
		setLabel("Delete variable");
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
