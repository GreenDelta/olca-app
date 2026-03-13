package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;
import org.openlca.sd.model.SystemBinding;

public class SystemUpdateCmd extends Command {

	private final SdGraph graph;
	private final SystemNode node;
	private final SystemBinding data;

	public SystemUpdateCmd(
		SdGraph graph, SystemNode node, SystemBinding data
	) {
		this.graph = graph;
		this.node = node;
		this.data = data;
		setLabel("Update product system bindings");
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
		var binding = node.binding();
		binding.setAmount(data.amount());
		binding.setAmountVar(data.amountVar());
		binding.varBindings().clear();
		binding.varBindings().addAll(data.varBindings());
		graph.update(node);
	}
}
