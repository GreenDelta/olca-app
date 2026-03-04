package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdVarNode;
import org.openlca.sd.model.SdModel;

public class AddVarCmd extends Command {

	private final SdModel sdModel;
	private final SdGraph graph;
	private final SdVarNode node;

	public AddVarCmd(SdGraph graph, SdVarNode node) {
		this.graph = graph;
		this.sdModel = graph.model();
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
