package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.VarNode;

public class VarAddCmd extends Command {

	private final SdGraph graph;
	private final VarNode node;

	public VarAddCmd(SdGraph graph, VarNode node) {
		this.graph = graph;
		this.node = node;
		setLabel("Add Variable");
	}

	@Override
	public boolean canExecute() {
		return graph != null && node != null;
	}

	@Override
	public void execute() {
		graph.add(node);
	}

	@Override
	public void undo() {
		graph.remove(node);
	}
}
