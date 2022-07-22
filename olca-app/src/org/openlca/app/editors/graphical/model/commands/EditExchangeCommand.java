package org.openlca.app.editors.graphical.model.commands;

import java.util.Objects;

import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;

import static org.openlca.app.editors.graphical.model.GraphFactory.updateExchangeItem;

public class EditExchangeCommand extends Command {

	private final ExchangeItem oldExchangeItem;
	private ExchangeItem newExchangeItem;
	private final IDatabase db = Database.get();
	private final Process process;
	private Exchange exchange;
	private final Node node;

	public EditExchangeCommand(ExchangeItem exchangeItem) {
		this.oldExchangeItem = exchangeItem;
		this.node = exchangeItem.getNode();
		process = db.get(Process.class, node.descriptor.id);
		exchange = process.exchanges.stream()
			.filter(e -> Objects.equals(e, oldExchangeItem.exchange))
			.findFirst()
			.orElse(null);
	}

	@Override
	public boolean canExecute() {
		var node = oldExchangeItem.getNode();
		return oldExchangeItem.exchange != null
			&& oldExchangeItem.exchange.flow != null
			&& process != null
			&& node != null
			&& node.descriptor != null
			&& node.isEditable();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		if (process == null || exchange == null)
			return;

		if (!EditExchangeDialog.open(exchange))
			return;
		newExchangeItem = new ExchangeItem(node.getGraph().editor, exchange);
		redo();
	}

	@Override
	public void redo() {
		exchange = newExchangeItem.exchange;
		db.update(process);
		var descriptor = Descriptor.of(process);
		updateExchangeItem(node, descriptor, oldExchangeItem, newExchangeItem);

		node.editor.setDirty();
	}

	@Override
	public void undo() {
		exchange = oldExchangeItem.exchange;
		db.update(process);
		var descriptor = Descriptor.of(process);
		updateExchangeItem(node, descriptor, newExchangeItem, oldExchangeItem);
	}

}
