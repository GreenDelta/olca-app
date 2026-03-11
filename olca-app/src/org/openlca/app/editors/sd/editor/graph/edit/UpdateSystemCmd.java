package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.VarBinding;

import java.util.ArrayList;
import java.util.List;

public class UpdateSystemCmd extends Command {

	private final SystemNode node;
	private final double amount;
	private final Id amountVar;
	private final List<VarBinding> varBindings;

	public UpdateSystemCmd(
		SystemNode node,
		double amount,
		Id amountVar,
		List<VarBinding> varBindings
	) {
		this.node = node;
		this.amount = amount;
		this.amountVar = amountVar;
		this.varBindings = new ArrayList<>(varBindings);
		setLabel("Update Product System");
	}

	@Override
	public boolean canExecute() {
		return node != null;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		var binding = node.binding();
		binding.setAmount(amount);
		binding.setAmountVar(amountVar);
		binding.varBindings().clear();
		binding.varBindings().addAll(varBindings);
		node.notifier().fire();
	}
}
