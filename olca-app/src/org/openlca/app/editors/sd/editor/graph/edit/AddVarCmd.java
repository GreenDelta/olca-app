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
	public void execute() {
		if (sdModel != null) {
			sdModel.vars().add(node.variable());
		}
		if (graph != null) {
			graph.add(node);
		}
	}

	@Override
	public void undo() {
		if (graph != null) {
			graph.remove(node);
		}
		if (sdModel != null) {
			sdModel.vars().remove(node.variable());
		}
	}

}
