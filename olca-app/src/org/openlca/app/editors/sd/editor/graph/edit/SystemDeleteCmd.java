package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;

public class SystemDeleteCmd extends Command {

	private final SdGraph graph;
	private final SystemNode node;

	public SystemDeleteCmd(SdGraph graph, SystemNode node) {
		this.graph = graph;
		this.node = node;
		setLabel("Delete product link");
	}

	@Override
	public boolean canExecute() {
		return graph != null && node != null;
	}

	@Override
	public void execute() {
		graph.removeSystem(node);
	}

	@Override
	public void undo() {
		graph.addSystem(node);
	}
}
