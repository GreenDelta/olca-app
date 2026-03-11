package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;

public class AddSystemCmd extends Command {

	private final SdGraph graph;
	private final SystemNode node;

	public AddSystemCmd(SdGraph graph, SystemNode node) {
		this.graph = graph;
		this.node = node;
		setLabel("Add Product System");
	}

	@Override
	public boolean canExecute() {
		return graph != null && node != null;
	}

	@Override
	public void execute() {
		graph.addSystem(node);
	}

	@Override
	public void undo() {
		graph.removeSystem(node);
	}
}
